package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.SysModule;
import com.rightmanage.entity.SysRole;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.entity.SysTenant;
import com.rightmanage.entity.AssetType;
import com.rightmanage.entity.flow.*;
import com.rightmanage.mapper.flow.*;
import com.rightmanage.mapper.BankOrgMapper;
import com.rightmanage.mapper.SysUserRoleMapper;
import com.rightmanage.mapper.AssetTypeMapper;
import com.rightmanage.service.flow.FlowInstanceService;
import com.rightmanage.service.flow.FlowDefinitionService;
import com.rightmanage.service.flow.FlowCommonService;
import com.rightmanage.service.flow.FlowOperationLogService;
import com.rightmanage.service.SysUserService;
import com.rightmanage.service.SysRoleService;
import com.rightmanage.service.SysTenantService;
import com.rightmanage.service.SysModuleService;
import com.rightmanage.enums.*;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance> implements FlowInstanceService {

    private static final Logger log = LoggerFactory.getLogger(FlowInstanceServiceImpl.class);

    private final ThreadLocal<Map<String, DynamicHandlerDTO>> dynamicHandlerThreadLocal = new ThreadLocal<>();
    // 多租户审批节点：发起时选择的租户编码列表
    private final ThreadLocal<List<String>> nodeTenantsThreadLocal = new ThreadLocal<>();
    // 机构相关审批：发起时选择的机构ID
    private final ThreadLocal<String> nodeSourceOrgIdThreadLocal = new ThreadLocal<>();

    private static final Map<String, TaskCallbackContext> callbackTokenMap = new ConcurrentHashMap<>();

    @Data
    public static class TaskCallbackContext {
        private String taskId;
        private String instanceId;
        private String moduleCode;
        private String nodeKey;
        private Long operatorId;
        private String operatorName;
        private Date callbackTime;
    }

    @Data
    public static class FlowLine {
        private String fromNode;
        private String toNode;
        private String fromSide;
        private String toSide;
    }

    @Data
    public static class FlowJsonData {
        private List<Map<String, Object>> nodes;
        private List<FlowLine> lines;
    }

    private Map<String, String> buildFlowJsonNodeAliasMap(FlowJsonData flowJsonData) {
        Map<String, String> aliasMap = new HashMap<>();
        if (flowJsonData == null || flowJsonData.getNodes() == null) {
            return aliasMap;
        }
        for (Map<String, Object> node : flowJsonData.getNodes()) {
            if (node == null) {
                continue;
            }
            Object idObj = node.get("id");
            Object nodeKeyObj = node.get("nodeKey");
            String id = idObj == null ? null : String.valueOf(idObj).trim();
            String nodeKey = nodeKeyObj == null ? null : String.valueOf(nodeKeyObj).trim();
            if (StringUtils.hasText(id) && StringUtils.hasText(nodeKey)) {
                aliasMap.put(id, nodeKey);
            }
        }
        return aliasMap;
    }

    private void mergeFlowJsonAliasesIntoNodeIdMap(Map<String, FlowNodeConfig> nodeKeyMap,
            Map<String, FlowNodeConfig> nodeIdMap, Map<String, String> flowJsonNodeAliasMap) {
        if (flowJsonNodeAliasMap == null || flowJsonNodeAliasMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : flowJsonNodeAliasMap.entrySet()) {
            FlowNodeConfig cfg = nodeKeyMap.get(entry.getValue());
            if (cfg != null) {
                nodeIdMap.put(entry.getKey(), cfg);
            }
        }
    }

    private TaskCallbackContext getCallbackContext(String callbackToken) {
        TaskCallbackContext ctx = callbackTokenMap.get(callbackToken);
        if (ctx != null) {
            return ctx;
        }

        FlowTask task = flowTaskMapper.selectOne(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getCallbackToken, callbackToken)
                        .eq(FlowTask::getDeleted, 0)
        );
        if (task == null) {
            return null;
        }

        ctx = new TaskCallbackContext();
        ctx.setTaskId(task.getTaskId());
        ctx.setInstanceId(task.getInstanceId());
        ctx.setNodeKey(task.getNodeKey());
        ctx.setCallbackTime(task.getExecuteTime());

        FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
        if (instance != null) {
            ctx.setModuleCode(instance.getModuleCode());
        }

        if (task.getHandlerId() != null) {
            SysUser handler = sysUserService.getById(task.getHandlerId());
            ctx.setOperatorId(task.getHandlerId());
            ctx.setOperatorName(handler != null ? handler.getUsername() : null);
        }

        callbackTokenMap.put(callbackToken, ctx);
        return ctx;
    }

    @Autowired
    private FlowInstanceMapper flowInstanceMapper;
    @Autowired
    private FlowDefinitionMapper flowDefinitionMapper;
    @Autowired
    private FlowDefinitionService flowDefinitionService;
    @Autowired
    private FlowNodeConfigMapper flowNodeConfigMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowOperationLogService logService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysRoleService sysRoleService;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysTenantService sysTenantService;
    @Autowired
    private SysModuleService sysModuleService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private BankOrgMapper bankOrgMapper;
    @Autowired
    private AssetTypeMapper assetTypeMapper;
    @Autowired
    private FlowTemplateParamMapper flowTemplateParamMapper;
    @Autowired
    private FlowInstanceParamMapper flowInstanceParamMapper;
    @Autowired
    @Lazy
    private FlowCommonService flowCommonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 判断是否是多租户模块
     */
    private boolean isMultiTenantModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isEmpty()) {
            return false;
        }
        return sysModuleService.isMultiTenant(moduleCode);
    }

    private AssetType getAssetTypeByCode(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return null;
        }
        return assetTypeMapper.selectOne(
                new LambdaQueryWrapper<AssetType>().eq(AssetType::getTypeCode, typeCode).last("LIMIT 1"));
    }

    @Override
    public List<FlowInstance> list() {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowInstance>()
                .orderByDesc(FlowInstance::getCreateTime));
    }

    @Override
    public String startFlow(FlowStartDTO dto, Long userId) {
        if (dto == null) {
            throw new RuntimeException("请求体不能为空");
        }
        System.out.println("startFlow called - flowId: " + dto.getFlowCode() + ", userId: " + userId);
        try {
            return startFlowInternal(dto, userId);
        } catch (Exception e) {
            System.out.println("startFlow exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            dynamicHandlerThreadLocal.remove();
            nodeTenantsThreadLocal.remove();
            nodeSourceOrgIdThreadLocal.remove();
        }
    }

    private String startFlowInternal(FlowStartDTO dto, Long userId) {
        FlowDefinition flow = flowDefinitionMapper.selectById(dto.getFlowCode());
        if (flow == null || flow.getStatus() != 1) {
            throw new RuntimeException("流程不存在或未启用");
        }
        if (!StringUtils.hasText(dto.getModuleCode())) {
            throw new RuntimeException("发起流程时必须选择模块");
        }
        if (!StringUtils.hasText(dto.getAssetTypeId())) {
            throw new RuntimeException("发起流程时必须选择资产类型");
        }

        // 从 nodeConfigs 构建运行时配置 map（nodeKey -> DTO），同时汇总 nodeTenants
        Map<String, DynamicHandlerDTO> dynamicHandlerMap = new HashMap<>();
        List<String> resolvedNodeTenants = new ArrayList<>();

        if (dto.getNodeConfigs() != null && !dto.getNodeConfigs().isEmpty()) {
            for (FlowNodeConfigDTO nc : dto.getNodeConfigs()) {
                System.out.println("NodeConfig - nodeKey: " + nc.getNodeKey()
                        + ", handlerType: " + nc.getHandlerType()
                        + ", handlerId: " + nc.getHandlerId()
                        + ", handlerName: " + nc.getHandlerName()
                        + ", tenantCode: " + nc.getTenantCode());

                DynamicHandlerDTO dto2 = new DynamicHandlerDTO();
                dto2.setNodeKey(nc.getNodeKey());
                dto2.setHandlerId(nc.getHandlerId());
                dto2.setHandlerName(nc.getHandlerName());
                dto2.setTenantCode(nc.getTenantCode());
                dto2.setSourceOrgId(nc.getSourceOrgId());
                dto2.setModuleCode(nc.getModuleCode());
                dynamicHandlerMap.put(nc.getNodeKey(), dto2);

                // 汇总非空的 tenantCode（保留到列表，用于 ThreadLocal）
                if (nc.getTenantCode() != null && !resolvedNodeTenants.contains(nc.getTenantCode())) {
                    resolvedNodeTenants.add(nc.getTenantCode());
                }
            }
        }
        dynamicHandlerThreadLocal.set(dynamicHandlerMap);

        // 收集 nodeConfigs 中的 sourceOrgId（取第一个非空值，用于机构相关审批）
        String globalSourceOrgId = null;
        if (dto.getNodeConfigs() != null) {
            for (FlowNodeConfigDTO nc : dto.getNodeConfigs()) {
                if (nc.getSourceOrgId() != null) {
                    globalSourceOrgId = nc.getSourceOrgId();
                    break;
                }
            }
        }
        nodeSourceOrgIdThreadLocal.set(globalSourceOrgId);

        // 解析多租户审批节点的租户列表（优先用 nodeConfigs 汇总结果，降级用顶层字段）
        List<String> nodeTenants = !resolvedNodeTenants.isEmpty() ? resolvedNodeTenants
                : (dto.getNodeTenants() != null ? dto.getNodeTenants() : new ArrayList<>());
        if (!nodeTenants.isEmpty()) {
            System.out.println("===== [多租户审批] 允许的租户 =====");
            System.out.println("  租户Codes: " + nodeTenants);
            System.out.println("========================================");
        }
        nodeTenantsThreadLocal.set(nodeTenants);

        SysUser applicant = sysUserService.getById(userId);
        if (applicant == null) {
            throw new RuntimeException("发起人不存在，userId=" + userId);
        }

        boolean needTenant = flowDefinitionService.checkFlowNeedTenant(dto.getFlowCode());
        if (needTenant && (nodeTenants == null || nodeTenants.isEmpty())) {
            throw new RuntimeException("该流程包含多租户审批节点，必须选择至少一个租户");
        }

        if (flow.getNeedAttachment() != null && flow.getNeedAttachment() == 1) {
            if (dto.getAttachmentUrl() == null || dto.getAttachmentUrl().isEmpty()) {
                throw new RuntimeException("该流程需要上传凭证，请上传凭证文件（支持Excel、PDF、WPS格式）");
            }
        }

        FlowInstance instance = new FlowInstance();
        instance.setInstanceId(UUID.randomUUID().toString().replace("-", ""));
        instance.setFlowCode(dto.getFlowCode());
        instance.setInstanceName(dto.getInstanceName());
        instance.setApplicantId(userId);
        instance.setModuleCode(dto.getModuleCode());
        instance.setAssetTypeId(dto.getAssetTypeId());
        instance.setTenantCode(dto.getTenantCode());
        instance.setCurrentNodeName("开始节点");
        instance.setStatus(FlowInstanceStatus.RUNNING.getCode());
        instance.setAttachmentUrl(dto.getAttachmentUrl());
        instance.setAttachmentName(dto.getAttachmentName());

        if ("REPORT_RELEASE".equals(dto.getFlowCode())) {
            String randomUrl = "https://gray.example.com/release/" + UUID.randomUUID().toString().substring(0, 8);
            instance.setExtraInfo("请访问灰度发布链接：" + randomUrl);
        }

        if (dto.getNodeConfigs() != null && !dto.getNodeConfigs().isEmpty()) {
            try {
                Map<String, Object> extraData = new HashMap<>();
                extraData.put("nodeConfigs", dto.getNodeConfigs());
                if (!nodeTenants.isEmpty()) {
                    extraData.put("nodeTenants", nodeTenants);
                }
                instance.setDynamicHandlers(objectMapper.writeValueAsString(extraData));
            } catch (Exception e) {
                log.error("保存节点配置信息失败", e);
            }
        }

        flowInstanceMapper.insert(instance);

        logService.saveLog(instance.getInstanceId(), userId, applicant.getUsername(),
                FlowOperationType.INIT.getCode(), "用户" + applicant.getUsername() + "发起流程：" + dto.getInstanceName());

        List<FlowNodeConfig> nodeConfigs = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, dto.getFlowCode())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        if (nodeConfigs.isEmpty()) {
            throw new RuntimeException("流程未配置节点，请先设计流程");
        }

        List<FlowLine> flowLines = new ArrayList<>();
        Map<String, String> flowJsonNodeAliasMap = new HashMap<>();
        if (flow.getFlowJson() != null && !flow.getFlowJson().isEmpty()) {
            try {
                FlowJsonData flowJsonData = objectMapper.readValue(flow.getFlowJson(), FlowJsonData.class);
                if (flowJsonData != null && flowJsonData.getLines() != null) {
                    flowLines = flowJsonData.getLines();
                    flowJsonNodeAliasMap = buildFlowJsonNodeAliasMap(flowJsonData);
                    System.out.println("===== [调试] 解析 flowJson 连线信息，共 " + flowLines.size() + " 条连线 =====");
                    for (int i = 0; i < flowLines.size(); i++) {
                        FlowLine fl = flowLines.get(i);
                        System.out.println("  连线" + i + ": fromNode=" + fl.getFromNode() + ", toNode=" + fl.getToNode());
                    }
                }
            } catch (Exception e) {
                log.warn("解析 flowJson 失败，使用 sort 顺序作为兼容模式", e);
            }
        }

        // 构建节点映射：nodeKey -> FlowNodeConfig 和 nodeId -> FlowNodeConfig
        Map<String, FlowNodeConfig> nodeKeyMap = new HashMap<>();
        Map<String, FlowNodeConfig> nodeIdMap = new HashMap<>();
        System.out.println("===== [调试] 构建节点映射 =====");
        for (FlowNodeConfig node : nodeConfigs) {
            nodeKeyMap.put(node.getNodeKey(), node);
            if (StringUtils.hasText(node.getNodeId())) {
                nodeIdMap.put(node.getNodeId(), node);
            }
            System.out.println("  节点: nodeKey=" + node.getNodeKey() + ", nodeId=" + node.getNodeId() + ", type=" + node.getNodeType());
        }
        mergeFlowJsonAliasesIntoNodeIdMap(nodeKeyMap, nodeIdMap, flowJsonNodeAliasMap);
        System.out.println("  nodeIdMap 大小: " + nodeIdMap.size() + ", nodeKeyMap 大小: " + nodeKeyMap.size());

        // 找到开始节点
        FlowNodeConfig startNode = null;
        for (FlowNodeConfig node : nodeConfigs) {
            if (FlowNodeType.START.getCode().equals(node.getNodeType())) {
                startNode = node;
                break;
            }
        }

        if (startNode == null) {
            startNode = nodeConfigs.get(0);
        }

        // 创建开始节点的任务记录
        createAutoTask(instance.getInstanceId(), startNode, "auto", "开始节点自动执行");
        logService.saveLog(instance.getInstanceId(), null, null,
                FlowOperationType.NOTIFY.getCode(), "开始节点自动触发");

        // 【关键修改】查找开始节点的所有后续节点（支持并行分支）
        List<FlowNodeConfig> nextNodes = findNextNodesByLines(startNode, nodeConfigs, flowLines, nodeKeyMap, nodeIdMap);
        
        if (nextNodes.isEmpty() && nodeConfigs.size() > 1) {
            // 兼容模式：使用 sort 顺序的下一个节点
            for (int i = 0; i < nodeConfigs.size(); i++) {
                if (nodeConfigs.get(i).getNodeKey().equals(startNode.getNodeKey())) {
                    if (i + 1 < nodeConfigs.size()) {
                        nextNodes.add(nodeConfigs.get(i + 1));
                    }
                    break;
                }
            }
        }

        if (nextNodes.isEmpty()) {
            throw new RuntimeException("流程仅配置开始节点，无后续节点");
        }

        // 【关键修改】并行触发所有后续节点
        for (FlowNodeConfig nextNode : nextNodes) {
            System.out.println("startFlow - 触发节点: " + nextNode.getNodeKey());
            processNodeAndChildren(instance, nextNode, nodeConfigs, flowLines, nodeKeyMap, nodeIdMap, dynamicHandlerMap, new HashSet<>());
        }

        if ("REPORT_RELEASE".equals(flow.getFlowCode())) {
            fillTextNodeCustomFields(instance.getInstanceId(), nodeConfigs);
        }

        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            saveFlowInstanceParams(instance.getInstanceId(), dto.getFlowCode(), dto.getParams());
        }

        return instance.getInstanceId();
    }

    /**
     * 【核心】根据连线查找当前节点的所有后续节点（支持并行分支）
     */
    private List<FlowNodeConfig> findNextNodesByLines(FlowNodeConfig currentNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {

        List<FlowNodeConfig> nextNodes = new ArrayList<>();

        // 获取当前节点的标识（可能是 nodeKey 或 nodeId）
        String currentKey = currentNode.getNodeKey();
        String currentNodeId = currentNode.getNodeId();

        System.out.println("===== [调试] findNextNodesByLines =====");
        System.out.println("  当前节点: nodeKey=[" + currentKey + "], nodeId=[" + currentNodeId + "], type=[" + currentNode.getNodeType() + "]");
        System.out.println("  连线总数: " + (flowLines == null ? 0 : flowLines.size()));
        System.out.println("  nodeIdMap: " + nodeIdMap.keySet());
        System.out.println("  nodeKeyMap: " + nodeKeyMap.keySet());

        if (flowLines == null || flowLines.isEmpty()) {
            System.out.println("  结果: 连线为空，返回空列表");
            return nextNodes;
        }

        // 查找所有以当前节点为起点的连线
        Set<String> addedNodeKeys = new HashSet<>();
        System.out.println("  开始遍历连线...");
        for (int i = 0; i < flowLines.size(); i++) {
            FlowLine line = flowLines.get(i);
            // 匹配起始节点
            String fromNode = line.getFromNode();
            String toNode = line.getToNode();
            if (!StringUtils.hasText(fromNode) || !StringUtils.hasText(toNode)) {
                continue;
            }

            FlowNodeConfig fromCfg = nodeIdMap.get(fromNode);
            if (fromCfg == null) {
                fromCfg = nodeKeyMap.get(fromNode);
            }
            boolean keyMatches = fromCfg != null && Objects.equals(fromCfg.getNodeKey(), currentKey);
            boolean nodeIdMatches = fromCfg != null && currentNodeId != null && Objects.equals(fromCfg.getNodeId(), currentNodeId);
            boolean matches = keyMatches || nodeIdMatches;

            System.out.println("  连线" + i + ": fromNode=" + fromNode + ", toNode=" + toNode
                    + " | keyMatches=" + keyMatches + "(currentKey=[" + currentKey + "]), nodeIdMatches=" + nodeIdMatches + "(currentNodeId=[" + currentNodeId + "])"
                    + " | 匹配=" + matches);

            if (matches) {
                String targetKey = toNode;
                System.out.println("  -> 匹配成功! targetKey=" + targetKey);
                // 如果 target 是 nodeId，转为 nodeKey
                FlowNodeConfig targetNode = nodeIdMap.get(targetKey);
                System.out.println("  -> nodeIdMap查找: " + (targetNode != null ? "找到 nodeKey=" + targetNode.getNodeKey() : "null"));
                if (targetNode == null) {
                    targetNode = nodeKeyMap.get(targetKey);
                    System.out.println("  -> nodeKeyMap查找: " + (targetNode != null ? "找到 nodeKey=" + targetNode.getNodeKey() : "null"));
                }

                if (targetNode != null) {
                    if (!addedNodeKeys.contains(targetNode.getNodeKey())) {
                        nextNodes.add(targetNode);
                        addedNodeKeys.add(targetNode.getNodeKey());
                        System.out.println("  -> 添加到结果: nodeKey=" + targetNode.getNodeKey() + ", type=" + targetNode.getNodeType());
                    } else {
                        System.out.println("  -> 已存在，跳过: nodeKey=" + targetNode.getNodeKey());
                    }
                } else {
                    System.out.println("  -> 未找到对应节点，忽略!");
                }
            }
        }

        System.out.println("  最终结果: " + nextNodes.size() + " 个后续节点");
        for (FlowNodeConfig n : nextNodes) {
            System.out.println("    - " + n.getNodeKey() + " (" + n.getNodeType() + ")");
        }

        // 兼容旧数据：当 flowJson 连线中的节点ID与 flow_node_config.node_id 不一致时，回退到 sort 顺序
        if (nextNodes.isEmpty() && allNodes != null && allNodes.size() > 1) {
            for (int i = 0; i < allNodes.size(); i++) {
                FlowNodeConfig node = allNodes.get(i);
                boolean isCurrent = Objects.equals(node.getNodeKey(), currentNode.getNodeKey())
                        || (StringUtils.hasText(node.getNodeId()) && Objects.equals(node.getNodeId(), currentNode.getNodeId()));
                if (isCurrent && i + 1 < allNodes.size()) {
                    nextNodes.add(allNodes.get(i + 1));
                    System.out.println("  兼容回退: 使用 sort 顺序后续节点 -> " + allNodes.get(i + 1).getNodeKey());
                    break;
                }
            }
        }
        return nextNodes;
    }

    /**
     * 【核心】处理节点及其后续节点（递归处理并行分支）
     * @param instance 流程实例
     * @param node 当前节点
     * @param allNodes 所有节点
     * @param flowLines 所有连线
     * @param nodeKeyMap nodeKey 到节点的映射
     * @param nodeIdMap nodeId 到节点的映射
     * @param dynamicHandlerMap 动态处理人映射
     * @param processedNodes 已处理的节点（防止循环）
     */
    private void processNodeAndChildren(FlowInstance instance, FlowNodeConfig node,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap,
            Map<String, DynamicHandlerDTO> dynamicHandlerMap, Set<String> processedNodes) {
        
        // 防止循环处理（但这个集合不跨调用持久化，所以还要查库做幂等）
        if (processedNodes.contains(node.getNodeKey())) {
            return;
        }
        processedNodes.add(node.getNodeKey());

        // 【幂等检查】如果该节点的任务已存在（status=1 已通过 或 status=0 待处理），不再重复创建
        // 这防止了多次审批通过导致同一节点被重复创建的问题
        List<FlowTask> existingTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getInstanceId())
                        .eq(FlowTask::getNodeKey, node.getNodeKey())
                        .eq(FlowTask::getDeleted, 0)
        );
        if (!existingTasks.isEmpty()) {
            System.out.println("processNodeAndChildren - 幂等：节点 " + node.getNodeKey() + " 已存在任务，不再重复创建");
            // 如果任务已存在但尚未处理后续节点，仍然递归处理后续节点（但后续节点会有自己的幂等检查）
        }

        String nodeType = node.getNodeType();
        System.out.println("processNodeAndChildren - 处理节点: " + node.getNodeKey() + ", type: " + nodeType);

        // 【修复】currentNodeKey 只记录需要用户处理的审批节点，不记录其他类型节点
        // 这样 currentNodeKey 就只显示当前等待审批的节点
        if (FlowNodeType.APPROVE.getCode().equals(nodeType)) {
            // 只有审批节点才更新 currentNodeKey
            instance.setCurrentNodeKey(node.getNodeKey());
            instance.setCurrentNodeName(node.getNodeName());
            flowInstanceMapper.updateById(instance);
        }

        if (FlowNodeType.APPROVE.getCode().equals(nodeType)) {
            // 审批节点：创建待办任务
            // 【重要】审批节点的后续节点不在这里处理！
            // 审批节点的后续节点应该由 checkLogicNodesAfterApproval 在审批通过后来触发
            // 这里只创建当前审批节点的任务，不递归处理后续节点
            createApprovalTasks(instance, node, dynamicHandlerMap);
            System.out.println("processNodeAndChildren - 审批节点[" + node.getNodeKey() + "]任务已创建，等待审批通过后触发后续节点");
            // 直接返回，不处理后续节点！后续节点由 checkLogicNodesAfterApproval 处理
            return;

        } else if (FlowNodeType.NOTIFY.getCode().equals(nodeType)) {
            // 通知节点：自动触发
            createAutoTask(instance.getInstanceId(), node, "notify", "通知节点自动执行");
            logService.saveLog(instance.getInstanceId(), null, null,
                    FlowOperationType.NOTIFY.getCode(), "通知节点[" + node.getNodeName() + "]自动执行");

        } else if (FlowNodeType.END.getCode().equals(nodeType)) {
            // 【修复】结束节点：不立即创建任务，只记录日志
            // 结束节点的任务应该在所有审批节点完成后，由 checkAndCompleteInstance 统一创建
            logService.saveLog(instance.getInstanceId(), null, null,
                    FlowOperationType.COMPLETE.getCode(), "流程到达结束节点，等待所有审批节点完成");
            // 直接返回，不创建任务，不继续处理后续节点
            return;

        } else if (FlowNodeType.TEXT.getCode().equals(nodeType)) {
            // 文本节点：自动通过
            createAutoTask(instance.getInstanceId(), node, "auto", "文本节点自动通过");
            logService.saveLog(instance.getInstanceId(), null, null,
                    FlowOperationType.PASS.getCode(), "文本节点[" + node.getNodeName() + "]自动通过");

        } else if (FlowNodeType.LOGIC_AND.getCode().equals(nodeType) || FlowNodeType.LOGIC_OR.getCode().equals(nodeType)) {
            // 【修复】逻辑节点：不应该自动通过！
            // 逻辑节点需要等待其所有前置审批节点完成后才能判断是否流转
            System.out.println("processNodeAndChildren - 处理逻辑节点: " + node.getNodeKey() + ", type: " + nodeType);
            
            // 查找前置审批节点
            List<FlowNodeConfig> predecessors = findPredecessorApprovalNodes(node, allNodes, flowLines, nodeKeyMap, nodeIdMap);
            System.out.println("processNodeAndChildren - 逻辑节点[" + node.getNodeKey() + "]找到 " + predecessors.size() + " 个前置审批节点");
            
            if (predecessors.isEmpty()) {
                // 如果没有前置审批节点，逻辑节点可以直接通过
                createAutoTask(instance.getInstanceId(), node, "auto", 
                        FlowNodeType.LOGIC_AND.getCode().equals(nodeType) ? "逻辑与节点自动通过" : "逻辑或节点自动通过");
                logService.saveLog(instance.getInstanceId(), null, null,
                        FlowOperationType.PASS.getCode(), 
                        FlowNodeType.LOGIC_AND.getCode().equals(nodeType) 
                            ? "逻辑与节点[" + node.getNodeName() + "]自动通过（无前置审批节点）"
                            : "逻辑或节点[" + node.getNodeName() + "]自动通过（无前置审批节点）");
            } else {
                // 有前置审批节点，记录等待状态，但不需要创建任务
                String logicType = FlowNodeType.LOGIC_AND.getCode().equals(nodeType) ? "逻辑与" : "逻辑或";
                String predNames = predecessors.stream()
                        .map(FlowNodeConfig::getNodeName)
                        .collect(Collectors.joining(", "));
                logService.saveLog(instance.getInstanceId(), null, null,
                        FlowOperationType.PASS.getCode(), 
                        "【" + logicType + "节点】[" + node.getNodeName() + "]等待前置审批节点完成：" + predNames);
                
                // 不继续流转到后续节点！后续节点需要等逻辑节点条件满足后才能触发
                return;
            }
        }

        // 查找后续节点（支持并行分支）
        List<FlowNodeConfig> nextNodes = findNextNodesByLines(node, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        
        if (nextNodes.isEmpty()) {
            // 没有后续节点，可能是结束节点或兼容模式
            // 检查是否是结束节点
            // 【修复】逻辑节点不应该在这里调用 checkAndCompleteInstance，
            // 逻辑节点的完成检查由 checkLogicNodesAfterApproval 在条件满足后统一处理
            if (FlowNodeType.END.getCode().equals(nodeType)) {
                checkAndCompleteInstance(instance);
            }
            // LOGIC_AND/OR 节点如果后续为空，只记录日志，不触发完成检查
            // 它们的完成由 checkLogicNodesAfterApproval 统一处理
            return;
        }

        // 递归处理所有后续节点
        for (FlowNodeConfig nextNode : nextNodes) {
            if (!processedNodes.contains(nextNode.getNodeKey())) {
                processNodeAndChildren(instance, nextNode, allNodes, flowLines, 
                        nodeKeyMap, nodeIdMap, dynamicHandlerMap, processedNodes);
            }
        }
    }

    /**
     * 创建审批任务
     */
    private void createApprovalTasks(FlowInstance instance, FlowNodeConfig node,
            Map<String, DynamicHandlerDTO> dynamicHandlerMap) {

        // role_dynamic_user 节点：需要前端已选中审批人，直接用 dynamicHandlerMap 中的值创建任务
        if ("role_dynamic_user".equals(node.getHandlerType())) {
            DynamicHandlerDTO nodeHandler = dynamicHandlerMap.get(node.getNodeKey());
            if (nodeHandler == null || nodeHandler.getHandlerId() == null) {
                System.out.println("createApprovalTasks - 警告：节点 " + node.getNodeKey() + " 是 role_dynamic_user 类型，但未选中审批人，跳过预分配");
                return;
            }
            // 直接取选中的单个用户创建任务
            SysUser handler = sysUserService.getById(nodeHandler.getHandlerId());
            FlowTask task = new FlowTask();
            task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
            task.setInstanceId(instance.getInstanceId());
            task.setNodeKey(node.getNodeKey());
            task.setNodeName(node.getNodeName());
            task.setNodeType(node.getNodeType());
            task.setHandlerId(nodeHandler.getHandlerId());
            task.setHandlerName(handler != null ? handler.getUsername() : "");
            task.setStatus(0);
            task.setTenantCode(nodeHandler.getTenantCode());
            task.setSourceOrgId(nodeHandler.getSourceOrgId());
            flowTaskMapper.insert(task);
            logService.saveLog(instance.getInstanceId(), null, null,
                    FlowOperationType.INIT.getCode(),
                    "审批节点[" + node.getNodeName() + "]已分配处理人：" + task.getHandlerName());
            return;
        }

        // 判断该节点是否为多租户审批（审批人模块是 multiTenant=1 且 handlerType=role）
        String selectedTenant = null;
        boolean isMultiTenantNode = "role".equals(node.getHandlerType())
                && sysModuleService.isMultiTenant(node.getModuleCode());

        if (isMultiTenantNode) {
            // 优先从节点专属配置读取租户（前端在 nodeConfigs 中配置的），
            // 如果没有则降级使用全局 nodeTenantsThreadLocal（兼容旧逻辑）
            DynamicHandlerDTO nodeHandler = dynamicHandlerMap.get(node.getNodeKey());
            String nodeSpecificTenant = null;
            if (nodeHandler != null && nodeHandler.getTenantCode() != null) {
                nodeSpecificTenant = nodeHandler.getTenantCode();
            } else {
                List<String> threadTenants = nodeTenantsThreadLocal.get();
                if (threadTenants != null && !threadTenants.isEmpty()) {
                    nodeSpecificTenant = threadTenants.get(0);
                }
            }
            if (nodeSpecificTenant == null) {
                System.out.println("createApprovalTasks - 警告：节点 " + node.getNodeKey() + " 是多租户审批节点，但未传入租户选择");
                logService.saveLog(instance.getInstanceId(), null, null,
                        FlowOperationType.INIT.getCode(),
                        "审批节点[" + node.getNodeName() + "]是多租户审批节点，但未选择租户，无法分配处理人");
                return;
            }
            System.out.println("createApprovalTasks - 多租户审批节点[" + node.getNodeKey() + "]，允许的租户: " + nodeSpecificTenant);
            // 后续使用节点专属的租户
            selectedTenant = nodeSpecificTenant;
        }

        // 判断 role 类型审批是否有 org_related=1 的角色
        String sourceOrgId = null;
        boolean hasOrgRelatedRole = false;
        if ("role".equals(node.getHandlerType()) && StringUtils.hasText(node.getHandlerIds())) {
            for (String roleId : node.getHandlerIds().split(",")) {
                if (isOrgRelatedRole(roleId)) {
                    hasOrgRelatedRole = true;
                    break;
                }
            }
        }
        if (hasOrgRelatedRole) {
            // 从节点专属配置或全局 ThreadLocal 取 sourceOrgId
            DynamicHandlerDTO nodeHandler = dynamicHandlerMap.get(node.getNodeKey());
            if (nodeHandler != null && nodeHandler.getSourceOrgId() != null) {
                sourceOrgId = nodeHandler.getSourceOrgId();
            } else {
                sourceOrgId = nodeSourceOrgIdThreadLocal.get();
            }
            if (sourceOrgId == null) {
                System.out.println("createApprovalTasks - 警告：节点 " + node.getNodeKey() + " 是机构相关审批节点，但未传入发起机构");
                logService.saveLog(instance.getInstanceId(), null, null,
                        FlowOperationType.INIT.getCode(),
                        "审批节点[" + node.getNodeName() + "]是机构相关审批节点，但未选择发起机构，无法分配处理人");
                return;
            }
            System.out.println("createApprovalTasks - 机构相关审批节点[" + node.getNodeKey() + "]，发起机构ID: " + sourceOrgId);
        }

        // 收集所有符合条件的处理人及其归属租户
        Set<String> allHandlerIds = new HashSet<>();
        Map<String, String> handlerTenantMap = new HashMap<>(); // handlerId -> tenantCode

        if (isMultiTenantNode) {
            // 多租户审批：使用单选租户查询
            String handlerIds = getRealHandlerIdsWithOrgFilter(node, selectedTenant, sourceOrgId, dynamicHandlerMap);
            if (StringUtils.hasText(handlerIds)) {
                for (String hid : handlerIds.split(",")) {
                    if (StringUtils.hasText(hid)) {
                        allHandlerIds.add(hid.trim());
                        handlerTenantMap.put(hid.trim(), selectedTenant);
                    }
                }
            }
        } else {
            // 非多租户审批：使用流程实例的租户
            String handlerIds = getRealHandlerIdsWithOrgFilter(node, instance.getTenantCode(), sourceOrgId, dynamicHandlerMap);
            if (StringUtils.hasText(handlerIds)) {
                for (String hid : handlerIds.split(",")) {
                    if (StringUtils.hasText(hid)) {
                        allHandlerIds.add(hid.trim());
                        handlerTenantMap.put(hid.trim(), instance.getTenantCode());
                    }
                }
            }
        }

        String handlerIds = String.join(",", allHandlerIds);
        String handlerNames = getHandlerNames(node, handlerIds, dynamicHandlerMap);

        if (allHandlerIds.isEmpty()) {
            String msg = "审批节点[" + node.getNodeName() + "]未匹配到可用处理人，请检查角色、租户与发起机构配置";
            System.out.println("createApprovalTasks - 错误：" + msg);
            logService.saveLog(instance.getInstanceId(), null, null,
                    FlowOperationType.INIT.getCode(), msg);
            throw new RuntimeException(msg);
        }

        System.out.println("createApprovalTasks - handlerIds: " + handlerIds + ", handlerNames: " + handlerNames);

        for (String handlerId : allHandlerIds) {
            // 幂等检查：如果该 handler 的任务已存在，不再重复创建
            Long existingCount = flowTaskMapper.selectCount(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, instance.getInstanceId())
                            .eq(FlowTask::getNodeKey, node.getNodeKey())
                            .eq(FlowTask::getHandlerId, Long.parseLong(handlerId))
                            .eq(FlowTask::getDeleted, 0)
            );
            if (existingCount != null && existingCount > 0) {
                continue;
            }

            SysUser handler = sysUserService.getById(Long.parseLong(handlerId));
            FlowTask task = new FlowTask();
            task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
            task.setInstanceId(instance.getInstanceId());
            task.setNodeKey(node.getNodeKey());
            task.setNodeName(node.getNodeName());
            task.setNodeType(node.getNodeType());
            task.setHandlerId(Long.parseLong(handlerId));
            task.setHandlerName(handler != null ? handler.getUsername() : "");
            task.setStatus(0); // 待处理
            // 多租户审批节点：记录该用户归属的租户编码，用于过滤"我的审批"
            task.setTenantCode(handlerTenantMap.get(handlerId));
            // 机构相关审批：记录发起机构ID，用于审批时的机构层级过滤
            task.setSourceOrgId(sourceOrgId);
            flowTaskMapper.insert(task);
        }

        logService.saveLog(instance.getInstanceId(), null, null,
                FlowOperationType.INIT.getCode(),
                "审批节点[" + node.getNodeName() + "]已分配处理人：" + handlerNames);

        // 模拟通知
        if ("1".equals(node.getEnableNotify())) {
            String handlerTypeText = "role".equals(node.getHandlerType()) ? "按角色" : ("user".equals(node.getHandlerType()) ? "按用户" : ("role_dynamic_user".equals(node.getHandlerType()) ? "角色+动态用户" : "动态用户"));
            String notifyContent = node.getNotifyContent() != null ? node.getNotifyContent() : "";
            
            System.out.println("========== 审批节点通知模拟 ==========");
            System.out.println("节点名称: " + node.getNodeName());
            System.out.println("通知方式: " + node.getNotifyType());
            System.out.println("审批人类型: " + handlerTypeText);
            System.out.println("审批人: " + handlerNames);
            System.out.println("通知内容模板: " + (notifyContent.isEmpty() ? "（未填写）" : notifyContent));
            System.out.println("======================================");

            logService.saveLog(instance.getInstanceId(), null, null,
                    FlowOperationType.NOTIFY.getCode(),
                    "【审批通知】通知方式：" + node.getNotifyType() + "，内容：" + (notifyContent.isEmpty() ? "（未填写）" : notifyContent));
        }
    }

    /**
     * 检查是否所有分支都到达结束节点
     */
    private void checkAndCompleteInstance(FlowInstance instance) {
        System.out.println(">>> checkAndCompleteInstance 被调用 <<<");
        
        // 查找所有结束节点的任务
        List<FlowTask> endTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getInstanceId())
                        .eq(FlowTask::getDeleted, 0)
                        .eq(FlowTask::getNodeType, FlowNodeType.END.getCode())
        );

        // 获取所有任务
        List<FlowTask> allTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getInstanceId())
                        .eq(FlowTask::getDeleted, 0)
        );

        System.out.println("checkAndCompleteInstance - 实例ID: " + instance.getInstanceId() + ", 状态: " + instance.getStatus());
        System.out.println("checkAndCompleteInstance - 结束节点任务数: " + endTasks.size() + ", 总任务数: " + allTasks.size());
        
        // 打印所有任务的状态
        System.out.println("checkAndCompleteInstance - 所有任务状态:");
        for (FlowTask t : allTasks) {
            System.out.println("  - " + t.getNodeKey() + " (" + t.getNodeType() + "): status=" + t.getStatus());
        }

        // 【修复】检查是否有未完成的审批任务
        // 关键：一个 nodeKey 可能有多个任务（多 handler 审批），需要取该 nodeKey 的最高状态来判断
        // status 语义: 0=待处理, 1=通过, 2=驳回, 3=回调中
        Map<String, Integer> nodeMaxStatus = new HashMap<>();
        for (FlowTask t : allTasks) {
            String nodeKey = t.getNodeKey();
            Integer existing = nodeMaxStatus.get(nodeKey);
            if (existing == null || t.getStatus() > existing) {
                nodeMaxStatus.put(nodeKey, t.getStatus());
            }
        }

        boolean allCompleted = true;
        for (Map.Entry<String, Integer> entry : nodeMaxStatus.entrySet()) {
            String nodeKey = entry.getKey();
            Integer status = entry.getValue();

            // 直接使用 task 的 nodeType（FlowTask 在创建时就保存了 nodeType）
            // 找出该 nodeKey 对应的节点类型
            String nodeType = null;
            for (FlowTask t : allTasks) {
                if (t.getNodeKey().equals(nodeKey)) {
                    nodeType = t.getNodeType();
                    break;
                }
            }
            if (nodeType == null) continue;

            // 结束、开始、文本、逻辑节点 不参与完成检查
            if (FlowNodeType.END.getCode().equals(nodeType)
                    || FlowNodeType.START.getCode().equals(nodeType)
                    || FlowNodeType.TEXT.getCode().equals(nodeType)
                    || FlowNodeType.LOGIC_AND.getCode().equals(nodeType)
                    || FlowNodeType.LOGIC_OR.getCode().equals(nodeType)) {
                continue;
            }
            // 【新增】status = 5（已跳过）视为已完成，不阻塞流程
            // status = 1 表示已通过，视为完成
            // 其他状态（0=待处理, 2=驳回, 3=业务执行中, 4=逻辑处理失败）表示未完成
            if (status != 1 && status != 5) {
                allCompleted = false;
                System.out.println("checkAndCompleteInstance - 发现未完成任务: " + nodeKey + ", status=" + status);
                break;
            }
        }

        System.out.println("checkAndCompleteInstance - 全部完成: " + allCompleted);

        // 如果有结束节点任务且所有审批任务都已完成，更新流程状态
        if (allCompleted) {
            System.out.println("checkAndCompleteInstance - 准备更新流程状态为已完成");
            instance.setStatus(FlowInstanceStatus.COMPLETED.getCode());
            instance.setCurrentNodeKey("");
            instance.setCurrentNodeName("");
            flowInstanceMapper.updateById(instance);

            // 【修复】创建结束节点的任务（如果不存在）
            if (endTasks.isEmpty()) {
                // 获取结束节点配置
                List<FlowNodeConfig> nodeConfigs = flowNodeConfigMapper.selectList(
                        new LambdaQueryWrapper<FlowNodeConfig>()
                                .eq(FlowNodeConfig::getFlowCode, instance.getFlowCode())
                                .eq(FlowNodeConfig::getNodeType, FlowNodeType.END.getCode())
                );
                if (!nodeConfigs.isEmpty()) {
                    FlowNodeConfig endNode = nodeConfigs.get(0);
                    createAutoTask(instance.getInstanceId(), endNode, "auto", "流程结束");
                    System.out.println("checkAndCompleteInstance - 为结束节点创建任务");
                }
            }

            // 重新查询验证
            FlowInstance updated = flowInstanceMapper.selectById(instance.getInstanceId());
            System.out.println("checkAndCompleteInstance - 更新后流程状态: " + updated.getStatus());
            
            logService.saveLog(instance.getInstanceId(), null, null,
                    FlowOperationType.COMPLETE.getCode(), "流程所有节点已完成，状态改为已完成");
        }
    }

    /**
     * 判断用户在 bi_workstation 模块下是否拥有 SUPER_ADMIN 角色
     */
    private boolean isSuperAdminForBiWorkstation(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        for (SysUserRole ur : userRoles) {
            if ("SUPER_ADMIN".equals(ur.getRoleCode()) && "bi_workstation".equals(ur.getModuleCode())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void approveFlow(String taskId, String action, String comment, Long userId) {
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        // bi_workstation 模块的 SUPER_ADMIN 用户拥有所有流程的审批权限，跳过 handlerId 检查
        boolean isSuperAdmin = isSuperAdminForBiWorkstation(userId);
        if (!isSuperAdmin && (!task.getHandlerId().equals(userId) || task.getStatus() != 0)) {
            throw new RuntimeException("无权限处理该任务或任务已处理");
        }

        task.setAction(action);
        task.setComment(comment);
        task.setExecuteTime(new Date());

        FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
        loadDynamicHandlersToThreadLocal(instance);

        SysUser operator = sysUserService.getById(userId);
        String actionName = "approve".equals(action) ? "审批通过" : "审批驳回";

        logService.saveLog(instance.getInstanceId(), userId, operator.getUsername(),
                action, "用户" + operator.getUsername() + "对节点[" + task.getNodeName() + "]" + actionName + "，意见：" + comment);

        System.out.println(">>> approveFlow - 用户[" + operator.getUsername() + "]审批通过节点[" + task.getNodeName()
                + "(key=" + task.getNodeKey() + ")]");

        // 查询该节点的配置信息（审批通过和驳回都可能用到）
        FlowNodeConfig nodeConfig = flowNodeConfigMapper.selectOne(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, instance.getFlowCode())
                        .eq(FlowNodeConfig::getNodeKey, task.getNodeKey())
        );

        if ("reject".equals(action)) {
            // 驳回：流程终止
            task.setStatus(2); // 已驳回
            flowTaskMapper.updateById(task);
            instance.setStatus(FlowInstanceStatus.REJECTED.getCode());
            flowInstanceMapper.updateById(instance);

            // 【新增】驳回时通知业务执行模块
            if (nodeConfig != null && StringUtils.hasText(nodeConfig.getExecuteModules())) {
                notifyModulesOnReject(instance.getInstanceId(), instance.getFlowCode(),
                        task.getNodeKey(), task.getNodeName(), operator, comment);
            }

            dynamicHandlerThreadLocal.remove();
            return;
        }

        // 【新增】检查该节点是否配置了业务执行模块
        boolean hasExecuteModules = nodeConfig != null
                && StringUtils.hasText(nodeConfig.getExecuteModules());

        if (hasExecuteModules) {
            // 【新增】配置了业务执行模块：状态设为"业务执行中"，等待外部回调
            System.out.println(">>> approveFlow - 节点配置了业务执行模块，进入异步执行模式");
            task.setStatus(3); // 业务执行中
            flowTaskMapper.updateById(task);

            // 生成回调令牌
            String callbackToken = UUID.randomUUID().toString();
            task.setCallbackToken(callbackToken);
            flowTaskMapper.updateById(task);

            // 模拟异步调用外部模块
            executeModuleAsync(nodeConfig.getExecuteModules(), instance, task, operator);

            // 记录日志
            logService.saveLog(instance.getInstanceId(), userId, operator.getUsername(),
                    "approve", "节点[" + task.getNodeName() + "]审批通过，业务执行中，等待外部模块回调");
        } else {
            // 【修复】没有配置业务执行模块：状态直接设为"已通过"
            task.setStatus(1); // 已通过
            flowTaskMapper.updateById(task);

            // 跳过同节点的其余并行任务
            skipParallelTasks(instance, task);

            // 【修复】审批通过后，清空 currentNodeKey
            instance.setCurrentNodeKey("");
            instance.setCurrentNodeName("");
            flowInstanceMapper.updateById(instance);

            // 【审批通过】检查后续逻辑节点
            checkLogicNodesAfterApproval(instance, task);
        }

        dynamicHandlerThreadLocal.remove();
    }

    /**
     * 当一个用户审批通过后，跳过同节点的其余并行任务
     * 角色审批模式下，同一节点可能分配给多个用户（同一角色的多个成员），
     * 其中一人审批通过后，其余人的待处理任务应标记为"已跳过"，不再显示在"我的审批"中
     */
    private void skipParallelTasks(FlowInstance instance, FlowTask completedTask) {
        List<FlowTask> parallelTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getInstanceId())
                        .eq(FlowTask::getNodeKey, completedTask.getNodeKey())
                        .eq(FlowTask::getStatus, 0)  // 只处理待处理的任务
                        .eq(FlowTask::getDeleted, 0)
        );
        for (FlowTask parallelTask : parallelTasks) {
            parallelTask.setStatus(5); // 已跳过
            parallelTask.setAction("skip");
            parallelTask.setComment("同节点其他审批人已通过，自动跳过");
            parallelTask.setExecuteTime(new Date());
            flowTaskMapper.updateById(parallelTask);
            logService.saveLog(instance.getInstanceId(), parallelTask.getHandlerId(), parallelTask.getHandlerName(),
                    "skip", "审批节点[" + parallelTask.getNodeName() + "]其他审批人已通过，该任务已自动跳过");
            System.out.println("skipParallelTasks - 跳过并行任务: taskId=" + parallelTask.getTaskId()
                    + ", handler=" + parallelTask.getHandlerName());
        }
    }

    /**
     * 【核心】审批通过后检查逻辑节点
     */
    private void checkLogicNodesAfterApproval(FlowInstance instance, FlowTask completedTask) {
        System.out.println("========================================");
        System.out.println("checkLogicNodesAfterApproval - 开始检查后续逻辑节点");
        System.out.println("checkLogicNodesAfterApproval - 已完成节点: " + completedTask.getNodeKey() + ", " + completedTask.getNodeName());
        
        // 获取流程定义
        FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowCode());
        if (flow == null) {
            System.out.println("checkLogicNodesAfterApproval - 流程定义为空，直接返回");
            return;
        }

        // 获取所有节点和连线
        List<FlowNodeConfig> allNodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, instance.getFlowCode())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        List<FlowLine> flowLines = new ArrayList<>();
        Map<String, String> flowJsonNodeAliasMap = new HashMap<>();
        if (StringUtils.hasText(flow.getFlowJson())) {
            try {
                FlowJsonData flowJsonData = objectMapper.readValue(flow.getFlowJson(), FlowJsonData.class);
                if (flowJsonData != null && flowJsonData.getLines() != null) {
                    flowLines = flowJsonData.getLines();
                    flowJsonNodeAliasMap = buildFlowJsonNodeAliasMap(flowJsonData);
                }
            } catch (Exception e) {
                log.warn("解析 flowJson 失败", e);
            }
        }

        // 构建节点映射
        Map<String, FlowNodeConfig> nodeKeyMap = new HashMap<>();
        Map<String, FlowNodeConfig> nodeIdMap = new HashMap<>();
        for (FlowNodeConfig node : allNodes) {
            nodeKeyMap.put(node.getNodeKey(), node);
            if (StringUtils.hasText(node.getNodeId())) {
                nodeIdMap.put(node.getNodeId(), node);
            }
        }
        mergeFlowJsonAliasesIntoNodeIdMap(nodeKeyMap, nodeIdMap, flowJsonNodeAliasMap);

        // 获取当前节点的配置
        FlowNodeConfig currentNode = nodeKeyMap.get(completedTask.getNodeKey());
        if (currentNode == null) return;

        // 查找所有后续逻辑节点
        List<FlowNodeConfig> logicNodes = findLogicNodesAfter(currentNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        
        System.out.println("checkLogicNodesAfterApproval - 找到 " + logicNodes.size() + " 个后续逻辑节点");
        for (int i = 0; i < logicNodes.size(); i++) {
            System.out.println("  逻辑节点[" + i + "]: nodeKey=" + logicNodes.get(i).getNodeKey()
                    + ", nodeId=" + logicNodes.get(i).getNodeId());
        }

        // 找出所有需要通过 fallback 流转的直接后续节点（这些节点不在逻辑节点控制路径上）
        List<FlowNodeConfig> directNextNodes = findNextNodesByLines(currentNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        System.out.println("checkLogicNodesAfterApproval - 直接后续节点数量: " + directNextNodes.size());
        for (int i = 0; i < directNextNodes.size(); i++) {
            System.out.println("  直接后续[" + i + "]: nodeKey=" + directNextNodes.get(i).getNodeKey());
        }
        Set<String> logicNodeControlledKeys = new HashSet<>();
        for (FlowNodeConfig logicNode : logicNodes) {
            logicNodeControlledKeys.add(logicNode.getNodeKey()); // 逻辑节点自身也受控制
            List<FlowNodeConfig> logicNext = findNextNodesByLines(logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
            System.out.println("checkLogicNodesAfterApproval - 逻辑节点[" + logicNode.getNodeKey() + "]的后续节点: " + logicNext.size());
            for (int i = 0; i < logicNext.size(); i++) {
                System.out.println("  逻辑后续[" + i + "]: nodeKey=" + logicNext.get(i).getNodeKey());
            }
            for (FlowNodeConfig n : logicNext) {
                logicNodeControlledKeys.add(n.getNodeKey());
            }
        }
        System.out.println("checkLogicNodesAfterApproval - 受逻辑节点控制的节点: " + logicNodeControlledKeys);
        // fallback 只处理不受逻辑节点控制的直接后续节点，且排除 LOGIC_AND/OR 节点
        List<FlowNodeConfig> fallbackNodes = new ArrayList<>();
        for (FlowNodeConfig n : directNextNodes) {
            if (!logicNodeControlledKeys.contains(n.getNodeKey())
                    && !FlowNodeType.LOGIC_AND.getCode().equals(n.getNodeType())
                    && !FlowNodeType.LOGIC_OR.getCode().equals(n.getNodeType())) {
                fallbackNodes.add(n);
            }
        }
        System.out.println("checkLogicNodesAfterApproval - fallbackNodes 数量: " + fallbackNodes.size());

        Map<String, DynamicHandlerDTO> dynamicHandlerMap = dynamicHandlerThreadLocal.get();

        // 1. 处理逻辑节点分支（AND/OR 控制的节点，如 end_6）
        for (FlowNodeConfig logicNode : logicNodes) {
            System.out.println("checkLogicNodesAfterApproval - 检查逻辑节点: " + logicNode.getNodeKey() + ", type: " + logicNode.getNodeType());

            boolean canPass = checkLogicNodeCondition(instance, logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);

            System.out.println("checkLogicNodesAfterApproval - 条件检查结果: " + canPass);

            if (canPass) {
                // 【新增】如果是逻辑或节点，需要跳过其余并列的审批节点
                if (FlowNodeType.LOGIC_OR.getCode().equals(logicNode.getNodeType())) {
                    skipParallelApprovalNodes(instance, logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
                }

                // 条件满足，流转逻辑节点控制的节点
                logService.saveLog(instance.getInstanceId(), null, null,
                        FlowOperationType.PASS.getCode(),
                        "逻辑节点[" + logicNode.getNodeName() + "]条件满足，流转到后续节点");

                List<FlowNodeConfig> nextNodes = findNextNodesByLines(logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);

                Set<String> processedNodes = new HashSet<>();
                processedNodes.add(logicNode.getNodeKey());

                for (FlowNodeConfig nextNode : nextNodes) {
                    if (!processedNodes.contains(nextNode.getNodeKey())) {
                        processNodeAndChildren(instance, nextNode, allNodes, flowLines,
                                nodeKeyMap, nodeIdMap, dynamicHandlerMap, processedNodes);
                    }
                }

                System.out.println("checkLogicNodesAfterApproval - 逻辑节点处理完成，调用 checkAndCompleteInstance");
                checkAndCompleteInstance(instance);
            } else {
                // 条件不满足，记录日志，但不阻止 fallback
                List<String> pendingNodes = getPendingPredecessorNodes(instance, logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
                String pendingStr = String.join(", ", pendingNodes);
                logService.saveLog(instance.getInstanceId(), null, null,
                        "logic_wait", "审批通过[" + completedTask.getNodeName() + "]，逻辑节点[" + logicNode.getNodeName() + "]等待其他前置审批完成：" + pendingStr);
            }
        }

        // 2. fallback：处理不受逻辑节点控制的直接后续节点（如 shenpi）
        // 以及受逻辑节点控制的审批节点（它们不能等待，只能主动触发）
        System.out.println("checkLogicNodesAfterApproval - fallback 处理中...");
        System.out.println("checkLogicNodesAfterApproval - 受控节点: " + logicNodeControlledKeys);

        // 【BUG修复】找出需要主动触发的受控审批节点（不在逻辑节点本身，只在逻辑节点的后续中）
        // 关键：只有当逻辑与(AND)节点的条件已满足，或逻辑或(OR)节点的条件已满足时，
        // 才能触发其控制的审批节点。逻辑与节点在所有并行分支完成前不得触发后续审批节点。
        Set<String> controlledApprovalNodeKeys = new HashSet<>();
        for (FlowNodeConfig logicNode : logicNodes) {
            boolean conditionMet = checkLogicNodeCondition(instance, logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
            if (conditionMet) {
                // 只有条件满足的逻辑节点，才将其后续审批节点加入触发列表
                List<FlowNodeConfig> logicNext = findNextNodesByLines(logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
                for (FlowNodeConfig n : logicNext) {
                    controlledApprovalNodeKeys.add(n.getNodeKey());
                }
            } else {
                // 逻辑与(AND)节点条件不满足时，打印日志记录等待状态
                List<String> pendingNodes = getPendingPredecessorNodes(instance, logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
                System.out.println("checkLogicNodesAfterApproval - 逻辑与节点[" + logicNode.getNodeKey()
                        + "]条件未满足，等待: " + String.join(", ", pendingNodes));
            }
        }

        System.out.println("checkLogicNodesAfterApproval - 需要主动触发的受控审批节点: " + controlledApprovalNodeKeys);

        // 主动触发受控的审批节点
        if (!controlledApprovalNodeKeys.isEmpty()) {
            Set<String> triggeredProcessedNodes = new HashSet<>();
            triggeredProcessedNodes.add(currentNode.getNodeKey());
            for (String controlledKey : controlledApprovalNodeKeys) {
                FlowNodeConfig controlledNode = nodeKeyMap.get(controlledKey);
                if (controlledNode != null && FlowNodeType.APPROVE.getCode().equals(controlledNode.getNodeType())) {
                    System.out.println("checkLogicNodesAfterApproval - 主动触发受控审批节点: " + controlledKey);
                    processNodeAndChildren(instance, controlledNode, allNodes, flowLines,
                            nodeKeyMap, nodeIdMap, dynamicHandlerMap, triggeredProcessedNodes);
                }
            }
        }

        // 处理不受控制的 fallback 节点
        if (!fallbackNodes.isEmpty()) {
            System.out.println("checkLogicNodesAfterApproval - fallback 节点不为空，正常流转后续节点: " + fallbackNodes.size());

            Set<String> processedNodes = new HashSet<>();
            processedNodes.add(currentNode.getNodeKey());

            for (FlowNodeConfig nextNode : fallbackNodes) {
                if (!processedNodes.contains(nextNode.getNodeKey())) {
                    System.out.println("checkLogicNodesAfterApproval - fallback 处理节点: " + nextNode.getNodeKey());
                    processNodeAndChildren(instance, nextNode, allNodes, flowLines,
                            nodeKeyMap, nodeIdMap, dynamicHandlerMap, processedNodes);
                }
            }

            System.out.println("checkLogicNodesAfterApproval - fallback 处理完成，调用 checkAndCompleteInstance");
            checkAndCompleteInstance(instance);
        } else {
            // 没有后续节点，检查是否是结束节点
            if (FlowNodeType.END.getCode().equals(currentNode.getNodeType())) {
                System.out.println("checkLogicNodesAfterApproval - 当前节点是结束节点，调用 checkAndCompleteInstance");
                checkAndCompleteInstance(instance);
            } else {
                System.out.println("checkLogicNodesAfterApproval - fallbackNodes 为空，且当前节点不是结束节点，不调用 checkAndCompleteInstance");
            }
        }
    }
    /*
     * 查找某个节点后的所有逻辑节点
     */
    private List<FlowNodeConfig> findLogicNodesAfter(FlowNodeConfig currentNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {
        
        List<FlowNodeConfig> logicNodes = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> toVisit = new LinkedList<>();
        
        // 从当前节点的下一个节点开始查找
        List<FlowNodeConfig> directNext = findNextNodesByLines(currentNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        for (FlowNodeConfig node : directNext) {
            toVisit.add(node.getNodeKey());
        }
        
        while (!toVisit.isEmpty()) {
            String nodeKey = toVisit.poll();
            if (visited.contains(nodeKey)) continue;
            visited.add(nodeKey);
            
            FlowNodeConfig node = nodeKeyMap.get(nodeKey);
            if (node == null) continue;
            
            if (FlowNodeType.isLogicNode(node.getNodeType())) {
                logicNodes.add(node);
                // 找到逻辑节点后，不再继续往前找了（逻辑节点之后的节点由逻辑节点控制）
                continue;
            }
            
            // 继续查找后续节点
            List<FlowNodeConfig> nextNodes = findNextNodesByLines(node, allNodes, flowLines, nodeKeyMap, nodeIdMap);
            for (FlowNodeConfig next : nextNodes) {
                if (!visited.contains(next.getNodeKey())) {
                    toVisit.add(next.getNodeKey());
                }
            }
        }
        
        return logicNodes;
    }

    /**
     * 【核心】检查逻辑节点条件是否满足
     */
    private boolean checkLogicNodeCondition(FlowInstance instance, FlowNodeConfig logicNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {
        
        System.out.println("checkLogicNodeCondition - logicNode: " + logicNode.getNodeKey() + ", type: " + logicNode.getNodeType());

        // 查找所有指向该逻辑节点的审批节点
        List<FlowNodeConfig> predecessorNodes = findPredecessorApprovalNodes(logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        
        System.out.println("checkLogicNodeCondition - 找到 " + predecessorNodes.size() + " 个前置审批节点");
        for (int i = 0; i < predecessorNodes.size(); i++) {
            System.out.println("  前置节点[" + i + "]: nodeKey=" + predecessorNodes.get(i).getNodeKey()
                    + ", nodeId=" + predecessorNodes.get(i).getNodeId());
        }

        if (predecessorNodes.isEmpty()) {
            // 【BUG追踪】记录详细诊断信息
            log.warn("[BUG追踪] checkLogicNodeCondition - 逻辑节点[{}]没有找到前置审批节点！"
                    + "flowLines数量={}, allNodes数量={}, logicKey={}, logicNodeId={}",
                    logicNode.getNodeName(), flowLines.size(), allNodes.size(), logicNode.getNodeKey(), logicNode.getNodeId());
            for (int i = 0; i < Math.min(10, flowLines.size()); i++) {
                FlowLine line = flowLines.get(i);
                log.warn("  flowLines[{}]: fromNode={}, toNode={}", i, line.getFromNode(), line.getToNode());
            }
            log.warn("  nodeKeyMap keys: {}", String.join(", ", nodeKeyMap.keySet()));
            log.warn("  nodeIdMap keys: {}", String.join(", ", nodeIdMap.keySet()));
        }

        if (predecessorNodes.isEmpty()) {
            // 【调试】没有前置审批节点，记录警告日志（不代表BUG，见兼容模式说明）
            log.warn("[调试] 逻辑节点[{}]没有前置审批节点，条件满足（自动通过）", logicNode.getNodeName());
            return true;
        }

        // 获取这些审批节点的任务状态
        List<FlowTask> allTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getInstanceId())
                        .eq(FlowTask::getDeleted, 0)
        );

        Map<String, Integer> taskStatusMap = new HashMap<>();
        for (FlowTask t : allTasks) {
            Integer existingStatus = taskStatusMap.get(t.getNodeKey());
            if (existingStatus == null || t.getStatus() > existingStatus) {
                taskStatusMap.put(t.getNodeKey(), t.getStatus());
            }
        }

        // 统计已通过的前置审批节点
        int approvedCount = 0;
        for (FlowNodeConfig predNode : predecessorNodes) {
            Integer status = taskStatusMap.get(predNode.getNodeKey());
            System.out.println("checkLogicNodeCondition - 前置节点 " + predNode.getNodeKey() + " 状态: " + (status == null ? "无任务" : status));
            if (status != null && status == 1) {
                approvedCount++;
            }
        }

        int totalCount = predecessorNodes.size();
        System.out.println("checkLogicNodeCondition - 已通过: " + approvedCount + "/" + totalCount);

        if (FlowNodeType.LOGIC_AND.getCode().equals(logicNode.getNodeType())) {
            // 逻辑与：所有前置审批节点都通过
            return approvedCount == totalCount;
        } else if (FlowNodeType.LOGIC_OR.getCode().equals(logicNode.getNodeType())) {
            // 逻辑或：任意一个前置审批节点通过
            return approvedCount >= 1;
        }

        return false;
    }

    /**
     * 【新增】跳过逻辑或节点控制的并列审批节点
     * 当逻辑或节点的任意一个前置审批节点通过后，其余并列的待处理审批节点将被标记为"已跳过"
     */
    private void skipParallelApprovalNodes(FlowInstance instance, FlowNodeConfig logicNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {
        
        System.out.println("skipParallelApprovalNodes - 开始处理逻辑或节点的并列审批节点");
        
        // 获取逻辑或节点的所有前置审批节点
        List<FlowNodeConfig> predecessorNodes = findPredecessorApprovalNodes(logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        
        if (predecessorNodes.isEmpty()) {
            return;
        }
        
        // 获取当前实例的所有任务
        List<FlowTask> allTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getInstanceId())
                        .eq(FlowTask::getDeleted, 0)
        );
        
        // 找出所有前置审批节点中状态为"待处理"的任务（status=0）
        // 这些任务对应的是与已通过审批节点并列的其他审批节点
        List<String> skipNodeKeys = new ArrayList<>();
        for (FlowNodeConfig predNode : predecessorNodes) {
            for (FlowTask task : allTasks) {
                if (task.getNodeKey().equals(predNode.getNodeKey()) && task.getStatus() == 0) {
                    skipNodeKeys.add(predNode.getNodeKey());
                    System.out.println("skipParallelApprovalNodes - 将跳过节点: " + predNode.getNodeName() + " (key=" + predNode.getNodeKey() + ")");
                }
            }
        }
        
        // 将并列的待处理审批节点标记为"已跳过"（status=5）
        for (String nodeKey : skipNodeKeys) {
            // 查找该节点的所有待处理任务
            List<FlowTask> tasksToSkip = flowTaskMapper.selectList(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, instance.getInstanceId())
                            .eq(FlowTask::getNodeKey, nodeKey)
                            .eq(FlowTask::getStatus, 0)
                            .eq(FlowTask::getDeleted, 0)
            );
            
            for (FlowTask task : tasksToSkip) {
                // 【修复】必须用 LambdaUpdateWrapper，否则 handlerId=null 不会清空数据库字段
                LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<FlowTask>()
                        .eq(FlowTask::getTaskId, task.getTaskId())
                        .eq(FlowTask::getDeleted, 0)
                        .set(FlowTask::getStatus, 5) // 已跳过
                        .set(FlowTask::getAction, "skip")
                        .set(FlowTask::getComment, "因逻辑或节点其他分支已通过，该节点被自动跳过")
                        .set(FlowTask::getExecuteTime, new Date());
                flowTaskMapper.update(null, updateWrapper);
                
                // 记录操作日志
                logService.saveLog(instance.getInstanceId(), null, null,
                        "skip", "审批节点[" + task.getNodeName() + "]因逻辑或条件满足被自动跳过");
                
                System.out.println("skipParallelApprovalNodes - 已将节点[" + task.getNodeName() + "]标记为已跳过");
            }
        }
        
        if (!skipNodeKeys.isEmpty()) {
            logService.saveLog(instance.getInstanceId(), null, null,
                    "skip", "逻辑或节点条件满足，跳过并列审批节点：" + String.join(", ", skipNodeKeys));
        }
    }

    /**
     * 查找所有指向指定节点（包括逻辑节点）的审批节点
     * 支持递归穿透 LOGIC_AND/LOGIC_OR 节点向上查找真正的审批前置节点
     */
    private List<FlowNodeConfig> findPredecessorApprovalNodes(FlowNodeConfig targetNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {

        List<FlowNodeConfig> predecessors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        // 直接以 targetNode 作为起点（而不是找指向它的线）
        queue.add(targetNode.getNodeKey());
        visited.add(targetNode.getNodeKey());

        while (!queue.isEmpty()) {
            String currentKey = queue.poll();
            FlowNodeConfig current = nodeKeyMap.get(currentKey);
            if (current == null) current = nodeIdMap.get(currentKey);
            if (current == null) continue;

            // 查找所有指向 current 的前置节点（反向遍历连线）
            for (FlowLine line : flowLines) {
                String toNode = line.getToNode();
                String fromNodeKey = line.getFromNode();
                if (!StringUtils.hasText(toNode) || !StringUtils.hasText(fromNodeKey)) {
                    continue;
                }
                FlowNodeConfig toNodeCfg = nodeIdMap.get(toNode);
                if (toNodeCfg == null) {
                    toNodeCfg = nodeKeyMap.get(toNode);
                }
                if (toNodeCfg == null || !Objects.equals(toNodeCfg.getNodeKey(), current.getNodeKey())) {
                    continue;
                }
                String fromKey = fromNodeKey;
                FlowNodeConfig fromNode = nodeIdMap.get(fromKey);
                if (fromNode == null) fromNode = nodeKeyMap.get(fromKey);
                if (fromNode == null) continue;

                if (FlowNodeType.APPROVE.getCode().equals(fromNode.getNodeType())) {
                    if (!predecessors.contains(fromNode)) {
                        predecessors.add(fromNode);
                    }
                } else if (FlowNodeType.isLogicNode(fromNode.getNodeType())) {
                    // LOGIC_AND/LOGIC_OR 节点：递归穿透，继续向上找
                    if (!visited.contains(fromNode.getNodeKey())) {
                        visited.add(fromNode.getNodeKey());
                        queue.add(fromNode.getNodeKey());
                    }
                }
                // START 节点或其他类型节点：忽略
            }
        }

        return predecessors;
    }

    /**
     * 获取等待中的前置节点名称
     */
    private List<String> getPendingPredecessorNodes(FlowInstance instance, FlowNodeConfig logicNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {
        
        List<String> pending = new ArrayList<>();
        List<FlowNodeConfig> predecessors = findPredecessorApprovalNodes(logicNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        
        List<FlowTask> allTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getInstanceId())
                        .eq(FlowTask::getDeleted, 0)
        );

        Map<String, Integer> taskStatusMap = new HashMap<>();
        for (FlowTask t : allTasks) {
            taskStatusMap.put(t.getNodeKey(), t.getStatus());
        }

        for (FlowNodeConfig pred : predecessors) {
            Integer status = taskStatusMap.get(pred.getNodeKey());
            if (status == null || status != 1) {
                pending.add(pred.getNodeName() + (status == null ? "(无任务)" : "(待处理)"));
            }
        }

        return pending;
    }

    @Async
    public void executeModuleAsync(String moduleCode, FlowInstance instance, FlowTask task, SysUser operator) {
        Map<String, Object> params = flowCommonService.getFlowParams(instance.getInstanceId());

        String flowCode = "";
        FlowDefinition flowDefinition = flowDefinitionMapper.selectById(instance.getFlowCode());
        if (flowDefinition != null) {
            flowCode = flowDefinition.getFlowCode();
        }

        String callbackToken = task.getCallbackToken();
        String callbackUrl = "/flow/callback/complete";
        String callbackSuccessUrl = "/flow/callback/success";
        String callbackFailedUrl = "/flow/callback/failed";
        String callbackLogUrl = "/flow/callback/log";

        System.out.println("【异步模块调用】========================================");
        System.out.println("【异步模块调用】目标模块: " + moduleCode);
        System.out.println("【异步模块调用】流程编码(flow_code): " + flowCode);
        System.out.println("【异步模块调用】节点编码(node_key): " + task.getNodeKey());
        System.out.println("【异步模块调用】流程实例ID: " + instance.getInstanceId());
        System.out.println("【异步模块调用】任务ID: " + task.getTaskId());
        System.out.println("【异步模块调用】回调令牌(callback_token): " + callbackToken);
        System.out.println("【异步模块调用】审批人: " + (operator != null ? operator.getUsername() : "系统"));
        System.out.println("【异步模块调用】审批时间: " + (task.getExecuteTime() != null ? task.getExecuteTime() : new Date()));
        System.out.println("【异步模块调用】自定义参数: " + params);
        System.out.println("【异步模块调用】========================================");
        System.out.println("【异步模块调用】回调接口说明：");
        System.out.println("【异步模块调用】- 业务执行成功，调用: POST " + callbackSuccessUrl + "?callbackToken=" + callbackToken + "&message=成功信息");
        System.out.println("【异步模块调用】- 业务执行失败，调用: POST " + callbackFailedUrl + "?callbackToken=" + callbackToken + "&message=失败原因");
        System.out.println("【异步模块调用】- 更新执行日志: POST " + callbackLogUrl + "?callbackToken=" + callbackToken + "&logContent=日志内容");
        System.out.println("【异步模块调用】- 通用回调: POST " + callbackUrl + "?callbackToken=" + callbackToken + "&success=true&message=信息");
        System.out.println("【异步模块调用】========================================");

        TaskCallbackContext ctx = new TaskCallbackContext();
        ctx.setTaskId(task.getTaskId());
        ctx.setInstanceId(instance.getInstanceId());
        ctx.setModuleCode(moduleCode);
        ctx.setNodeKey(task.getNodeKey());
        ctx.setOperatorId(operator != null ? operator.getId() : null);
        ctx.setOperatorName(operator != null ? operator.getUsername() : "系统");
        ctx.setCallbackTime(new Date());
        callbackTokenMap.put(callbackToken, ctx);

        logService.saveLog(instance.getInstanceId(), operator != null ? operator.getId() : null,
                operator != null ? operator.getUsername() : "系统",
                "module_call_async", "异步调用模块[" + moduleCode + "]，flow_code: " + flowCode +
                "，node_key: " + task.getNodeKey() + "，callback_token: " + callbackToken);

        // 【模拟】在这里可以添加实际的HTTP调用逻辑，例如：
        // try {
        //     String moduleEndpoint = "http://module-service/api/execute";
        //     Map<String, Object> requestBody = new HashMap<>();
        //     requestBody.put("flow_code", flowCode);
        //     requestBody.put("node_key", task.getNodeKey());
        //     requestBody.put("instance_id", instance.getInstanceId());
        //     requestBody.put("callback_token", callbackToken);
        //     requestBody.put("callback_url", callbackUrl);
        //     requestBody.put("params", params);
        //     // 发送HTTP请求到外部模块
        //     restTemplate.postForObject(moduleEndpoint, requestBody, String.class);
        // } catch (Exception e) {
        //     // 调用失败，自动触发失败回调
        //     handleModuleCallback(callbackToken, false, "模块调用失败: " + e.getMessage(), null);
        // }
    }

    @Override
    /**
     * 驳回时通知所有涉及的业务执行模块
     */
    public void notifyModulesOnReject(String instanceId, String flowCode, String rejectedNodeKey, String rejectedNodeName, SysUser operator, String comment) {
        Map<String, Object> params = flowCommonService.getFlowParams(instanceId);
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        String instanceName = instance != null ? instance.getInstanceName() : "";
        Long applicantId = instance != null ? instance.getApplicantId() : null;
        String applicantName = "";
        if (applicantId != null) {
            SysUser applicant = sysUserService.getById(applicantId);
            applicantName = applicant != null ? applicant.getUsername() : "";
        }
        String tenantCode = instance != null ? instance.getTenantCode() : null;

        List<FlowTask> allTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instanceId)
                        .eq(FlowTask::getDeleted, 0)
                        .in(FlowTask::getStatus, Arrays.asList(1, 2, 5))
                        .orderByAsc(FlowTask::getCreateTime)
        );

        Set<String> notifiedModules = new LinkedHashSet<>();
        Set<String> failedModules = new LinkedHashSet<>();

        System.out.println("【驳回模块通知】========================================");
        System.out.println("【驳回模块通知】通知类型: 流程驳回");
        System.out.println("【驳回模块通知】流程编码(flow_code): " + flowCode);
        System.out.println("【驳回模块通知】流程实例ID: " + instanceId);
        System.out.println("【驳回模块通知】流程实例名称: " + instanceName);
        System.out.println("【驳回模块通知】申请人: " + applicantName);
        System.out.println("【驳回模块通知】被驳回节点(key): " + rejectedNodeKey);
        System.out.println("【驳回模块通知】被驳回节点(名称): " + rejectedNodeName);
        System.out.println("【驳回模块通知】驳回人: " + (operator != null ? operator.getUsername() : "系统"));
        System.out.println("【驳回模块通知】驳回意见: " + (comment != null ? comment : "无"));
        System.out.println("【驳回模块通知】驳回时间: " + new Date());
        System.out.println("【驳回模块通知】自定义参数: " + params);
        System.out.println("【驳回模块通知】--- 开始通知各个业务执行模块 ---");

        // 1. 通知当前被驳回的节点
        FlowNodeConfig rejectedConfig = flowNodeConfigMapper.selectOne(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, flowCode)
                        .eq(FlowNodeConfig::getNodeKey, rejectedNodeKey)
        );
        if (rejectedConfig != null && StringUtils.hasText(rejectedConfig.getExecuteModules())) {
            for (String module : rejectedConfig.getExecuteModules().split(",")) {
                module = module.trim();
                if (!StringUtils.hasText(module) || notifiedModules.contains(module)) {
                    continue;
                }
                notifiedModules.add(module);
                System.out.println("【驳回模块通知】[通知-当前驳回节点] 模块: " + module);

                String logMsg = notifySingleModuleReject(module, flowCode, instanceId, instanceName,
                        applicantName, rejectedNodeKey, rejectedNodeName, tenantCode,
                        operator, comment, params, "当前驳回节点", rejectedNodeKey, rejectedNodeName);
                logService.saveLog(instanceId, operator != null ? operator.getId() : null,
                        operator != null ? operator.getUsername() : "系统",
                        "reject_notify", logMsg);
            }
        }

        // 2. 通知之前所有已通过的、且配置了业务执行模块的节点
        for (FlowTask task : allTasks) {
            if (task.getNodeKey().equals(rejectedNodeKey)) {
                continue;
            }
            if (task.getStatus() != 1) {
                continue;
            }
            FlowNodeConfig cfg = flowNodeConfigMapper.selectOne(
                    new LambdaQueryWrapper<FlowNodeConfig>()
                            .eq(FlowNodeConfig::getFlowCode, flowCode)
                            .eq(FlowNodeConfig::getNodeKey, task.getNodeKey())
            );
            if (cfg != null && StringUtils.hasText(cfg.getExecuteModules())) {
                for (String module : cfg.getExecuteModules().split(",")) {
                    module = module.trim();
                    if (!StringUtils.hasText(module) || notifiedModules.contains(module)) {
                        continue;
                    }
                    notifiedModules.add(module);
                    System.out.println("【驳回模块通知】[通知-历史通过节点] 模块: " + module);
                    System.out.println("  节点类型: 之前已审批通过的节点");
                    System.out.println("  节点(key): " + task.getNodeKey());
                    System.out.println("  节点(名称): " + task.getNodeName());
                    System.out.println("  该节点通过时间: " + task.getExecuteTime());

                    String logMsg = notifySingleModuleReject(module, flowCode, instanceId, instanceName,
                            applicantName, rejectedNodeKey, rejectedNodeName, tenantCode,
                            operator, comment, params, "历史通过节点[" + task.getNodeName() + "]",
                            task.getNodeKey(), task.getNodeName());
                    logService.saveLog(instanceId, operator != null ? operator.getId() : null,
                            operator != null ? operator.getUsername() : "系统",
                            "reject_notify", logMsg);
                }
            }
        }

        System.out.println("【驳回模块通知】共通知了 " + notifiedModules.size() + " 个业务模块: " + notifiedModules);
        if (!failedModules.isEmpty()) {
            System.out.println("【驳回模块通知】通知失败模块: " + failedModules);
        }
        System.out.println("【驳回模块通知】========================================");
    }

    /**
     * 向单个业务模块发送驳回通知
     * @param moduleCode        模块编码
     * @param flowCode          流程编码
     * @param instanceId         流程实例ID
     * @param instanceName       实例名称
     * @param applicantName      申请人
     * @param rejectedNodeKey    被驳回节点编码
     * @param rejectedNodeName   被驳回节点名称
     * @param tenantCode        租户编码
     * @param operator          驳回操作人
     * @param comment           驳回意见
     * @param params            流程参数
     * @param sourceDesc        来源描述（当前驳回节点 / 历史通过节点）
     * @param notifyNodeKey     本次通知对应的节点key（HTTP请求体中使用该值）
     * @param notifyNodeName    本次通知对应的节点名称（HTTP请求体中使用该值）
     */
    private String notifySingleModuleReject(String moduleCode, String flowCode, String instanceId,
            String instanceName, String applicantName, String rejectedNodeKey, String rejectedNodeName,
            String tenantCode, SysUser operator, String comment, Map<String, Object> params,
            String sourceDesc, String notifyNodeKey, String notifyNodeName) {
        String moduleEndpoint = getModuleCallbackBaseUrl(moduleCode);
        if (moduleEndpoint == null) {
            System.out.println("【驳回模块通知】[通知失败] 模块[" + moduleCode + "]：未配置回调地址，跳过 HTTP 通知（仅记录日志）");
            return "驳回通知：通知业务执行模块[" + moduleCode + "]，" + sourceDesc + "已被驳回，模块回调地址未配置";
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("flow_code", flowCode);
        requestBody.put("action", "reject");
        requestBody.put("instance_id", instanceId);
        requestBody.put("instance_name", instanceName);
        requestBody.put("applicant_name", applicantName);
        requestBody.put("tenant_code", tenantCode);
        requestBody.put("reject_node_key", notifyNodeKey);
        requestBody.put("reject_node_name", notifyNodeName);
        requestBody.put("reject_operator", operator != null ? operator.getUsername() : "系统");
        requestBody.put("reject_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        requestBody.put("reject_comment", comment != null ? comment : "");
        requestBody.put("params", params);

        String callbackToken = UUID.randomUUID().toString();
        requestBody.put("callback_token", callbackToken);
        requestBody.put("callback_url", "/flow/callback/reject/complete");

        try {
            System.out.println("【驳回模块通知】[HTTP通知] 开始通知模块[" + moduleCode + "]: " + moduleEndpoint);
            System.out.println("【驳回模块通知】[HTTP通知] 请求体: " + requestBody);

            @SuppressWarnings("unchecked")
            Map<String, Object> httpResp = restTemplate.postForObject(
                    moduleEndpoint, requestBody, Map.class);

            System.out.println("【驳回模块通知】[HTTP通知] 模块[" + moduleCode + "] 响应: " + httpResp);

            if (httpResp != null && Boolean.TRUE.equals(httpResp.get("success"))) {
                return "驳回通知：通知业务执行模块[" + moduleCode + "]，" + sourceDesc + "已被驳回[" + rejectedNodeName + "]，模块已处理";
            } else {
                String msg = httpResp != null ? String.valueOf(httpResp.get("message")) : "未知原因";
                return "驳回通知：通知业务执行模块[" + moduleCode + "]，" + sourceDesc + "已被驳回[" + rejectedNodeName + "]，模块处理失败：" + msg;
            }
        } catch (Exception e) {
            System.out.println("【驳回模块通知】[HTTP通知] 模块[" + moduleCode + "] 调用异常: " + e.getMessage());
            return "驳回通知：通知业务执行模块[" + moduleCode + "]，" + sourceDesc + "已被驳回[" + rejectedNodeName + "]，HTTP通知失败：" + e.getMessage();
        }
    }

    /**
     * 根据模块编码查询回调基础地址
     * 通过查询 sys_module 或模块内注册的回调配置获取
     * 目前从 sys_module 表的 module_url 字段读取，未配置时返回 null（仅记录日志）
     */
    private String getModuleCallbackBaseUrl(String moduleCode) {
        SysModule module = sysModuleService.getByCode(moduleCode);
        if (module != null) {
            String url = module.getModuleUrl();
            if (StringUtils.hasText(url)) {
                return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            }
        }
        return null;
    }

    public Map<String, Object> handleModuleCallback(String callbackToken, boolean success, String message, String extraData) {
        Map<String, Object> result = new HashMap<>();

        if (!StringUtils.hasText(callbackToken)) {
            result.put("success", false);
            result.put("message", "回调令牌不能为空");
            return result;
        }

        TaskCallbackContext ctx = getCallbackContext(callbackToken);
        if (ctx == null) {
            result.put("success", false);
            result.put("message", "无效的回调令牌：" + callbackToken);
            return result;
        }

        FlowTask task = flowTaskMapper.selectById(ctx.getTaskId());
        if (task == null) {
            result.put("success", false);
            result.put("message", "任务不存在，taskId: " + ctx.getTaskId());
            return result;
        }

        if (task.getStatus() != 3) {
            result.put("success", false);
            result.put("message", "任务状态已变更，当前状态: " + task.getStatus());
            return result;
        }

        FlowInstance instance = flowInstanceMapper.selectById(ctx.getInstanceId());
        if (instance == null) {
            result.put("success", false);
            result.put("message", "流程实例不存在");
            return result;
        }

        loadDynamicHandlersToThreadLocal(instance);

        if (success) {
            task.setStatus(1);
            flowTaskMapper.updateById(task);

            // 跳过同节点的其余并行任务
            skipParallelTasks(instance, task);

            callbackTokenMap.remove(callbackToken);

            // 清空 currentNodeKey，让后续节点触发时设置正确的值
            instance.setCurrentNodeKey("");
            instance.setCurrentNodeName("");
            flowInstanceMapper.updateById(instance);

            // 审批通过后检查逻辑节点
            checkLogicNodesAfterApproval(instance, task);

            String logMsg = "外部模块[" + ctx.getModuleCode() + "]回调成功";
            if (StringUtils.hasText(message)) {
                logMsg += "，消息: " + message;
            }
            logService.saveLog(instance.getInstanceId(), ctx.getOperatorId(), ctx.getOperatorName(),
                    "callback_success", logMsg);

            System.out.println("【回调成功】任务ID: " + task.getTaskId() + "，节点: " + task.getNodeKey());

            result.put("success", true);
            result.put("message", "回调处理成功");
            result.put("taskId", task.getTaskId());
            result.put("instanceId", instance.getInstanceId());

        } else {
            task.setStatus(4);
            task.setComment((task.getComment() != null ? task.getComment() + "；" : "") +
                    "[外部回调失败] " + (StringUtils.hasText(message) ? message : "未知原因"));
            flowTaskMapper.updateById(task);

            String logMsg = "外部模块[" + ctx.getModuleCode() + "]回调失败";
            if (StringUtils.hasText(message)) {
                logMsg += "，原因: " + message;
            }
            logService.saveLog(instance.getInstanceId(), ctx.getOperatorId(), ctx.getOperatorName(),
                    "callback_failed", logMsg);

            result.put("success", true);
            result.put("message", "回调处理成功，任务已重置为「逻辑处理失败」状态");
            result.put("taskId", task.getTaskId());
        }

        dynamicHandlerThreadLocal.remove();
        return result;
    }

    public void resetTaskStatus(String taskId, Long userId, String reason) {
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        if (task.getStatus() != 4) {
            throw new RuntimeException("只有「逻辑处理失败」状态的任务才能重置");
        }

        FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (userId != null && !userId.equals(task.getHandlerId())) {
            throw new RuntimeException("只有原审批人可以重置任务");
        }

        task.setStatus(0);
        task.setCallbackToken(null);
        task.setAction(null);
        task.setExecuteTime(null);
        task.setComment("【重置】" + (StringUtils.hasText(reason) ? reason : "已重置，可重新审批"));
        flowTaskMapper.updateById(task);

        if (!FlowInstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            instance.setStatus(FlowInstanceStatus.RUNNING.getCode());
            flowInstanceMapper.updateById(instance);
        }

        logService.saveLog(instance.getInstanceId(), userId, sysUserService.getById(userId).getUsername(),
                "task_reset", "任务[" + task.getNodeName() + "]已重置");

        System.out.println("【任务重置】任务ID: " + taskId);
    }

    public String getTaskStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待处理";
            case 1: return "已通过";
            case 2: return "已驳回";
            case 3: return "业务执行中";
            case 4: return "逻辑处理失败";
            default: return "未知(" + status + ")";
        }
    }

    public Map<String, Object> updateExecuteLog(String callbackToken, String logContent) {
        Map<String, Object> result = new HashMap<>();

        if (!StringUtils.hasText(callbackToken)) {
            result.put("success", false);
            result.put("message", "回调令牌不能为空");
            return result;
        }

        TaskCallbackContext ctx = getCallbackContext(callbackToken);
        if (ctx == null) {
            result.put("success", false);
            result.put("message", "无效的回调令牌");
            return result;
        }

        FlowTask task = flowTaskMapper.selectById(ctx.getTaskId());
        if (task == null) {
            result.put("success", false);
            result.put("message", "任务不存在");
            return result;
        }

        try {
            List<Map<String, Object>> logList;
            if (StringUtils.hasText(task.getExecuteLog())) {
                logList = objectMapper.readValue(task.getExecuteLog(), new TypeReference<List<Map<String, Object>>>() {});
            } else {
                logList = new ArrayList<>();
            }

            Map<String, Object> entry = new HashMap<>();
            entry.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            entry.put("content", logContent);
            logList.add(entry);

            task.setExecuteLog(objectMapper.writeValueAsString(logList));
            flowTaskMapper.updateById(task);

            result.put("success", true);
            result.put("message", "执行日志已更新");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新执行日志失败: " + e.getMessage());
        }

        return result;
    }

    private Map<String, DynamicHandlerDTO> restoreDynamicHandlerMap(FlowInstance instance) {
        Map<String, DynamicHandlerDTO> dynamicHandlerMap = new HashMap<>();
        String dynamicHandlersJson = instance.getDynamicHandlers();
        if (StringUtils.hasText(dynamicHandlersJson)) {
            try {
                Map<String, Object> extraData = objectMapper.readValue(dynamicHandlersJson,
                        new TypeReference<Map<String, Object>>() {});
                if (extraData != null) {
                    // 新格式：nodeConfigs
                    if (extraData.containsKey("nodeConfigs")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> configs = objectMapper.convertValue(extraData.get("nodeConfigs"),
                                new TypeReference<List<Map<String, Object>>>() {});
                        if (configs != null) {
                            for (Map<String, Object> cfg : configs) {
                                DynamicHandlerDTO dto = new DynamicHandlerDTO();
                                dto.setNodeKey((String) cfg.get("nodeKey"));
                                Object handlerId = cfg.get("handlerId");
                                if (handlerId != null) {
                                    dto.setHandlerId(handlerId instanceof Number ? ((Number) handlerId).longValue() : Long.parseLong(handlerId.toString()));
                                }
                                dto.setHandlerName((String) cfg.get("handlerName"));
                                // 读取租户（tenantCode，旧数据中的数值字段无法推断租户编码）
                                String tenantCode = null;
                                if (cfg.containsKey("tenantCode") && cfg.get("tenantCode") != null) {
                                    tenantCode = cfg.get("tenantCode").toString();
                                } else if (cfg.containsKey("tenantCodes") && cfg.get("tenantCodes") != null) {
                                    @SuppressWarnings("unchecked")
                                    List<?> oldList = (List<?>) cfg.get("tenantCodes");
                                    if (!oldList.isEmpty() && oldList.get(0) != null) {
                                        tenantCode = oldList.get(0).toString();
                                    }
                                }
                                dto.setTenantCode(tenantCode);
                                // 恢复发起机构ID（机构相关审批）
                                Object sourceOrgId = cfg.get("sourceOrgId");
                                if (sourceOrgId != null) {
                                    dto.setSourceOrgId(sourceOrgId.toString());
                                }
                                dynamicHandlerMap.put(dto.getNodeKey(), dto);
                            }
                        }
                    }
                    // 旧格式：dynamicHandlers
                    else if (extraData.containsKey("dynamicHandlers")) {
                        @SuppressWarnings("unchecked")
                        List<DynamicHandlerDTO> handlers = objectMapper.convertValue(extraData.get("dynamicHandlers"),
                                new TypeReference<List<DynamicHandlerDTO>>() {});
                        if (handlers != null) {
                            for (DynamicHandlerDTO handler : handlers) {
                                dynamicHandlerMap.put(handler.getNodeKey(), handler);
                            }
                        }
                    }
                    // 最旧格式：直接是数组
                    else {
                        List<DynamicHandlerDTO> handlers = objectMapper.readValue(dynamicHandlersJson,
                                new TypeReference<List<DynamicHandlerDTO>>() {});
                        if (handlers != null) {
                            for (DynamicHandlerDTO handler : handlers) {
                                dynamicHandlerMap.put(handler.getNodeKey(), handler);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("解析动态处理人信息失败", e);
            }
        }
        return dynamicHandlerMap;
    }

    /**
     * 从流程实例的 dynamicHandlers 字段恢复 nodeTenants 并设置到 ThreadLocal
     */
    private void restoreAndSetNodeTenantSelections(FlowInstance instance) {
        String dynamicHandlersJson = instance.getDynamicHandlers();
        if (StringUtils.hasText(dynamicHandlersJson)) {
            try {
                Map<String, Object> extraData = objectMapper.readValue(dynamicHandlersJson,
                        new TypeReference<Map<String, Object>>() {});
                if (extraData != null && extraData.containsKey("nodeTenants")) {
                    @SuppressWarnings("unchecked")
                    List<String> nodeTenants = objectMapper.convertValue(extraData.get("nodeTenants"),
                            new TypeReference<List<String>>() {});
                    nodeTenantsThreadLocal.set(nodeTenants != null ? nodeTenants : new ArrayList<>());
                    System.out.println("===== [多租户审批] 审批时恢复租户列表 =====");
                    System.out.println("  租户Codes: " + nodeTenantsThreadLocal.get());
                    return;
                }
            } catch (Exception e) {
                log.error("恢复节点租户列表失败", e);
            }
        }
        nodeTenantsThreadLocal.set(new ArrayList<>());
    }

    private void loadDynamicHandlersToThreadLocal(FlowInstance instance) {
        Map<String, DynamicHandlerDTO> dynamicHandlerMap = restoreDynamicHandlerMap(instance);
        dynamicHandlerThreadLocal.set(dynamicHandlerMap);
        restoreAndSetNodeTenantSelections(instance);
        restoreAndSetNodeSourceOrgId(instance);
    }

    /**
     * 从流程实例的 dynamicHandlers 字段恢复 nodeSourceOrgId 并设置到 ThreadLocal
     */
    private void restoreAndSetNodeSourceOrgId(FlowInstance instance) {
        String dynamicHandlersJson = instance.getDynamicHandlers();
        if (StringUtils.hasText(dynamicHandlersJson)) {
            try {
                Map<String, Object> extraData = objectMapper.readValue(dynamicHandlersJson,
                        new TypeReference<Map<String, Object>>() {});
                if (extraData != null && extraData.containsKey("nodeConfigs")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> configs = objectMapper.convertValue(extraData.get("nodeConfigs"),
                            new TypeReference<List<Map<String, Object>>>() {});
                    if (configs != null) {
                        for (Map<String, Object> cfg : configs) {
                            Object sourceOrgId = cfg.get("sourceOrgId");
                            if (sourceOrgId != null) {
                                String orgId = sourceOrgId.toString();
                                nodeSourceOrgIdThreadLocal.set(orgId);
                                System.out.println("===== [机构相关审批] 审批时恢复发起机构ID: " + orgId + " =====");
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("恢复发起机构ID失败", e);
            }
        }
        nodeSourceOrgIdThreadLocal.set(null);
    }

    @Override
    public void cancelFlow(String instanceId, Long userId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (!FlowInstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            throw new RuntimeException("只有运行中的流程才能撤销");
        }

        if (!instance.getApplicantId().equals(userId)) {
            throw new RuntimeException("只有申请人才能撤销流程");
        }

        // 检查是否已有节点审批过：有任意一个审批节点（type=approve）状态为 1（已通过）则不允许撤销
        Long approvedCount = flowTaskMapper.selectCount(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instanceId)
                        .eq(FlowTask::getNodeType, "approve")
                        .eq(FlowTask::getStatus, 1)
                        .eq(FlowTask::getDeleted, 0)
        );
        if (approvedCount != null && approvedCount > 0) {
            throw new RuntimeException("该流程已有节点审批通过，不允许撤销");
        }

        instance.setStatus(FlowInstanceStatus.CANCELED.getCode());
        flowInstanceMapper.updateById(instance);

        logService.saveLog(instanceId, userId, sysUserService.getById(userId).getUsername(),
                FlowOperationType.CANCEL.getCode(), "用户撤销流程");
    }

    @Override
    public void terminateFlow(String instanceId, Long userId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (!FlowInstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            throw new RuntimeException("只有运行中的流程才能终止");
        }

        instance.setStatus(FlowInstanceStatus.TERMINATED.getCode());
        flowInstanceMapper.updateById(instance);

        logService.saveLog(instanceId, userId, sysUserService.getById(userId).getUsername(),
                FlowOperationType.TERMINATE.getCode(), "管理员终止流程");
    }

    @Override
    public String rollbackFlow(String instanceId, Long userId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (!FlowInstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            throw new RuntimeException("只有运行中的流程才能回退");
        }

        // 检查是否有节点处于"业务执行中"状态，不允许回退
        List<FlowTask> executingTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instanceId)
                        .eq(FlowTask::getDeleted, 0)
                        .eq(FlowTask::getStatus, 3)
        );
        if (!executingTasks.isEmpty()) {
            String executingNodeNames = executingTasks.stream()
                    .map(FlowTask::getNodeName)
                    .collect(Collectors.joining("、"));
            throw new RuntimeException("等待当前节点[" + executingNodeNames + "]逻辑执行完成后再回退");
        }

        FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowCode());
        if (flow == null) {
            throw new RuntimeException("流程定义不存在");
        }

        List<FlowLine> flowLines = new ArrayList<>();
        Map<String, FlowNodeConfig> nodeKeyMap = new HashMap<>();
        Map<String, FlowNodeConfig> nodeIdMap = new HashMap<>();
        List<FlowNodeConfig> allNodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, instance.getFlowCode())
                        .orderByAsc(FlowNodeConfig::getSort)
        );
        for (FlowNodeConfig node : allNodes) {
            nodeKeyMap.put(node.getNodeKey(), node);
            if (StringUtils.hasText(node.getNodeId())) {
                nodeIdMap.put(node.getNodeId(), node);
            }
        }
        if (StringUtils.hasText(flow.getFlowJson())) {
            try {
                FlowJsonData flowJsonData = objectMapper.readValue(flow.getFlowJson(), FlowJsonData.class);
                if (flowJsonData != null && flowJsonData.getLines() != null) {
                    flowLines = flowJsonData.getLines();
                }
            } catch (Exception e) {
                log.warn("解析 flowJson 失败", e);
            }
        }

        List<FlowTask> allTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instanceId)
                        .eq(FlowTask::getDeleted, 0)
                        .orderByAsc(FlowTask::getCreateTime)
        );

        // 找出当前"最靠后"（执行顺序最后）的 APPROVE 审批任务
        // 【修复】按 createTime 倒序找最后一个 APPROVE 节点任务（而非按 status=0），
        // 因为回退起点可能是 status=3(业务执行中)、status=1(已完成) 等任意状态
        FlowTask currentPendingTask = null;
        for (int i = allTasks.size() - 1; i >= 0; i--) {
            FlowTask t = allTasks.get(i);
            if (FlowNodeType.APPROVE.getCode().equals(t.getNodeType())) {
                currentPendingTask = t;
                break;
            }
        }

        // 查找 currentPendingTask 所在节点的前置审批节点
        FlowNodeConfig currentNode = nodeKeyMap.get(currentPendingTask.getNodeKey());
        if (currentNode == null && StringUtils.hasText(currentPendingTask.getNodeKey())) {
            currentNode = nodeIdMap.get(currentPendingTask.getNodeKey());
        }

        List<FlowNodeConfig> predecessorNodes = new ArrayList<>();
        if (currentNode != null) {
            predecessorNodes = findPredecessorApprovalNodes(currentNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        }

        if (predecessorNodes.isEmpty()) {
            // 没有前置审批节点，说明当前就是第一个审批节点，已是初始状态
            return "已回退至流程初始状态";
        }

        SysUser operator = sysUserService.getById(userId);
        String operatorName = operator != null ? operator.getUsername() : "管理员";

        // 收集所有需要撤销的后续审批节点 nodeKey
        // APPROVE 节点：由 Step 1 设置为 status=5
        // NOTIFY/TEXT 节点：仅用于继续正向遍历（不加入 subsequentNodeKeys）
        // 【重要】从 currentNode 开始正向遍历，而非从 predecessorNodes 开始
        // 这样避免把 currentNode 本身错误地加入 subsequentNodeKeys
        Set<String> subsequentNodeKeys = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        // 从 currentNode 开始遍历，而不是从 predecessorNodes 开始
        String startKey = currentNode != null ? currentNode.getNodeKey() : null;
        if (startKey != null) {
            queue.add(startKey);
        }
        while (!queue.isEmpty()) {
            String nodeKey = queue.poll();
            if (visited.contains(nodeKey)) continue;
            visited.add(nodeKey);

            FlowNodeConfig current = nodeKeyMap.get(nodeKey);
            if (current == null) current = nodeIdMap.get(nodeKey);
            if (current == null) continue;

            List<FlowNodeConfig> nextNodes = findNextNodesByLines(current, allNodes, flowLines, nodeKeyMap, nodeIdMap);
            for (FlowNodeConfig next : nextNodes) {
                if (FlowNodeType.APPROVE.getCode().equals(next.getNodeType())) {
                    // APPROVE 节点：加入待撤销列表，后续由 Step 1 设置为 status=5
                    subsequentNodeKeys.add(next.getNodeKey());
                }
                // NOTIFY/TEXT 节点：仅用于继续正向遍历，不加入 subsequentNodeKeys
                if (!visited.contains(next.getNodeKey())) {
                    queue.add(next.getNodeKey());
                }
            }
        }

        // Step 1: 将所有后续节点的任务标记为"已跳过"（被回退撤销），清除所有字段
        for (FlowTask task : allTasks) {
            if (subsequentNodeKeys.contains(task.getNodeKey())) {
                // 【修复】必须用 LambdaUpdateWrapper，否则 handlerId=null 不会清空数据库字段
                LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<FlowTask>()
                        .eq(FlowTask::getTaskId, task.getTaskId())
                        .eq(FlowTask::getDeleted, 0)
                        .set(FlowTask::getStatus, 5)
                        .set(FlowTask::getHandlerId, null)
                        .set(FlowTask::getHandlerName, null)
                        .set(FlowTask::getAction, "rollback")
                        .set(FlowTask::getComment, "因回退操作，该节点任务被撤销")
                        .set(FlowTask::getExecuteTime, null);
                flowTaskMapper.update(null, updateWrapper);
                logService.saveLog(instanceId, task.getHandlerId(), task.getHandlerName(),
                        FlowOperationType.ROLLBACK.getCode(),
                        "审批节点[" + task.getNodeName() + "]因回退操作被撤销");
                System.out.println("rollbackFlow - 撤销后续任务: taskId=" + task.getTaskId() + ", node=" + task.getNodeName());
            }
        }

        // Step 1b: 将当前审批节点本身也重置为"待处理"，清除处理人、操作、执行时间
        if (currentPendingTask != null && FlowNodeType.APPROVE.getCode().equals(currentPendingTask.getNodeType())) {
            LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<FlowTask>()
                    .eq(FlowTask::getTaskId, currentPendingTask.getTaskId())
                    .eq(FlowTask::getDeleted, 0)
                    .set(FlowTask::getStatus, 0)
                    .set(FlowTask::getHandlerId, null)
                    .set(FlowTask::getHandlerName, null)
                    .set(FlowTask::getAction, null)
                    .set(FlowTask::getComment, null)
                    .set(FlowTask::getExecuteTime, null);
            flowTaskMapper.update(null, updateWrapper);
            // 同步更新内存对象，确保后续 Step 2 中不会重复处理
            currentPendingTask.setStatus(0);
            currentPendingTask.setHandlerId(null);
            currentPendingTask.setHandlerName(null);
            currentPendingTask.setAction(null);
            currentPendingTask.setComment(null);
            currentPendingTask.setExecuteTime(null);
            logService.saveLog(instanceId, userId, operatorName,
                    FlowOperationType.ROLLBACK.getCode(),
                    "审批节点[" + currentPendingTask.getNodeName() + "]被回退至待处理状态（由" + operatorName + "操作）");
            System.out.println("rollbackFlow - 重置当前节点为待处理: taskId=" + currentPendingTask.getTaskId() + ", node=" + currentPendingTask.getNodeName());
        }

        // Step 2: 将所有前置节点的任务重置为"待处理"
        for (FlowNodeConfig predNode : predecessorNodes) {
            List<FlowTask> predTasks = flowTaskMapper.selectList(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, instanceId)
                            .eq(FlowTask::getNodeKey, predNode.getNodeKey())
                            .eq(FlowTask::getDeleted, 0)
            );
            if (!predTasks.isEmpty()) {
                for (FlowTask task : predTasks) {
                    LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<FlowTask>()
                            .eq(FlowTask::getTaskId, task.getTaskId())
                            .eq(FlowTask::getDeleted, 0)
                            .set(FlowTask::getStatus, 0)
                            .set(FlowTask::getAction, null)
                            .set(FlowTask::getComment, null)
                            .set(FlowTask::getExecuteTime, null);
                    flowTaskMapper.update(null, updateWrapper);
                    logService.saveLog(instanceId, task.getHandlerId(), task.getHandlerName(),
                            FlowOperationType.ROLLBACK.getCode(),
                            "审批节点[" + task.getNodeName() + "]被回退至待处理状态（由" + operatorName + "操作）");
                    System.out.println("rollbackFlow - 重置任务为待处理: taskId=" + task.getTaskId() + ", node=" + task.getNodeName());
                }
            } else {
                // 无任务记录时创建新的待处理任务
                createRollbackTask(instance, predNode, operator);
            }
        }

        // 更新实例当前节点
        FlowNodeConfig firstPredecessor = predecessorNodes.get(0);
        instance.setCurrentNodeKey(firstPredecessor.getNodeKey());
        instance.setCurrentNodeName(firstPredecessor.getNodeName());
        flowInstanceMapper.updateById(instance);

        logService.saveLog(instanceId, userId, operatorName,
                FlowOperationType.ROLLBACK.getCode(),
                "流程从节点[" + currentPendingTask.getNodeName() + "]回退到[" + firstPredecessor.getNodeName() + "]");

        return "回退成功，当前节点已回退到[" + firstPredecessor.getNodeName() + "]";
    }

    /**
     * 根据角色编码获取第一个成员用户
     */
    private SysUser getFirstUserByRoleCode(String roleCode, String tenantCode) {
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserRole::getRoleCode, roleCode);
        if (tenantCode != null && !tenantCode.trim().isEmpty()) {
            queryWrapper.eq(SysUserRole::getTenantCode, tenantCode);
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(queryWrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return null;
        }
        return sysUserService.getById(userRoles.get(0).getUserId());
    }

    /**
     * 回退时为没有任务记录的前置节点创建新任务
     */
    private void createRollbackTask(FlowInstance instance, FlowNodeConfig node, SysUser operator) {
        FlowTask task = new FlowTask();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setInstanceId(instance.getInstanceId());
        task.setNodeKey(node.getNodeKey());
        task.setNodeName(node.getNodeName());
        task.setNodeType(node.getNodeType());
        task.setStatus(0); // 待处理
        task.setTenantCode(instance.getTenantCode());

        // 设置处理人
        if (StringUtils.hasText(node.getHandlerType()) && StringUtils.hasText(node.getHandlerIds())) {
            if ("user".equals(node.getHandlerType())) {
                // handlerIds 格式为逗号分隔的用户ID，取第一个
                String firstId = node.getHandlerIds().split(",")[0].trim();
                try {
                    Long handlerId = Long.parseLong(firstId);
                    task.setHandlerId(handlerId);
                    SysUser handler = sysUserService.getById(handlerId);
                    if (handler != null) task.setHandlerName(handler.getUsername());
                } catch (NumberFormatException ignored) {}
            } else if ("role".equals(node.getHandlerType())) {
                // handlerIds 为逗号分隔的角色编码，取第一个
                String firstRoleCode = node.getHandlerIds().split(",")[0].trim();
                SysUser roleUser = getFirstUserByRoleCode(firstRoleCode, instance.getTenantCode());
                if (roleUser != null) {
                    task.setHandlerId(roleUser.getId());
                    task.setHandlerName(roleUser.getUsername());
                }
            } else if ("dynamic".equals(node.getHandlerType()) || "role_dynamic_user".equals(node.getHandlerType())) {
                // 动态处理人：从 ThreadLocal 中获取（发起时已存过）
                Map<String, DynamicHandlerDTO> dynamicMap = dynamicHandlerThreadLocal.get();
                if (dynamicMap != null && dynamicMap.containsKey(node.getNodeKey())) {
                    DynamicHandlerDTO dto = dynamicMap.get(node.getNodeKey());
                    if (dto != null) {
                        task.setHandlerId(dto.getHandlerId());
                        task.setHandlerName(dto.getHandlerName());
                        task.setTenantCode(dto.getTenantCode());
                        task.setSourceOrgId(dto.getSourceOrgId());
                    }
                }
            }
        }

        flowTaskMapper.insert(task);
        Long opId = operator != null ? operator.getId() : null;
        String opName = operator != null ? operator.getUsername() : "管理员";
        logService.saveLog(instance.getInstanceId(), opId, opName,
                FlowOperationType.ROLLBACK.getCode(), "回退后为节点[" + node.getNodeName() + "]创建待处理任务");
    }

    @Override
    public IPage<FlowInstanceVO> myInitiated(Long userId, String moduleCode, String tenantCode, String flowCode, String typeCode, String currentNodeKey, Integer pageNum, Integer pageSize) {
        Page<FlowInstance> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getApplicantId, userId)
                .eq(FlowInstance::getDeleted, 0)
                .orderByDesc(FlowInstance::getCreateTime);

        if (flowCode != null) {
            wrapper.eq(FlowInstance::getFlowCode, flowCode);
        }

        if (tenantCode != null && !tenantCode.isEmpty()) {
            wrapper.eq(FlowInstance::getTenantCode, tenantCode);
        }
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(FlowInstance::getModuleCode, moduleCode);
        }
        if (typeCode != null && !typeCode.isEmpty()) {
            wrapper.eq(FlowInstance::getAssetTypeId, typeCode);
        }

        // 按当前审批节点过滤
        if (StringUtils.hasText(currentNodeKey)) {
            wrapper.eq(FlowInstance::getCurrentNodeKey, currentNodeKey);
        }

        IPage<FlowInstance> instancePage = flowInstanceMapper.selectPage(page, wrapper);
        List<FlowInstance> instanceList = instancePage.getRecords();

        List<FlowInstanceVO> voList = new ArrayList<>();
        for (FlowInstance inst : instanceList) {
            FlowInstanceVO vo = new FlowInstanceVO();
            BeanUtils.copyProperties(inst, vo);

            FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowCode());
            if (flow != null) {
                vo.setFlowName(flow.getFlowName());
            }
            if (inst.getAssetTypeId() != null) {
                AssetType assetType = getAssetTypeByCode(inst.getAssetTypeId());
                if (assetType != null) {
                    vo.setTypeCode(assetType.getTypeCode());
                    vo.setAssetTypeName(assetType.getTypeName());
                }
            }

            FlowInstanceStatus status = FlowInstanceStatus.fromCode(inst.getStatus());
            vo.setStatusName(status != null ? status.getName() : "");

            FlowTask latestTask = flowTaskMapper.selectOne(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, inst.getInstanceId())
                            .eq(FlowTask::getDeleted, 0)
                            .orderByDesc(FlowTask::getCreateTime)
                            .last("LIMIT 1")
            );
            if (latestTask != null && latestTask.getStatus() == 3) {
                vo.setExecuteLog(latestTask.getExecuteLog());
            }

            if (inst.getStatus() == FlowInstanceStatus.RUNNING.getCode() && inst.getCurrentNodeKey() != null) {
                FlowNodeConfig currentNode = flowNodeConfigMapper.selectOne(
                        new LambdaQueryWrapper<FlowNodeConfig>()
                                .eq(FlowNodeConfig::getFlowCode, inst.getFlowCode())
                                .eq(FlowNodeConfig::getNodeKey, inst.getCurrentNodeKey())
                );
                if (currentNode != null && FlowNodeType.APPROVE.getCode().equals(currentNode.getNodeType())) {
                    vo.setEnableNotify(currentNode.getEnableNotify());
                    vo.setNotifyType(currentNode.getNotifyType());
                    // 填充当前节点数据库主键（用于前端加载节点详情）
                    vo.setCurrentNodeId(currentNode.getNodeId());
                }
            }

            // 是否可撤回：运行中 且 未有任何审批节点（type=approve）审批通过（status=1）
            if (FlowInstanceStatus.RUNNING.getCode().equals(inst.getStatus())) {
                Long approvedCount = flowTaskMapper.selectCount(
                        new LambdaQueryWrapper<FlowTask>()
                                .eq(FlowTask::getInstanceId, inst.getInstanceId())
                                .eq(FlowTask::getNodeType, "approve")
                                .eq(FlowTask::getStatus, 1)
                                .eq(FlowTask::getDeleted, 0)
                );
                vo.setCanCancel(approvedCount == null || approvedCount == 0);
            } else {
                vo.setCanCancel(false);
            }

            voList.add(vo);
        }

        Page<FlowInstanceVO> resultPage = new Page<>(instancePage.getCurrent(), instancePage.getSize(), voList.size());
        resultPage.setRecords(voList);
        resultPage.setTotal(instancePage.getTotal());

        return resultPage;
    }

    @Override
    public IPage<FlowTaskVO> myApproval(Long userId, Integer taskStatus, String moduleCode, String tenantCode, String flowCode, String typeCode, String nodeKey, Integer pageNum, Integer pageSize) {
        Page<FlowTask> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getHandlerId, userId)
                .eq(FlowTask::getStatus, taskStatus)
                .eq(FlowTask::getDeleted, 0);

        if (tenantCode != null && !tenantCode.isEmpty()) {
            wrapper.and(w -> w.eq(FlowTask::getTenantCode, tenantCode).or().isNull(FlowTask::getTenantCode));
        }
        // 按审批节点过滤
        if (StringUtils.hasText(nodeKey)) {
            wrapper.eq(FlowTask::getNodeKey, nodeKey);
        }
        wrapper.orderByDesc(FlowTask::getCreateTime);

        IPage<FlowTask> taskPage = flowTaskMapper.selectPage(page, wrapper);
        List<FlowTask> taskList = taskPage.getRecords();

        // 按模块/流程/资产类型过滤（用于前端筛选展示）
        if ((moduleCode != null && !moduleCode.isEmpty()) || (tenantCode != null && !tenantCode.isEmpty()) || flowCode != null || (typeCode != null && !typeCode.isEmpty())) {
            List<FlowTask> filteredTasks = new ArrayList<>();
            for (FlowTask t : taskList) {
                FlowInstance inst = flowInstanceMapper.selectById(t.getInstanceId());
                if (inst == null) {
                    continue;
                }
                // 多租户审批节点过滤：有效租户 = 任务自己的租户 > 实例的租户
                String effectiveTenantCode = t.getTenantCode();
                if (effectiveTenantCode == null) {
                    effectiveTenantCode = inst.getTenantCode();
                }
                if (tenantCode != null && !tenantCode.isEmpty() && (effectiveTenantCode == null || !tenantCode.equals(effectiveTenantCode))) {
                    continue;
                }
                if (moduleCode != null && !moduleCode.isEmpty() && !moduleCode.equals(inst.getModuleCode())) {
                    continue;
                }
                if (flowCode != null && !flowCode.equals(inst.getFlowCode())) {
                    continue;
                }
                if (typeCode != null && !typeCode.isEmpty() && !typeCode.equals(inst.getAssetTypeId())) {
                    continue;
                }
                filteredTasks.add(t);
            }
            taskList = filteredTasks;
        }

        // 机构层级过滤：仅当 sourceOrgId 不为空时生效。
        // 逻辑说明：从 sourceOrgId 出发，依次向上遍历（parentId），直到根（parentId=0）。
        // 若用户的授权机构在此路径上，则有权审批此任务。
        // moduleCode 从审批节点配置（flow_node_config.module_code）中读取，而非 flow_definition.module_code。
        List<FlowTask> finalTaskList = new ArrayList<>();
        for (FlowTask t : taskList) {
            String srcOrg = t.getSourceOrgId();
            if (srcOrg == null) {
                // 无机构要求，任何人都能看（已在任务分配时过滤过，这里安全放行）
                finalTaskList.add(t);
                continue;
            }
            FlowInstance inst = flowInstanceMapper.selectById(t.getInstanceId());
            if (inst == null) {
                continue;
            }
            // 从审批节点的 module_code 读取，而非 flow_definition.module_code
            FlowNodeConfig nodeConfig = flowNodeConfigMapper.selectOne(
                    new LambdaQueryWrapper<FlowNodeConfig>()
                            .eq(FlowNodeConfig::getFlowCode, inst.getFlowCode())
                            .eq(FlowNodeConfig::getNodeKey, t.getNodeKey()));
            String nodeModuleCode = (nodeConfig != null) ? nodeConfig.getModuleCode() : null;
            String effectiveTenantCode = t.getTenantCode() != null ? t.getTenantCode() : inst.getTenantCode();
            // 调用独立方法判断机构层级权限（便于后续扩展）
            boolean authorized = sysUserService.isUserAuthorizedForOrgLevel(
                    userId, nodeModuleCode, effectiveTenantCode, srcOrg);
            if (authorized) {
                finalTaskList.add(t);
            }
        }
        taskList = finalTaskList;

        List<FlowTaskVO> voList = new ArrayList<>();
        for (FlowTask t : taskList) {
            FlowTaskVO vo = new FlowTaskVO();
            BeanUtils.copyProperties(t, vo);

            FlowInstance inst = flowInstanceMapper.selectById(t.getInstanceId());
            if (inst != null) {
                vo.setFlowCode(inst.getFlowCode());
                FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowCode());
                vo.setFlowName(flow != null ? flow.getFlowName() : "");
                vo.setInstanceName(inst.getInstanceName());
                vo.setModuleCode(inst.getModuleCode());
                vo.setAssetTypeId(inst.getAssetTypeId());

                SysUser applicant = sysUserService.getById(inst.getApplicantId());
                vo.setApplicantName(applicant != null ? applicant.getUsername() : "");

                // 填充租户名称（优先用任务自己的租户编码，其次用实例的租户编码）
                String effectiveTenantCode = t.getTenantCode();
                if (effectiveTenantCode == null || effectiveTenantCode.trim().isEmpty()) {
                    effectiveTenantCode = inst.getTenantCode();
                }
                if (effectiveTenantCode != null && !effectiveTenantCode.trim().isEmpty()) {
                    SysTenant tenant = sysTenantService.getByTenantCode(effectiveTenantCode);
                    vo.setTenantName(tenant != null ? tenant.getTenantName() : "");
                }
                // 填充发起机构名称
                if (t.getSourceOrgId() != null) {
                    BankOrg srcOrg = bankOrgMapper.selectById(t.getSourceOrgId());
                    vo.setSourceOrgName(srcOrg != null ? srcOrg.getName() : "");
                }
                // 填充资产类型信息
                if (inst.getAssetTypeId() != null) {
                    AssetType assetType = getAssetTypeByCode(inst.getAssetTypeId());
                    if (assetType != null) {
                        vo.setTypeCode(assetType.getTypeCode());
                        vo.setAssetTypeName(assetType.getTypeName());
                    }
                }
            }

            vo.setCurrentNodeName(t.getNodeName());
            vo.setCreateTime(t.getCreateTime());

            voList.add(vo);
        }

        Page<FlowTaskVO> resultPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), voList.size());
        resultPage.setRecords(voList);
        resultPage.setTotal(taskPage.getTotal());

        return resultPage;
    }

    @Override
    public IPage<FlowTaskVO> adminAllTasks(Integer taskStatus, String moduleCode, String tenantCode,
                                           String flowCode, String typeCode, String nodeKey,
                                           Integer pageNum, Integer pageSize) {
        Page<FlowTask> page = new Page<>(pageNum, pageSize);

        // 不限制 handlerId，获取所有任务
        // 【修复】taskStatus=0 时排除 handlerId=null 的任务（回退后被撤销的节点，无处理人）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>();
        if (taskStatus != null) {
            if (taskStatus == 0) {
                wrapper.eq(FlowTask::getStatus, 0).isNotNull(FlowTask::getHandlerId);
            } else {
                wrapper.eq(FlowTask::getStatus, taskStatus);
            }
        }
        wrapper.eq(FlowTask::getDeleted, 0);

        if (StringUtils.hasText(tenantCode)) {
            wrapper.and(w -> w.eq(FlowTask::getTenantCode, tenantCode).or().isNull(FlowTask::getTenantCode));
        }
        if (StringUtils.hasText(nodeKey)) {
            wrapper.eq(FlowTask::getNodeKey, nodeKey);
        }
        wrapper.orderByDesc(FlowTask::getCreateTime);

        IPage<FlowTask> taskPage = flowTaskMapper.selectPage(page, wrapper);
        List<FlowTask> taskList = taskPage.getRecords();

        if (StringUtils.hasText(flowCode) || StringUtils.hasText(moduleCode) || StringUtils.hasText(typeCode)) {
            List<FlowTask> filteredTasks = new ArrayList<>();
            for (FlowTask t : taskList) {
                FlowInstance inst = flowInstanceMapper.selectById(t.getInstanceId());
                if (inst == null) continue;
                FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowCode());
                if (flow == null) continue;
                if (StringUtils.hasText(flowCode) && !flowCode.equals(inst.getFlowCode())) continue;
                if (StringUtils.hasText(moduleCode) && !moduleCode.equals(inst.getModuleCode())) continue;
                if (StringUtils.hasText(typeCode) && !typeCode.equals(inst.getAssetTypeId())) continue;
                if (StringUtils.hasText(tenantCode)) {
                    String effectiveTenantCode = t.getTenantCode() != null ? t.getTenantCode() : inst.getTenantCode();
                    if (effectiveTenantCode == null || !tenantCode.equals(effectiveTenantCode)) continue;
                }
                filteredTasks.add(t);
            }
            taskList = filteredTasks;
        }

        List<FlowTaskVO> voList = new ArrayList<>();
        for (FlowTask t : taskList) {
            FlowTaskVO vo = new FlowTaskVO();
            BeanUtils.copyProperties(t, vo);

            FlowInstance inst = flowInstanceMapper.selectById(t.getInstanceId());
            if (inst != null) {
                vo.setFlowCode(inst.getFlowCode());
                FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowCode());
                vo.setFlowName(flow != null ? flow.getFlowName() : "");
                vo.setInstanceName(inst.getInstanceName());
                vo.setModuleCode(inst.getModuleCode());
                vo.setAssetTypeId(inst.getAssetTypeId());

                SysUser applicant = sysUserService.getById(inst.getApplicantId());
                vo.setApplicantName(applicant != null ? applicant.getUsername() : "");

                String effectiveTenantCode = t.getTenantCode();
                if (effectiveTenantCode == null || effectiveTenantCode.trim().isEmpty()) {
                    effectiveTenantCode = inst.getTenantCode();
                }
                if (effectiveTenantCode != null && !effectiveTenantCode.trim().isEmpty()) {
                    SysTenant tenant = sysTenantService.getByTenantCode(effectiveTenantCode);
                    vo.setTenantName(tenant != null ? tenant.getTenantName() : "");
                }
                if (t.getSourceOrgId() != null) {
                    BankOrg srcOrg = bankOrgMapper.selectById(t.getSourceOrgId());
                    vo.setSourceOrgName(srcOrg != null ? srcOrg.getName() : "");
                }
                if (inst.getAssetTypeId() != null) {
                    AssetType assetType = getAssetTypeByCode(inst.getAssetTypeId());
                    if (assetType != null) {
                        vo.setTypeCode(assetType.getTypeCode());
                        vo.setAssetTypeName(assetType.getTypeName());
                    }
                }
            }
            vo.setCurrentNodeName(t.getNodeName());
            voList.add(vo);
        }

        Page<FlowTaskVO> resultPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), voList.size());
        resultPage.setRecords(voList);
        resultPage.setTotal(taskPage.getTotal());

        return resultPage;
    }

    @Override
    public FlowDetailVO getFlowDetail(String instanceId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return null;
        }

        FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowCode());
        SysUser applicant = sysUserService.getById(instance.getApplicantId());

        FlowDetailVO detailVO = new FlowDetailVO();
        detailVO.setInstanceId(instanceId);
        detailVO.setFlowName(flow != null ? flow.getFlowName() : "");
        detailVO.setInstanceName(instance.getInstanceName());
        detailVO.setApplicantName(applicant != null ? applicant.getUsername() : "");

        FlowInstanceStatus status = FlowInstanceStatus.fromCode(instance.getStatus());
        detailVO.setStatusName(status != null ? status.getName() : "");
        detailVO.setCreateTime(instance.getCreateTime());

        detailVO.setAttachmentUrl(instance.getAttachmentUrl());
        detailVO.setAttachmentName(instance.getAttachmentName());
        detailVO.setExtraInfo(instance.getExtraInfo());

        // 获取流程参数
        List<FlowInstanceParamVO> flowParamVOList = new ArrayList<>();
        FlowInstanceParam[] instanceParams = flowInstanceParamMapper.findByInstanceId(instanceId);
        if (instanceParams != null && instanceParams.length > 0 && flow != null) {
            List<FlowTemplateParam> templateParams = flowTemplateParamMapper.findByTemplateId(flow.getFlowCode());
            final Map<String, String> paramNameMap = new HashMap<>();
            if (templateParams != null && !templateParams.isEmpty()) {
                for (FlowTemplateParam tp : templateParams) {
                    paramNameMap.put(tp.getParamCode(), tp.getParamName());
                }
            }
            for (FlowInstanceParam ip : instanceParams) {
                FlowInstanceParamVO paramVO = new FlowInstanceParamVO();
                paramVO.setParamName(paramNameMap.getOrDefault(ip.getParamCode(), ip.getParamCode()));
                paramVO.setParamValue(ip.getParamValue());
                paramVO.setParamValueLabel(ip.getParamValueLabel());
                flowParamVOList.add(paramVO);
            }
        }
        detailVO.setFlowParams(flowParamVOList);

        // 获取节点配置和任务
        List<FlowNodeConfig> nodeConfigs = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, instance.getFlowCode())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        List<FlowTask> taskList = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instanceId)
                        .eq(FlowTask::getDeleted, 0)
        );

        // 加载动态处理人
        Map<String, DynamicHandlerDTO> dynamicHandlerMap = new HashMap<>();
        if (StringUtils.hasText(instance.getDynamicHandlers())) {
            try {
                List<DynamicHandlerDTO> handlers = objectMapper.readValue(instance.getDynamicHandlers(),
                        new TypeReference<List<DynamicHandlerDTO>>() {});
                if (handlers != null) {
                    for (DynamicHandlerDTO handler : handlers) {
                        dynamicHandlerMap.put(handler.getNodeKey(), handler);
                    }
                }
            } catch (Exception e) {
                log.error("解析动态处理人信息失败", e);
            }
        }

        // 解析连线信息
        List<FlowLine> flowLines = new ArrayList<>();
        Map<String, String> flowJsonNodeAliasMap = new HashMap<>();
        if (flow != null && StringUtils.hasText(flow.getFlowJson())) {
            try {
                FlowJsonData flowJsonData = objectMapper.readValue(flow.getFlowJson(), FlowJsonData.class);
                if (flowJsonData != null && flowJsonData.getLines() != null) {
                    flowLines = flowJsonData.getLines();
                    flowJsonNodeAliasMap = buildFlowJsonNodeAliasMap(flowJsonData);
                }
            } catch (Exception e) {
                log.warn("解析 flowJson 失败", e);
            }
        }

        // 构建节点映射
        Map<String, FlowNodeConfig> nodeKeyMap = new HashMap<>();
        Map<String, FlowNodeConfig> nodeIdMap = new HashMap<>();
        for (FlowNodeConfig node : nodeConfigs) {
            nodeKeyMap.put(node.getNodeKey(), node);
            if (StringUtils.hasText(node.getNodeId())) {
                nodeIdMap.put(node.getNodeId(), node);
            }
        }
        mergeFlowJsonAliasesIntoNodeIdMap(nodeKeyMap, nodeIdMap, flowJsonNodeAliasMap);
        detailVO.setLineList(buildFlowLineDetailList(flowLines, nodeKeyMap, nodeIdMap));

        // 【优化】根据拓扑排序重组节点列表，显示并行关系
        List<FlowNodeDetailVO> nodeDetailList = buildNodeDetailListWithParallelism(
                nodeConfigs, taskList, flowLines, nodeKeyMap, nodeIdMap, dynamicHandlerMap, instance.getStatus());

        detailVO.setNodeList(nodeDetailList);

        List<FlowOperationLog> logList = logService.listByInstanceId(instanceId);
        detailVO.setLogList(logList);

        return detailVO;
    }

    private List<FlowLineDetailVO> buildFlowLineDetailList(List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {
        List<FlowLineDetailVO> lineDetails = new ArrayList<>();
        if (flowLines == null || flowLines.isEmpty()) {
            return lineDetails;
        }
        for (FlowLine line : flowLines) {
            String fromRaw = line.getFromNode();
            String toRaw = line.getToNode();
            if (!StringUtils.hasText(fromRaw) || !StringUtils.hasText(toRaw)) {
                continue;
            }
            FlowNodeConfig fromNode = nodeIdMap.get(fromRaw);
            if (fromNode == null) {
                fromNode = nodeKeyMap.get(fromRaw);
            }
            FlowNodeConfig toNode = nodeIdMap.get(toRaw);
            if (toNode == null) {
                toNode = nodeKeyMap.get(toRaw);
            }
            if (fromNode == null || toNode == null) {
                continue;
            }

            FlowLineDetailVO vo = new FlowLineDetailVO();
            vo.setFromNodeKey(fromNode.getNodeKey());
            vo.setFromNodeName(fromNode.getNodeName());
            vo.setToNodeKey(toNode.getNodeKey());
            vo.setToNodeName(toNode.getNodeName());
            lineDetails.add(vo);
        }
        return lineDetails;
    }

    /**
     * 【优化】根据拓扑排序和并行关系构建节点详情列表
     * 1. 过滤掉逻辑与/逻辑或节点（不展示）
     * 2. 按拓扑分层，同一层级的审批节点并排显示
     * 3. end 节点：没有任务时显示"未执行"，有 status=1 任务才显示"已完成"
     * 4. 使用 nodeKey 作为唯一标识（优先 nodeKey，空时用 nodeId），保证 start/end 等空 nodeKey 节点不冲突
     * 5. 【修复】使用 Kahn 正向 BFS + 最长路径计算 level；过滤逻辑节点后重映射层级，使层级连续
     *    示例（用户流程）：start=0, logic_1=1, logic_2=1, logic_3=2（含被过滤的 and 的层级）,
     *    and 被过滤后不占层级，end=3
     */
    private List<FlowNodeDetailVO> buildNodeDetailListWithParallelism(
            List<FlowNodeConfig> nodeConfigs,
            List<FlowTask> taskList,
            List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap,
            Map<String, FlowNodeConfig> nodeIdMap,
            Map<String, DynamicHandlerDTO> dynamicHandlerMap,
            Integer instanceStatus) {

        List<FlowNodeDetailVO> nodeDetailList = new ArrayList<>();

        // 【过滤前】保留所有节点（用于正确计算连通性和入度）
        List<FlowNodeConfig> allNodes = nodeConfigs;

        // 【过滤】排除逻辑与、逻辑或节点（不展示在流程详情中）
        List<FlowNodeConfig> displayNodes = nodeConfigs.stream()
                .filter(n -> !FlowNodeType.LOGIC_AND.getCode().equals(n.getNodeType())
                        && !FlowNodeType.LOGIC_OR.getCode().equals(n.getNodeType()))
                .collect(Collectors.toList());

        // 获取节点唯一标识：优先 nodeKey，空时用 nodeId，保证 start/end 等节点不冲突
        java.util.function.Function<FlowNodeConfig, String> getNodeId = node -> {
            if (node.getNodeKey() != null && !node.getNodeKey().isEmpty()) {
                return node.getNodeKey();
            }
            return node.getNodeId();
        };

        // 构建 id -> nodeConfig 反查表
        Map<String, FlowNodeConfig> idToNode = new LinkedHashMap<>();
        Map<String, FlowNodeConfig> allIdToNode = new LinkedHashMap<>();
        for (FlowNodeConfig node : displayNodes) {
            idToNode.put(getNodeId.apply(node), node);
        }
        for (FlowNodeConfig node : allNodes) {
            allIdToNode.put(getNodeId.apply(node), node);
        }

        // 构建正向邻接表（from -> to）和入度
        // 用 allNodes 构建，以保证拓扑关系的完整性
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adjacency = new LinkedHashMap<>();
        for (FlowNodeConfig node : allNodes) {
            String id = getNodeId.apply(node);
            inDegree.put(id, 0);
            adjacency.put(id, new ArrayList<>());
        }

        if (flowLines != null && !flowLines.isEmpty()) {
            for (FlowLine line : flowLines) {
                String fromId = line.getFromNode();
                String toId = line.getToNode();

                FlowNodeConfig fromNode = nodeIdMap.get(fromId);
                FlowNodeConfig toNode = nodeIdMap.get(toId);
                if (fromNode == null) fromNode = nodeKeyMap.get(fromId);
                if (toNode == null) toNode = nodeKeyMap.get(toId);
                if (fromNode == null || toNode == null) continue;

                String fromUniqueId = getNodeId.apply(fromNode);
                String toUniqueId = getNodeId.apply(toNode);

                if (!adjacency.containsKey(fromUniqueId) || !inDegree.containsKey(toUniqueId)) continue;

                adjacency.get(fromUniqueId).add(toUniqueId);
                inDegree.put(toUniqueId, inDegree.get(toUniqueId) + 1);
            }
        }

        // Kahn 正向 BFS：从入度为 0 的节点（起点）开始，计算最长路径作为 level
        // level[node] = max(所有前驱节点的 level) + 1
        Map<String, Integer> level = new LinkedHashMap<>();
        for (String id : allIdToNode.keySet()) {
            level.put(id, 0);
        }

        // 复制入度副本用于队列管理
        Map<String, Integer> mutableInDegree = new LinkedHashMap<>(inDegree);

        Queue<String> queue = new LinkedList<>();
        for (String id : allIdToNode.keySet()) {
            if (mutableInDegree.get(id) == 0) {
                level.put(id, 0);
                queue.add(id);
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLevel = level.get(current);

            for (String next : adjacency.get(current)) {
                // next 的层级 = max(next 当前层级, currentLevel + 1)
                level.put(next, Math.max(level.get(next), currentLevel + 1));

                int newInDegree = mutableInDegree.get(next) - 1;
                mutableInDegree.put(next, newInDegree);
                if (newInDegree == 0) {
                    queue.add(next);
                }
            }
        }

        // 【核心修复】过滤逻辑节点后，重新映射层级为连续整数
        // 例如：原始 levels = [0,1,1,2,3,4]，过滤后 logic_and 在 level=2 → 过滤后变成 [0,1,1,3,4]
        // 需要压缩为 [0,1,1,2,3]
        Map<Integer, Integer> originalToDisplayLevel = new LinkedHashMap<>();
        int displayLevelCounter = 0;
        for (int origLevel = 0; origLevel <= Collections.max(level.values()); origLevel++) {
            // 检查该原始层级是否有逻辑节点
            final int lvl = origLevel;
            boolean hasLogicNode = allNodes.stream()
                    .anyMatch(n -> FlowNodeType.isLogicNode(n.getNodeType())
                            && level.get(getNodeId.apply(n)) == lvl);
            // 该原始层级是否在 displayNodes 中有节点
            boolean hasDisplayNode = displayNodes.stream()
                    .anyMatch(n -> level.get(getNodeId.apply(n)) == lvl);

            if (hasDisplayNode) {
                if (!hasLogicNode) {
                    // 该层级只有显示节点（没有逻辑节点），直接映射
                    originalToDisplayLevel.put(origLevel, displayLevelCounter);
                    displayLevelCounter++;
                } else {
                    // 该层级同时有逻辑节点和显示节点 → 显示节点使用当前计数器值（跳过逻辑节点的层级）
                    originalToDisplayLevel.put(origLevel, displayLevelCounter);
                    // 逻辑节点的原始层级被"吸收"，不额外占用层级
                    displayLevelCounter++;
                }
            }
            // 如果该层级只有逻辑节点（没有显示节点），不增加计数器
        }

        // 构建任务映射（nodeKey -> 任务列表）
        Map<String, List<FlowTask>> taskMap = new HashMap<>();
        for (FlowTask task : taskList) {
            taskMap.computeIfAbsent(task.getNodeKey(), k -> new ArrayList<>()).add(task);
        }

        // 按层级分组（level 升序），同一层级内按 displayNodes 原始顺序
        Map<String, Integer> idToIndex = new LinkedHashMap<>();
        for (int i = 0; i < displayNodes.size(); i++) {
            idToIndex.put(getNodeId.apply(displayNodes.get(i)), i);
        }

        Map<Integer, List<String>> levelMap = new TreeMap<>();
        for (FlowNodeConfig node : displayNodes) {
            String id = getNodeId.apply(node);
            int origLevel = level.get(id);
            int dispLevel = originalToDisplayLevel.getOrDefault(origLevel, origLevel);
            levelMap.computeIfAbsent(dispLevel, k -> new ArrayList<>()).add(id);
        }

        // 同一层级内按 displayNodes 原始顺序排序
        for (List<String> ids : levelMap.values()) {
            ids.sort(Comparator.comparingInt(id -> idToIndex.getOrDefault(id, Integer.MAX_VALUE)));
        }

        // 构建 VO 列表
        for (Integer lvl : levelMap.keySet()) {
            for (String nodeId : levelMap.get(lvl)) {
                FlowNodeConfig nodeConfig = idToNode.get(nodeId);
                if (nodeConfig == null) continue;

                FlowNodeDetailVO nodeVO = new FlowNodeDetailVO();
                nodeVO.setNodeKey(nodeConfig.getNodeKey());
                nodeVO.setNodeName(nodeConfig.getNodeName());

                FlowNodeType nodeType = FlowNodeType.fromCode(nodeConfig.getNodeType());
                nodeVO.setNodeType(nodeType != null ? nodeType.getName() : "");
                nodeVO.setCustomFields(nodeConfig.getCustomFields());

                // 设置层级信息（用于前端显示并行关系）
                nodeVO.setLevel(lvl);

                List<FlowTask> nodeTasks = taskMap.get(nodeConfig.getNodeKey());
                if (nodeTasks != null && !nodeTasks.isEmpty()) {
                    // 取该节点的最新状态任务
                    FlowTask latestTask = nodeTasks.get(0);
                    for (FlowTask t : nodeTasks) {
                        if (t.getExecuteTime() != null && latestTask.getExecuteTime() != null
                                && t.getExecuteTime().after(latestTask.getExecuteTime())) {
                            latestTask = t;
                        }
                    }
                    FlowTask firstTask = nodeTasks.get(0);

                    if ("role".equals(nodeConfig.getHandlerType())) {
                        nodeVO.setHandlerNames(getHandlerNames(nodeConfig, nodeConfig.getHandlerIds(), dynamicHandlerMap));
                    } else {
                        String handlerNames = nodeTasks.stream().map(FlowTask::getHandlerName).collect(Collectors.joining(","));
                        nodeVO.setHandlerNames(handlerNames);
                    }

                    if (firstTask.getAction() == null) {
                        nodeVO.setAction("自动");
                    } else if ("approve".equals(firstTask.getAction())) {
                        nodeVO.setAction("通过");
                    } else if ("reject".equals(firstTask.getAction())) {
                        nodeVO.setAction("驳回");
                    } else if ("notify".equals(firstTask.getAction())) {
                        nodeVO.setAction("通知");
                    } else if ("skip".equals(firstTask.getAction())) {
                        nodeVO.setAction("跳过");
                    } else {
                        nodeVO.setAction(firstTask.getAction());
                    }

                    nodeVO.setComment(firstTask.getComment());
                    nodeVO.setExecuteTime(firstTask.getExecuteTime());

                    // 【修复】end 节点状态：
                    // 1. 如果没有任务（正常情况），根据流程实例状态判断：已完成显示"已完成"，否则显示"待触发"
                    // 2. 如果有任务但流程未完成，说明任务被提前创建了，状态应该显示"待触发"
                    if (FlowNodeType.END.getCode().equals(nodeConfig.getNodeType())) {
                        boolean instanceCompleted = instanceStatus != null
                                && FlowInstanceStatus.COMPLETED.getCode().equals(instanceStatus);
                        // 流程未完成时，end 节点状态应该是"待触发"
                        nodeVO.setStatus(instanceCompleted ? "已完成" : "待触发");
                    } else {
                        // 其他节点：取第一个任务的 status
                        // 【修复】status=0 时：如果没有 handlerId，说明是回退后被撤销的任务，显示"待触发"
                        // 只有 status=0 且有 handlerId 的才是正常待处理的审批任务
                        String statusText;
                        if (firstTask.getStatus() == 0 && firstTask.getHandlerId() == null) {
                            statusText = "待触发";
                        } else {
                            switch (firstTask.getStatus()) {
                                case 0: statusText = "待处理"; break;
                                case 1: statusText = "已完成"; break;
                                case 2: statusText = "已驳回"; break;
                                case 3: statusText = "业务执行中"; break;
                                case 4: statusText = "逻辑处理失败"; break;
                                case 5: statusText = "已跳过"; break;
                                default: statusText = "未知"; break;
                            }
                        }
                        nodeVO.setStatus(statusText);
                    }
                    nodeVO.setExecuteLog(firstTask.getExecuteLog());
                    nodeVO.setCustomFieldValues(firstTask.getCustomFieldValues());
                } else {
                    // 【修复】end 节点无任务时，根据流程实例状态判断
                    nodeVO.setHandlerNames(getHandlerNames(nodeConfig, nodeConfig.getHandlerIds(), dynamicHandlerMap));
                    boolean instanceCompleted = instanceStatus != null
                            && FlowInstanceStatus.COMPLETED.getCode().equals(instanceStatus);
                    nodeVO.setStatus(instanceCompleted ? "已完成" : "待触发");
                }

                nodeDetailList.add(nodeVO);
            }
        }

        return nodeDetailList;
    }

    private void createAutoTask(String instanceId, FlowNodeConfig nodeConfig, String action, String comment) {
        // 幂等检查：如果该实例该节点已有自动任务，不再重复创建
        Long existingCount = flowTaskMapper.selectCount(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instanceId)
                        .eq(FlowTask::getNodeKey, nodeConfig.getNodeKey())
                        .eq(FlowTask::getDeleted, 0)
        );
        if (existingCount != null && existingCount > 0) {
            return;
        }

        FlowTask task = new FlowTask();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setInstanceId(instanceId);
        task.setNodeKey(nodeConfig.getNodeKey());
        task.setNodeName(nodeConfig.getNodeName());
        task.setNodeType(nodeConfig.getNodeType());
        task.setHandlerId(null);
        task.setHandlerName("系统自动");
        task.setAction(action);
        task.setComment(comment);
        task.setExecuteTime(new Date());
        task.setStatus(1);
        flowTaskMapper.insert(task);
    }

    private void fillTextNodeCustomFields(String instanceId, List<FlowNodeConfig> nodeConfigs) {
        try {
            FlowNodeConfig textNode = nodeConfigs.stream()
                    .filter(node -> FlowNodeType.TEXT.getCode().equals(node.getNodeType()))
                    .findFirst()
                    .orElse(null);

            if (textNode == null || !StringUtils.hasText(textNode.getCustomFields())) {
                return;
            }

            List<Map<String, String>> customFieldsConfig = objectMapper.readValue(
                    textNode.getCustomFields(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );

            boolean hasGreyUrlField = customFieldsConfig.stream()
                    .anyMatch(field -> "REPORT_GREY_URL".equals(field.get("fieldName")));

            if (!hasGreyUrlField) {
                return;
            }

            String randomUrl = "https://gray.example.com/report/" + UUID.randomUUID().toString().substring(0, 8);

            List<FlowTask> tasks = flowTaskMapper.selectList(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, instanceId)
                            .eq(FlowTask::getNodeKey, textNode.getNodeKey())
            );

            if (tasks.isEmpty()) {
                return;
            }

            Map<String, String> customFieldValues = new HashMap<>();
            customFieldValues.put("REPORT_GREY_URL", randomUrl);

            for (FlowTask task : tasks) {
                task.setCustomFieldValues(objectMapper.writeValueAsString(customFieldValues));
                flowTaskMapper.updateById(task);
            }

            System.out.println("【模拟填充】REPORT_RELEASE 流程自定义字段：REPORT_GREY_URL = " + randomUrl);

        } catch (Exception e) {
            System.err.println("填充文本节点自定义字段失败: " + e.getMessage());
        }
    }

    /**
     * 判断角色是否为机构相关（org_related=1）
     */
    private boolean isOrgRelatedRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) return false;
        SysRole role = sysRoleService.getById(roleCode.trim());
        return role != null && Boolean.TRUE.equals(role.getOrgRelated());
    }

    private String getRealHandlerIds(FlowNodeConfig nodeConfig, String tenantCode, Map<String, DynamicHandlerDTO> dynamicHandlerMap) {
        return getRealHandlerIdsWithOrgFilter(nodeConfig, tenantCode, null, dynamicHandlerMap);
    }

    /**
     * 获取符合条件的处理人ID，支持机构层级过滤
     * @param sourceOrgId 发起机构ID（机构相关审批时有效）
     */
    private String getRealHandlerIdsWithOrgFilter(FlowNodeConfig nodeConfig, String tenantCode, String sourceOrgId,
            Map<String, DynamicHandlerDTO> dynamicHandlerMap) {
        if (!StringUtils.hasText(nodeConfig.getHandlerType())) {
            return "";
        }

        if ("dynamic".equals(nodeConfig.getHandlerType())
                || "role_dynamic_user".equals(nodeConfig.getHandlerType())) {
            // dynamic/role_dynamic_user：从运行时配置中取选中的用户ID，不预分配
            if (nodeConfig.getNodeKey() != null && dynamicHandlerMap != null) {
                DynamicHandlerDTO handler = dynamicHandlerMap.get(nodeConfig.getNodeKey());
                if (handler != null && handler.getHandlerId() != null) {
                    return handler.getHandlerId().toString();
                }
            }
            return "";
        }

        if (!StringUtils.hasText(nodeConfig.getHandlerIds())) {
            return "";
        }

        Set<String> userIds = new HashSet<>();

        if ("role".equals(nodeConfig.getHandlerType())) {
            for (String roleId : nodeConfig.getHandlerIds().split(",")) {
                if (!StringUtils.hasText(roleId)) continue;

                LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(SysUserRole::getRoleCode, roleId.trim());

                // 多租户模块需要按租户筛选角色成员
                if (isMultiTenantModule(nodeConfig.getModuleCode()) && tenantCode != null && !tenantCode.trim().isEmpty()) {
                    queryWrapper.eq(SysUserRole::getTenantCode, tenantCode);
                }

                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(queryWrapper);
                for (SysUserRole ur : userRoles) {
                    // 机构层级过滤：调用独立方法判断（便于后续扩展）
                    if (sourceOrgId != null) {
                        boolean authorized = sysUserService.isUserAuthorizedForOrgLevel(
                                ur.getUserId(), nodeConfig.getModuleCode(), tenantCode, sourceOrgId);
                        if (!authorized) {
                            System.out.println("getRealHandlerIds - 用户 " + ur.getUserId() + " 授权机构不在发起机构之上，跳过");
                            continue;
                        }
                    }
                    userIds.add(ur.getUserId().toString());
                }
            }
        } else if ("user".equals(nodeConfig.getHandlerType())) {
            userIds.addAll(Arrays.asList(nodeConfig.getHandlerIds().split(",")));
        }

        return String.join(",", userIds);
    }

    /**
     * 获取用户在指定模块/租户下的授权机构ID
     */
    private String getUserOrgId(Long userId, String moduleCode, String tenantCode) {
        String effectiveTenantCode = isMultiTenantModule(moduleCode) ? tenantCode : "";
        LambdaQueryWrapper<com.rightmanage.entity.SysUserOrgAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.rightmanage.entity.SysUserOrgAuth::getUserId, userId)
                .eq(com.rightmanage.entity.SysUserOrgAuth::getModuleCode, moduleCode);
        if (effectiveTenantCode == null) {
            wrapper.isNull(com.rightmanage.entity.SysUserOrgAuth::getTenantCode);
        } else {
            wrapper.eq(com.rightmanage.entity.SysUserOrgAuth::getTenantCode, effectiveTenantCode);
        }
        wrapper.last("limit 1");
        com.rightmanage.entity.BankOrg org = sysUserService.getAuthorizedOrg(userId, moduleCode, tenantCode);
        return org != null ? org.getId() : null;
    }

    private String getHandlerNames(FlowNodeConfig nodeConfig, String handlerIds, Map<String, DynamicHandlerDTO> dynamicHandlerMap) {
        if (!StringUtils.hasText(handlerIds)) {
            return "无";
        }

        StringBuilder names = new StringBuilder();

        if ("role".equals(nodeConfig.getHandlerType())) {
            for (String roleCode : handlerIds.split(",")) {
                if (!StringUtils.hasText(roleCode)) continue;
                SysRole role = sysRoleService.getById(roleCode.trim());
                if (role != null) {
                    names.append("角色：").append(role.getRoleName()).append(",");
                }
            }
        } else if ("user".equals(nodeConfig.getHandlerType())) {
            for (String userId : handlerIds.split(",")) {
                if (!StringUtils.hasText(userId)) continue;
                SysUser user = sysUserService.getById(Long.parseLong(userId.trim()));
                if (user != null) {
                    names.append(user.getUsername()).append(",");
                }
            }
        } else if ("dynamic".equals(nodeConfig.getHandlerType())
                || "role_dynamic_user".equals(nodeConfig.getHandlerType())) {
            if (nodeConfig.getNodeKey() != null && dynamicHandlerMap != null) {
                DynamicHandlerDTO handler = dynamicHandlerMap.get(nodeConfig.getNodeKey());
                if (handler != null && handler.getHandlerName() != null) {
                    names.append(handler.getHandlerName());
                }
            }
        }

        if (names.length() > 0) {
            return names.toString().replaceAll(",$", "");
        }
        return "无";
    }

    private void saveFlowInstanceParams(String instanceId, String flowCode, Map<String, Object> params) {
        try {
            List<FlowTemplateParam> templateParams = flowTemplateParamMapper.findByTemplateId(flowCode);
            if (templateParams == null || templateParams.isEmpty()) {
                return;
            }

            Map<String, String> paramIdMap = new HashMap<>();
            Map<String, FlowTemplateParam> paramConfigMap = new HashMap<>();
            for (FlowTemplateParam tp : templateParams) {
                paramIdMap.put(tp.getParamCode(), tp.getDefinitionParamId());
                paramConfigMap.put(tp.getParamCode(), tp);
            }

            List<FlowInstanceParam> instanceParams = new ArrayList<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String paramCode = entry.getKey();
                Object paramValue = entry.getValue();

                if (paramIdMap.containsKey(paramCode) && paramValue != null) {
                    FlowInstanceParam ip = new FlowInstanceParam();
                    ip.setInstanceParamId(UUID.randomUUID().toString().replace("-", ""));
                    ip.setInstanceId(instanceId);
                    ip.setDefinitionParamId(paramIdMap.get(paramCode));
                    ip.setParamCode(paramCode);
                    ip.setParamValue(paramValue.toString());

                    FlowTemplateParam templateParam = paramConfigMap.get(paramCode);
                    if (templateParam != null && "select".equals(templateParam.getParamType())
                            && templateParam.getOptionJson() != null && !templateParam.getOptionJson().isEmpty()) {
                        try {
                            List<Map<String, String>> options = objectMapper.readValue(
                                    templateParam.getOptionJson(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                            for (Map<String, String> option : options) {
                                if (paramValue.toString().equals(option.get("value"))) {
                                    ip.setParamValueLabel(option.get("label"));
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("解析 optionJson 失败: " + e.getMessage());
                        }
                    }

                    instanceParams.add(ip);
                }
            }

            if (!instanceParams.isEmpty()) {
                flowInstanceParamMapper.batchSave(instanceParams.toArray(new FlowInstanceParam[0]));
            }
        } catch (Exception e) {
            System.err.println("保存流程参数失败: " + e.getMessage());
        }
    }

    @Override
    public String triggerNodeNotify(String instanceId, Long userId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return "流程实例不存在";
        }

        if (instance.getStatus() != FlowInstanceStatus.RUNNING.getCode()) {
            return "流程状态不是运行中，无法触发通知";
        }

        FlowNodeConfig currentNode = flowNodeConfigMapper.selectOne(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, instance.getFlowCode())
                        .eq(FlowNodeConfig::getNodeKey, instance.getCurrentNodeKey())
        );

        if (currentNode == null) {
            return "当前节点配置不存在";
        }

        if (!FlowNodeType.APPROVE.getCode().equals(currentNode.getNodeType())) {
            return "当前节点不是审批节点，无法触发通知";
        }

        if (!"1".equals(currentNode.getEnableNotify())) {
            return "当前节点未开启通知功能";
        }

        // 恢复动态处理人和多租户审批节点租户列表
        Map<String, DynamicHandlerDTO> dynamicHandlerMap = new HashMap<>();
        List<String> nodeTenants = new ArrayList<>();
        if (StringUtils.hasText(instance.getDynamicHandlers())) {
            try {
                Map<String, Object> extraData = objectMapper.readValue(
                        instance.getDynamicHandlers(),
                        new TypeReference<Map<String, Object>>() {});
                if (extraData != null) {
                    if (extraData.containsKey("dynamicHandlers")) {
                        @SuppressWarnings("unchecked")
                        List<DynamicHandlerDTO> handlers = objectMapper.convertValue(extraData.get("dynamicHandlers"),
                                new TypeReference<List<DynamicHandlerDTO>>() {});
                        if (handlers != null) {
                            for (DynamicHandlerDTO handler : handlers) {
                                dynamicHandlerMap.put(handler.getNodeKey(), handler);
                            }
                        }
                    }
                    if (extraData.containsKey("nodeTenants")) {
                        @SuppressWarnings("unchecked")
                        List<String> nts = objectMapper.convertValue(extraData.get("nodeTenants"),
                                new TypeReference<List<String>>() {});
                        if (nts != null) {
                            nodeTenants = nts;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析动态处理人/租户信息失败: {}", e.getMessage());
            }
        }

        // 判断当前节点是否为多租户审批节点
        boolean isMultiTenantNode = "role".equals(currentNode.getHandlerType())
                && sysModuleService.isMultiTenant(currentNode.getModuleCode());

        // 收集通知人（多租户节点：所有选中租户的用户；普通节点：实例租户的用户）
        Set<String> allHandlerIds = new HashSet<>();
        if (isMultiTenantNode && !nodeTenants.isEmpty()) {
            for (String tenantCode : nodeTenants) {
                String handlerIds = getRealHandlerIds(currentNode, tenantCode, dynamicHandlerMap);
                if (StringUtils.hasText(handlerIds)) {
                    for (String hid : handlerIds.split(",")) {
                        if (StringUtils.hasText(hid)) {
                            allHandlerIds.add(hid.trim());
                        }
                    }
                }
            }
        } else {
            String handlerIds = getRealHandlerIds(currentNode, instance.getTenantCode(), dynamicHandlerMap);
            if (StringUtils.hasText(handlerIds)) {
                for (String hid : handlerIds.split(",")) {
                    if (StringUtils.hasText(hid)) {
                        allHandlerIds.add(hid.trim());
                    }
                }
            }
        }

        String handlerIds = String.join(",", allHandlerIds);
        String handlerNames = getHandlerNames(currentNode, handlerIds, dynamicHandlerMap);

        String handlerTypeText = "role".equals(currentNode.getHandlerType()) ? "按角色" : ("user".equals(currentNode.getHandlerType()) ? "按用户" : "动态用户");
        String notifyContent = currentNode.getNotifyContent() != null ? currentNode.getNotifyContent() : "";
        String notifyTypeText = currentNode.getNotifyType() != null ? currentNode.getNotifyType() : "未配置";

        // 租户信息（多租户节点显示所选租户，普通节点显示实例租户）
        String tenantInfo;
        if (isMultiTenantNode && !nodeTenants.isEmpty()) {
            String names = nodeTenants.stream().map(tid -> {
                SysTenant t = sysTenantService.getByTenantCode(tid);
                return t != null ? t.getTenantName() : ("tenantCode=" + tid);
            }).collect(Collectors.joining("、"));
            tenantInfo = "多租户审批，所选租户: " + names;
        } else if (instance.getTenantCode() != null) {
            SysTenant tenant = sysTenantService.getByTenantCode(instance.getTenantCode());
            tenantInfo = tenant != null
                    ? "租户编码: " + tenant.getTenantCode() + "，租户名称: " + tenant.getTenantName()
                    : "租户编码: " + instance.getTenantCode() + "（未找到）";
        } else {
            tenantInfo = "无";
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("========== 主动触发审批节点通知 ==========");
        System.out.println("触发时间: " + timestamp);
        System.out.println("流程实例ID: " + instanceId);
        System.out.println("流程实例名称: " + instance.getInstanceName());
        System.out.println("当前节点: " + currentNode.getNodeName());
        System.out.println("通知方式: " + notifyTypeText);
        System.out.println("审批人类型: " + handlerTypeText);
        System.out.println("审批人: " + handlerNames);
        System.out.println("通知内容模板: " + (notifyContent.isEmpty() ? "（未填写）" : notifyContent));
        System.out.println("流程所属租户: " + tenantInfo);
        System.out.println("触发人: userId=" + userId);
        System.out.println("==========================================");

        logService.saveLog(instanceId, null, null,
                FlowOperationType.NOTIFY.getCode(),
                "【主动通知】触发人：" + userId + "，节点：" + currentNode.getNodeName() +
                        "，通知方式：" + notifyTypeText + "，审批人：" + handlerNames);

        return String.format(
                "通知已发送成功！\n\n节点名称：%s\n通知方式：%s\n审批人类型：%s\n审批人：%s\n通知内容：%s\n触发时间：%s",
                currentNode.getNodeName(), notifyTypeText, handlerTypeText, handlerNames,
                notifyContent.isEmpty() ? "（未填写）" : notifyContent, timestamp
        );
    }

    /**
     * 获取角色+动态用户（role_dynamic_user）节点的候选用户列表
     * 根据 moduleCode + roleIds + tenantCode 查出用户，再通过机构层级过滤
     */
    @Override
    public List<Map<String, Object>> getRoleDynamicUsers(String moduleCode, String roleIds,
            String tenantCode, String sourceOrgId) {
        if (!StringUtils.hasText(roleIds)) {
            return new java.util.ArrayList<>();
        }

        // 1. 根据角色编码列表查出用户
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        List<String> roleCodeList = new ArrayList<>();
        for (String r : roleIds.split(",")) {
            if (StringUtils.hasText(r)) {
                roleCodeList.add(r.trim());
            }
        }
        if (roleCodeList.isEmpty()) {
            return new ArrayList<>();
        }
        queryWrapper.in(SysUserRole::getRoleCode, roleCodeList);
        if (tenantCode != null && !tenantCode.trim().isEmpty()) {
            queryWrapper.eq(SysUserRole::getTenantCode, tenantCode);
        }

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(queryWrapper);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 去重 + 收集用户ID
        Set<Long> userIdSet = userRoles.stream()
                .map(SysUserRole::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        // 3. 机构层级过滤（sourceOrgId != null 时）
        if (sourceOrgId != null && StringUtils.hasText(moduleCode)) {
            for (Long uid : new ArrayList<>(userIdSet)) {
                boolean authorized = sysUserService.isUserAuthorizedForOrgLevel(
                        uid, moduleCode, tenantCode, sourceOrgId);
                if (!authorized) {
                    userIdSet.remove(uid);
                }
            }
        }

        if (userIdSet.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 查询用户信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long uid : userIdSet) {
            SysUser u = sysUserService.getById(uid);
            if (u != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", u.getId());
                item.put("username", u.getUsername());
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 统一查询接口
     * 根据 queryType 分发到对应的查询方法
     */
    @Override
    public FlowQueryResultVO<?> queryFlow(FlowQueryDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            throw new RuntimeException("userId 不能为空");
        }

        String resolvedFlowCode = resolveFlowCode(dto.getFlowCode());

        String queryType = dto.getQueryType();
        FlowQueryResultVO<Object> result = new FlowQueryResultVO<>();
        result.setQueryType(queryType);

        if ("pending".equals(queryType)) {
            IPage<FlowTaskVO> page = myApproval(
                    dto.getUserId(), 0,
                    dto.getModuleCode(), dto.getTenantCode(),
                    resolvedFlowCode, dto.getTypeCode(),
                    dto.getNodeKey(),
                    dto.getPageNum(), dto.getPageSize());
            page = applyPendingOrgScopeFilter(page, dto.getOrgCode(), dto.getOrgScope());
            buildResult(result, page);
            return result;
        } else if ("myApproval".equals(queryType)) {
            IPage<FlowTaskVO> page = myApproval(
                    dto.getUserId(),
                    dto.getTaskStatus() != null ? dto.getTaskStatus() : 1,
                    dto.getModuleCode(), dto.getTenantCode(),
                    resolvedFlowCode, dto.getTypeCode(),
                    dto.getNodeKey(),
                    dto.getPageNum(), dto.getPageSize());
            page = applyPendingOrgScopeFilter(page, dto.getOrgCode(), dto.getOrgScope());
            buildResult(result, page);
            return result;
        } else if ("myInitiated".equals(queryType)) {
            IPage<FlowInstanceVO> page = myInitiated(
                    dto.getUserId(),
                    dto.getModuleCode(), dto.getTenantCode(),
                    resolvedFlowCode, dto.getTypeCode(),
                    dto.getNodeKey(),
                    dto.getPageNum(), dto.getPageSize());
            buildResult(result, page);
            return result;
        } else {
            throw new RuntimeException("不支持的 queryType：" + queryType + "，可选值：pending、myApproval、myInitiated");
        }
    }

    private IPage<FlowTaskVO> applyPendingOrgScopeFilter(IPage<FlowTaskVO> page, String orgCode, String orgScope) {
        if (!StringUtils.hasText(orgCode) || !StringUtils.hasText(orgScope)) {
            return page;
        }

        Set<String> allowedOrgCodes = resolvePendingOrgScopeCodes(orgCode, orgScope);
        List<FlowTaskVO> originalRecords = page.getRecords() == null ? Collections.emptyList() : page.getRecords();
        List<FlowTaskVO> filteredRecords = new ArrayList<>();

        for (FlowTaskVO task : originalRecords) {
            if (!StringUtils.hasText(task.getSourceOrgId())) {
                filteredRecords.add(task);
                continue;
            }
            if (allowedOrgCodes.contains(task.getSourceOrgId())) {
                filteredRecords.add(task);
            }
        }

        Page<FlowTaskVO> filteredPage = new Page<>(page.getCurrent(), page.getSize(), filteredRecords.size());
        filteredPage.setRecords(filteredRecords);
        return filteredPage;
    }

    private Set<String> resolvePendingOrgScopeCodes(String orgCode, String orgScope) {
        Set<String> orgCodes = new LinkedHashSet<>();
        if (!StringUtils.hasText(orgCode) || !StringUtils.hasText(orgScope)) {
            return orgCodes;
        }

        String normalizedScope = orgScope.trim().toLowerCase(Locale.ROOT);
        if ("self".equals(normalizedScope)) {
            orgCodes.add(orgCode.trim());
            return orgCodes;
        }

        String tableName = "bmip_000_suborg_cmp_" + yesterdayMonth();
        if (!tableName.matches("^bmip_000_suborg_cmp_\\d{6}$")) {
            return orgCodes;
        }

        String dte = yesterday();
        List<String> queriedCodes;
        if ("next".equals(normalizedScope)) {
            queriedCodes = bankOrgMapper.selectNextLevelOrgCodes(tableName, dte, orgCode.trim());
        } else if ("all".equals(normalizedScope)) {
            queriedCodes = bankOrgMapper.selectAllSubOrgCodes(tableName, dte, orgCode.trim());
        } else {
            return orgCodes;
        }

        if (queriedCodes != null) {
            for (String code : queriedCodes) {
                if (StringUtils.hasText(code)) {
                    orgCodes.add(code.trim());
                }
            }
        }
        return orgCodes;
    }

    private String yesterday() {
        return LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String yesterdayMonth() {
        return LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private String resolveFlowCode(String flowCode) {
        if (!StringUtils.hasText(flowCode)) {
            return null;
        }
        FlowDefinition def = flowDefinitionMapper.selectOne(
                new LambdaQueryWrapper<FlowDefinition>()
                        .eq(FlowDefinition::getFlowCode, flowCode)
                        .eq(FlowDefinition::getStatus, 1)
                        .last("LIMIT 1"));
        return def != null ? def.getFlowCode() : null;
    }

    @SuppressWarnings("unchecked")
    private void buildResult(FlowQueryResultVO<Object> result, IPage<?> page) {
        result.setPage((com.baomidou.mybatisplus.core.metadata.IPage<Object>) page);
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        long totalPages = page.getSize() > 0 ? (page.getTotal() + page.getSize() - 1) / page.getSize() : 0;
        result.setTotalPages(totalPages);
        result.setRecords((java.util.List<Object>) page.getRecords());
    }
}




