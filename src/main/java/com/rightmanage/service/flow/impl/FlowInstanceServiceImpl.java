package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.SysRole;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.entity.SysTenant;
import com.rightmanage.entity.flow.*;
import com.rightmanage.mapper.flow.*;
import com.rightmanage.mapper.BankOrgMapper;
import com.rightmanage.mapper.SysUserRoleMapper;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance> implements FlowInstanceService {

    private static final Logger log = LoggerFactory.getLogger(FlowInstanceServiceImpl.class);

    private final ThreadLocal<Map<String, DynamicHandlerDTO>> dynamicHandlerThreadLocal = new ThreadLocal<>();
    // 多租户审批节点：发起时选择的租户ID列表
    private final ThreadLocal<List<Long>> nodeTenantsThreadLocal = new ThreadLocal<>();
    // 机构相关审批：发起时选择的机构ID
    private final ThreadLocal<Long> nodeSourceOrgIdThreadLocal = new ThreadLocal<>();

    private static final Map<String, TaskCallbackContext> callbackTokenMap = new ConcurrentHashMap<>();

    @Data
    public static class TaskCallbackContext {
        private Long taskId;
        private Long instanceId;
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
        ctx.setTaskId(task.getId());
        ctx.setInstanceId(task.getInstanceId());
        ctx.setNodeKey(task.getNodeKey());
        ctx.setCallbackTime(task.getExecuteTime());

        FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
        if (instance != null) {
            FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
            ctx.setModuleCode(flow != null ? flow.getModuleCode() : null);
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
    private BankOrgMapper bankOrgMapper;
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

    @Override
    public List<FlowInstance> list() {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowInstance>()
                .orderByDesc(FlowInstance::getCreateTime));
    }

    @Override
    public Long startFlow(FlowStartDTO dto, Long userId) {
        System.out.println("startFlow called - flowId: " + dto.getFlowId() + ", userId: " + userId);
        try {
            return startFlowInternal(dto, userId);
        } catch (Exception e) {
            System.out.println("startFlow exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private Long startFlowInternal(FlowStartDTO dto, Long userId) {
        FlowDefinition flow = flowDefinitionMapper.selectById(dto.getFlowId());
        if (flow == null || flow.getStatus() != 1) {
            throw new RuntimeException("流程不存在或未启用");
        }

        // 从 nodeConfigs 构建运行时配置 map（nodeKey -> DTO），同时汇总 nodeTenants
        Map<String, DynamicHandlerDTO> dynamicHandlerMap = new HashMap<>();
        List<Long> resolvedNodeTenants = new ArrayList<>();

        if (dto.getNodeConfigs() != null && !dto.getNodeConfigs().isEmpty()) {
            for (FlowNodeConfigDTO nc : dto.getNodeConfigs()) {
                System.out.println("NodeConfig - nodeKey: " + nc.getNodeKey()
                        + ", handlerType: " + nc.getHandlerType()
                        + ", handlerId: " + nc.getHandlerId()
                        + ", handlerName: " + nc.getHandlerName()
                        + ", tenantId: " + nc.getTenantId());

                DynamicHandlerDTO dto2 = new DynamicHandlerDTO();
                dto2.setNodeKey(nc.getNodeKey());
                dto2.setHandlerId(nc.getHandlerId());
                dto2.setHandlerName(nc.getHandlerName());
                dto2.setTenantId(nc.getTenantId());
                dto2.setSourceOrgId(nc.getSourceOrgId());
                dynamicHandlerMap.put(nc.getNodeKey(), dto2);

                // 汇总非空的 tenantId（保留到列表，用于 ThreadLocal）
                if (nc.getTenantId() != null && !resolvedNodeTenants.contains(nc.getTenantId())) {
                    resolvedNodeTenants.add(nc.getTenantId());
                }
            }
        }
        dynamicHandlerThreadLocal.set(dynamicHandlerMap);

        // 收集 nodeConfigs 中的 sourceOrgId（取第一个非空值，用于机构相关审批）
        Long globalSourceOrgId = null;
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
        List<Long> nodeTenants = !resolvedNodeTenants.isEmpty() ? resolvedNodeTenants
                : (dto.getNodeTenants() != null ? dto.getNodeTenants() : new ArrayList<>());
        if (!nodeTenants.isEmpty()) {
            System.out.println("===== [多租户审批] 允许的租户 =====");
            System.out.println("  租户IDs: " + nodeTenants);
            System.out.println("========================================");
        }
        nodeTenantsThreadLocal.set(nodeTenants);

        SysUser applicant = sysUserService.getById(userId);

        boolean needTenant = flowDefinitionService.checkFlowNeedTenant(dto.getFlowId());
        if (needTenant && (nodeTenants == null || nodeTenants.isEmpty())) {
            throw new RuntimeException("该流程包含多租户审批节点，必须选择至少一个租户");
        }

        if (flow.getNeedAttachment() != null && flow.getNeedAttachment() == 1) {
            if (dto.getAttachmentUrl() == null || dto.getAttachmentUrl().isEmpty()) {
                throw new RuntimeException("该流程需要上传凭证，请上传凭证文件（支持Excel、PDF、WPS格式）");
            }
        }

        FlowInstance instance = new FlowInstance();
        instance.setFlowId(dto.getFlowId());
        instance.setInstanceName(dto.getInstanceName());
        instance.setApplicantId(userId);
        instance.setTenantId(dto.getTenantId());
        instance.setCurrentNodeName("开始节点");
        instance.setStatus(FlowInstanceStatus.RUNNING.getCode());
        instance.setAttachmentUrl(dto.getAttachmentUrl());
        instance.setAttachmentName(dto.getAttachmentName());

        if (dto.getFlowId() != null && dto.getFlowId() == 5L) {
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

        logService.saveLog(instance.getId(), userId, applicant.getUsername(),
                FlowOperationType.INIT.getCode(), "用户" + applicant.getUsername() + "发起流程：" + dto.getInstanceName());

        List<FlowNodeConfig> nodeConfigs = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, dto.getFlowId())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        if (nodeConfigs.isEmpty()) {
            throw new RuntimeException("流程未配置节点，请先设计流程");
        }

        List<FlowLine> flowLines = new ArrayList<>();
        if (flow.getFlowJson() != null && !flow.getFlowJson().isEmpty()) {
            try {
                FlowJsonData flowJsonData = objectMapper.readValue(flow.getFlowJson(), FlowJsonData.class);
                if (flowJsonData != null && flowJsonData.getLines() != null) {
                    flowLines = flowJsonData.getLines();
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

        // 构建节点映射：nodeKey -> FlowNodeConfig 和 uuid -> FlowNodeConfig
        Map<String, FlowNodeConfig> nodeKeyMap = new HashMap<>();
        Map<String, FlowNodeConfig> nodeIdMap = new HashMap<>();
        System.out.println("===== [调试] 构建节点映射 =====");
        for (FlowNodeConfig node : nodeConfigs) {
            nodeKeyMap.put(node.getNodeKey(), node);
            if (StringUtils.hasText(node.getUuid())) {
                nodeIdMap.put(node.getUuid(), node);
            }
            System.out.println("  节点: nodeKey=" + node.getNodeKey() + ", uuid=" + node.getUuid() + ", type=" + node.getNodeType());
        }
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
        createAutoTask(instance.getId(), startNode, "auto", "开始节点自动执行");
        logService.saveLog(instance.getId(), null, null,
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
            fillTextNodeCustomFields(instance.getId(), nodeConfigs);
        }

        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            saveFlowInstanceParams(instance.getId(), dto.getFlowId(), dto.getParams());
        }

        dynamicHandlerThreadLocal.remove();
        nodeTenantsThreadLocal.remove();
        return instance.getId();
    }

    /**
     * 【核心】根据连线查找当前节点的所有后续节点（支持并行分支）
     */
    private List<FlowNodeConfig> findNextNodesByLines(FlowNodeConfig currentNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {

        List<FlowNodeConfig> nextNodes = new ArrayList<>();

        // 获取当前节点的标识（可能是 nodeKey 或 uuid）
        String currentKey = currentNode.getNodeKey();
        String currentUuid = currentNode.getUuid();

        System.out.println("===== [调试] findNextNodesByLines =====");
        System.out.println("  当前节点: nodeKey=[" + currentKey + "], uuid=[" + currentUuid + "], type=[" + currentNode.getNodeType() + "]");
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
            boolean keyMatches = line.getFromNode().equals(currentKey);
            boolean uuidMatches = currentUuid != null && line.getFromNode().equals(currentUuid);
            boolean matches = keyMatches || uuidMatches;

            System.out.println("  连线" + i + ": fromNode=" + line.getFromNode() + ", toNode=" + line.getToNode()
                    + " | keyMatches=" + keyMatches + "(currentKey=[" + currentKey + "]), uuidMatches=" + uuidMatches + "(currentUuid=[" + currentUuid + "])"
                    + " | 匹配=" + matches);

            if (matches) {
                String targetKey = line.getToNode();
                System.out.println("  -> 匹配成功! targetKey=" + targetKey);
                // 如果 target 是 UUID，转为 nodeKey
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
        return nextNodes;
    }

    /**
     * 【核心】处理节点及其后续节点（递归处理并行分支）
     * @param instance 流程实例
     * @param node 当前节点
     * @param allNodes 所有节点
     * @param flowLines 所有连线
     * @param nodeKeyMap nodeKey 到节点的映射
     * @param nodeIdMap uuid 到节点的映射
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
                        .eq(FlowTask::getInstanceId, instance.getId())
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
            createAutoTask(instance.getId(), node, "notify", "通知节点自动执行");
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.NOTIFY.getCode(), "通知节点[" + node.getNodeName() + "]自动执行");

        } else if (FlowNodeType.END.getCode().equals(nodeType)) {
            // 【修复】结束节点：不立即创建任务，只记录日志
            // 结束节点的任务应该在所有审批节点完成后，由 checkAndCompleteInstance 统一创建
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.COMPLETE.getCode(), "流程到达结束节点，等待所有审批节点完成");
            // 直接返回，不创建任务，不继续处理后续节点
            return;

        } else if (FlowNodeType.TEXT.getCode().equals(nodeType)) {
            // 文本节点：自动通过
            createAutoTask(instance.getId(), node, "auto", "文本节点自动通过");
            logService.saveLog(instance.getId(), null, null,
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
                createAutoTask(instance.getId(), node, "auto", 
                        FlowNodeType.LOGIC_AND.getCode().equals(nodeType) ? "逻辑与节点自动通过" : "逻辑或节点自动通过");
                logService.saveLog(instance.getId(), null, null,
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
                logService.saveLog(instance.getId(), null, null,
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

        // 判断该节点是否为多租户审批（审批人模块是 multiTenant=1 且 handlerType=role）
        Long selectedTenant = null;
        boolean isMultiTenantNode = "role".equals(node.getHandlerType())
                && sysModuleService.isMultiTenant(node.getModuleCode());

        if (isMultiTenantNode) {
            // 优先从节点专属配置读取租户（前端在 nodeConfigs 中配置的），
            // 如果没有则降级使用全局 nodeTenantsThreadLocal（兼容旧逻辑）
            DynamicHandlerDTO nodeHandler = dynamicHandlerMap.get(node.getNodeKey());
            Long nodeSpecificTenant = null;
            if (nodeHandler != null && nodeHandler.getTenantId() != null) {
                nodeSpecificTenant = nodeHandler.getTenantId();
            } else {
                List<Long> threadTenants = nodeTenantsThreadLocal.get();
                if (threadTenants != null && !threadTenants.isEmpty()) {
                    nodeSpecificTenant = threadTenants.get(0);
                }
            }
            if (nodeSpecificTenant == null) {
                System.out.println("createApprovalTasks - 警告：节点 " + node.getNodeKey() + " 是多租户审批节点，但未传入租户选择");
                logService.saveLog(instance.getId(), null, null,
                        FlowOperationType.INIT.getCode(),
                        "审批节点[" + node.getNodeName() + "]是多租户审批节点，但未选择租户，无法分配处理人");
                return;
            }
            System.out.println("createApprovalTasks - 多租户审批节点[" + node.getNodeKey() + "]，允许的租户: " + nodeSpecificTenant);
            // 后续使用节点专属的租户
            selectedTenant = nodeSpecificTenant;
        }

        // 判断 role 类型审批是否有 org_related=1 的角色
        Long sourceOrgId = null;
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
                logService.saveLog(instance.getId(), null, null,
                        FlowOperationType.INIT.getCode(),
                        "审批节点[" + node.getNodeName() + "]是机构相关审批节点，但未选择发起机构，无法分配处理人");
                return;
            }
            System.out.println("createApprovalTasks - 机构相关审批节点[" + node.getNodeKey() + "]，发起机构ID: " + sourceOrgId);
        }

        // 收集所有符合条件的处理人及其归属租户
        Set<String> allHandlerIds = new HashSet<>();
        Map<String, Long> handlerTenantMap = new HashMap<>(); // handlerId -> tenantId

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
            String handlerIds = getRealHandlerIdsWithOrgFilter(node, instance.getTenantId(), sourceOrgId, dynamicHandlerMap);
            if (StringUtils.hasText(handlerIds)) {
                for (String hid : handlerIds.split(",")) {
                    if (StringUtils.hasText(hid)) {
                        allHandlerIds.add(hid.trim());
                        handlerTenantMap.put(hid.trim(), instance.getTenantId());
                    }
                }
            }
        }

        String handlerIds = String.join(",", allHandlerIds);
        String handlerNames = getHandlerNames(node, handlerIds, dynamicHandlerMap);

        if (allHandlerIds.isEmpty()) {
            System.out.println("createApprovalTasks - 警告：节点 " + node.getNodeKey() + " 没有处理人");
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.INIT.getCode(),
                    "审批节点[" + node.getNodeName() + "]没有配置处理人");
            return;
        }

        System.out.println("createApprovalTasks - handlerIds: " + handlerIds + ", handlerNames: " + handlerNames);

        for (String handlerId : allHandlerIds) {
            // 幂等检查：如果该 handler 的任务已存在，不再重复创建
            Long existingCount = flowTaskMapper.selectCount(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, instance.getId())
                            .eq(FlowTask::getNodeKey, node.getNodeKey())
                            .eq(FlowTask::getHandlerId, Long.parseLong(handlerId))
                            .eq(FlowTask::getDeleted, 0)
            );
            if (existingCount != null && existingCount > 0) {
                continue;
            }

            SysUser handler = sysUserService.getById(Long.parseLong(handlerId));
            FlowTask task = new FlowTask();
            task.setInstanceId(instance.getId());
            task.setNodeKey(node.getNodeKey());
            task.setNodeName(node.getNodeName());
            task.setNodeType(node.getNodeType());
            task.setHandlerId(Long.parseLong(handlerId));
            task.setHandlerName(handler != null ? handler.getUsername() : "");
            task.setStatus(0); // 待处理
            // 多租户审批节点：记录该用户归属的租户ID，用于过滤"我的审批"
            task.setTenantId(handlerTenantMap.get(handlerId));
            // 机构相关审批：记录发起机构ID，用于审批时的机构层级过滤
            task.setSourceOrgId(sourceOrgId);
            flowTaskMapper.insert(task);
        }

        logService.saveLog(instance.getId(), null, null,
                FlowOperationType.INIT.getCode(),
                "审批节点[" + node.getNodeName() + "]已分配处理人：" + handlerNames);

        // 模拟通知
        if ("1".equals(node.getEnableNotify())) {
            String handlerTypeText = "role".equals(node.getHandlerType()) ? "按角色" : ("user".equals(node.getHandlerType()) ? "按用户" : "动态用户");
            String notifyContent = node.getNotifyContent() != null ? node.getNotifyContent() : "";
            
            System.out.println("========== 审批节点通知模拟 ==========");
            System.out.println("节点名称: " + node.getNodeName());
            System.out.println("通知方式: " + node.getNotifyType());
            System.out.println("审批人类型: " + handlerTypeText);
            System.out.println("审批人: " + handlerNames);
            System.out.println("通知内容模板: " + (notifyContent.isEmpty() ? "（未填写）" : notifyContent));
            System.out.println("======================================");

            logService.saveLog(instance.getId(), null, null,
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
                        .eq(FlowTask::getInstanceId, instance.getId())
                        .eq(FlowTask::getDeleted, 0)
                        .eq(FlowTask::getNodeType, FlowNodeType.END.getCode())
        );

        // 获取所有任务
        List<FlowTask> allTasks = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getInstanceId, instance.getId())
                        .eq(FlowTask::getDeleted, 0)
        );

        System.out.println("checkAndCompleteInstance - 实例ID: " + instance.getId() + ", 状态: " + instance.getStatus());
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
                                .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
                                .eq(FlowNodeConfig::getNodeType, FlowNodeType.END.getCode())
                );
                if (!nodeConfigs.isEmpty()) {
                    FlowNodeConfig endNode = nodeConfigs.get(0);
                    createAutoTask(instance.getId(), endNode, "auto", "流程结束");
                    System.out.println("checkAndCompleteInstance - 为结束节点创建任务");
                }
            }

            // 重新查询验证
            FlowInstance updated = flowInstanceMapper.selectById(instance.getId());
            System.out.println("checkAndCompleteInstance - 更新后流程状态: " + updated.getStatus());
            
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.COMPLETE.getCode(), "流程所有节点已完成，状态改为已完成");
        }
    }

    @Override
    public void approveFlow(Long taskId, String action, String comment, Long userId) {
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (task == null || !task.getHandlerId().equals(userId) || task.getStatus() != 0) {
            throw new RuntimeException("无权限处理该任务或任务已处理");
        }

        task.setAction(action);
        task.setComment(comment);
        task.setExecuteTime(new Date());

        FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
        loadDynamicHandlersToThreadLocal(instance);

        SysUser operator = sysUserService.getById(userId);
        String actionName = "approve".equals(action) ? "审批通过" : "审批驳回";

        logService.saveLog(instance.getId(), userId, operator.getUsername(),
                action, "用户" + operator.getUsername() + "对节点[" + task.getNodeName() + "]" + actionName + "，意见：" + comment);

        System.out.println(">>> approveFlow - 用户[" + operator.getUsername() + "]审批通过节点[" + task.getNodeName()
                + "(key=" + task.getNodeKey() + ")]");

        if ("reject".equals(action)) {
            // 驳回：流程终止
            task.setStatus(2); // 已驳回
            flowTaskMapper.updateById(task);
            instance.setStatus(FlowInstanceStatus.REJECTED.getCode());
            flowInstanceMapper.updateById(instance);
            dynamicHandlerThreadLocal.remove();
            return;
        }

        // 【新增】检查该节点是否配置了业务执行模块
        FlowNodeConfig nodeConfig = flowNodeConfigMapper.selectOne(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
                        .eq(FlowNodeConfig::getNodeKey, task.getNodeKey())
        );

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
            logService.saveLog(instance.getId(), userId, operator.getUsername(),
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
                        .eq(FlowTask::getInstanceId, instance.getId())
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
            logService.saveLog(instance.getId(), parallelTask.getHandlerId(), parallelTask.getHandlerName(),
                    "skip", "审批节点[" + parallelTask.getNodeName() + "]其他审批人已通过，该任务已自动跳过");
            System.out.println("skipParallelTasks - 跳过并行任务: taskId=" + parallelTask.getId()
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
        FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
        if (flow == null) {
            System.out.println("checkLogicNodesAfterApproval - 流程定义为空，直接返回");
            return;
        }

        // 获取所有节点和连线
        List<FlowNodeConfig> allNodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        List<FlowLine> flowLines = new ArrayList<>();
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

        // 构建节点映射
        Map<String, FlowNodeConfig> nodeKeyMap = new HashMap<>();
        Map<String, FlowNodeConfig> nodeIdMap = new HashMap<>();
        for (FlowNodeConfig node : allNodes) {
            nodeKeyMap.put(node.getNodeKey(), node);
            if (StringUtils.hasText(node.getUuid())) {
                nodeIdMap.put(node.getUuid(), node);
            }
        }

        // 获取当前节点的配置
        FlowNodeConfig currentNode = nodeKeyMap.get(completedTask.getNodeKey());
        if (currentNode == null) return;

        // 查找所有后续逻辑节点
        List<FlowNodeConfig> logicNodes = findLogicNodesAfter(currentNode, allNodes, flowLines, nodeKeyMap, nodeIdMap);
        
        System.out.println("checkLogicNodesAfterApproval - 找到 " + logicNodes.size() + " 个后续逻辑节点");
        for (int i = 0; i < logicNodes.size(); i++) {
            System.out.println("  逻辑节点[" + i + "]: nodeKey=" + logicNodes.get(i).getNodeKey()
                    + ", uuid=" + logicNodes.get(i).getUuid());
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
                logService.saveLog(instance.getId(), null, null,
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
                logService.saveLog(instance.getId(), null, null,
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
                    + ", uuid=" + predecessorNodes.get(i).getUuid());
        }

        if (predecessorNodes.isEmpty()) {
            // 【BUG追踪】记录详细诊断信息
            log.warn("[BUG追踪] checkLogicNodeCondition - 逻辑节点[{}]没有找到前置审批节点！"
                    + "flowLines数量={}, allNodes数量={}, logicKey={}, logicUuid={}",
                    logicNode.getNodeName(), flowLines.size(), allNodes.size(), logicNode.getNodeKey(), logicNode.getUuid());
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
                        .eq(FlowTask::getInstanceId, instance.getId())
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
                        .eq(FlowTask::getInstanceId, instance.getId())
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
                            .eq(FlowTask::getInstanceId, instance.getId())
                            .eq(FlowTask::getNodeKey, nodeKey)
                            .eq(FlowTask::getStatus, 0)
                            .eq(FlowTask::getDeleted, 0)
            );
            
            for (FlowTask task : tasksToSkip) {
                task.setStatus(5); // 已跳过
                task.setAction("skip");
                task.setComment("因逻辑或节点其他分支已通过，该节点被自动跳过");
                task.setExecuteTime(new Date());
                flowTaskMapper.updateById(task);
                
                // 记录操作日志
                logService.saveLog(instance.getId(), null, null,
                        "skip", "审批节点[" + task.getNodeName() + "]因逻辑或条件满足被自动跳过");
                
                System.out.println("skipParallelApprovalNodes - 已将节点[" + task.getNodeName() + "]标记为已跳过");
            }
        }
        
        if (!skipNodeKeys.isEmpty()) {
            logService.saveLog(instance.getId(), null, null,
                    "skip", "逻辑或节点条件满足，跳过并列审批节点：" + String.join(", ", skipNodeKeys));
        }
    }

    /**
     * 查找所有指向指定逻辑节点的审批节点
     */
    private List<FlowNodeConfig> findPredecessorApprovalNodes(FlowNodeConfig logicNode,
            List<FlowNodeConfig> allNodes, List<FlowLine> flowLines,
            Map<String, FlowNodeConfig> nodeKeyMap, Map<String, FlowNodeConfig> nodeIdMap) {
        
        List<FlowNodeConfig> predecessors = new ArrayList<>();
        
        if (flowLines == null || flowLines.isEmpty()) {
            // 兼容模式：按 sort 顺序查找逻辑节点之前的所有审批节点
            int logicIndex = -1;
            for (int i = 0; i < allNodes.size(); i++) {
                if (allNodes.get(i).getNodeKey().equals(logicNode.getNodeKey())) {
                    logicIndex = i;
                    break;
                }
            }
            
            if (logicIndex > 0) {
                for (int i = logicIndex - 1; i >= 0; i--) {
                    FlowNodeConfig node = allNodes.get(i);
                    if (FlowNodeType.APPROVE.getCode().equals(node.getNodeType())) {
                        predecessors.add(node);
                    } else if (FlowNodeType.START.getCode().equals(node.getNodeType())) {
                        // 开始节点不计入
                    } else if (FlowNodeType.isLogicNode(node.getNodeType())) {
                        // 遇到其他逻辑节点，停止查找
                        break;
                    } else {
                        // 其他节点类型，停止查找
                        break;
                    }
                }
            }
            return predecessors;
        }

        // 使用连线信息查找
        String logicKey = logicNode.getNodeKey();
        String logicUuid = logicNode.getUuid();

        for (FlowLine line : flowLines) {
            // 检查连线是否指向该逻辑节点
            boolean pointsToLogic = line.getToNode().equals(logicKey) 
                    || (logicUuid != null && line.getToNode().equals(logicUuid));
            
            if (pointsToLogic) {
                // 找到指向逻辑节点的连线，获取起始节点
                String fromKey = line.getFromNode();
                FlowNodeConfig fromNode = nodeIdMap.get(fromKey);
                if (fromNode == null) {
                    fromNode = nodeKeyMap.get(fromKey);
                }
                
                if (fromNode != null && FlowNodeType.APPROVE.getCode().equals(fromNode.getNodeType())) {
                    if (!predecessors.contains(fromNode)) {
                        predecessors.add(fromNode);
                    }
                }
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
                        .eq(FlowTask::getInstanceId, instance.getId())
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
        Map<String, Object> params = flowCommonService.getFlowParams(instance.getId());

        String flowCode = "";
        FlowDefinition flowDefinition = flowDefinitionMapper.selectById(instance.getFlowId());
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
        System.out.println("【异步模块调用】流程实例ID: " + instance.getId());
        System.out.println("【异步模块调用】任务ID: " + task.getId());
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
        ctx.setTaskId(task.getId());
        ctx.setInstanceId(instance.getId());
        ctx.setModuleCode(moduleCode);
        ctx.setNodeKey(task.getNodeKey());
        ctx.setOperatorId(operator != null ? operator.getId() : null);
        ctx.setOperatorName(operator != null ? operator.getUsername() : "系统");
        ctx.setCallbackTime(new Date());
        callbackTokenMap.put(callbackToken, ctx);

        logService.saveLog(instance.getId(), operator != null ? operator.getId() : null,
                operator != null ? operator.getUsername() : "系统",
                "module_call_async", "异步调用模块[" + moduleCode + "]，flow_code: " + flowCode +
                "，node_key: " + task.getNodeKey() + "，callback_token: " + callbackToken);

        // 【模拟】在这里可以添加实际的HTTP调用逻辑，例如：
        // try {
        //     String moduleEndpoint = "http://module-service/api/execute";
        //     Map<String, Object> requestBody = new HashMap<>();
        //     requestBody.put("flow_code", flowCode);
        //     requestBody.put("node_key", task.getNodeKey());
        //     requestBody.put("instance_id", instance.getId());
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
            logService.saveLog(instance.getId(), ctx.getOperatorId(), ctx.getOperatorName(),
                    "callback_success", logMsg);

            System.out.println("【回调成功】任务ID: " + task.getId() + "，节点: " + task.getNodeKey());

            result.put("success", true);
            result.put("message", "回调处理成功");
            result.put("taskId", task.getId());
            result.put("instanceId", instance.getId());

        } else {
            task.setStatus(4);
            task.setComment((task.getComment() != null ? task.getComment() + "；" : "") +
                    "[外部回调失败] " + (StringUtils.hasText(message) ? message : "未知原因"));
            flowTaskMapper.updateById(task);

            String logMsg = "外部模块[" + ctx.getModuleCode() + "]回调失败";
            if (StringUtils.hasText(message)) {
                logMsg += "，原因: " + message;
            }
            logService.saveLog(instance.getId(), ctx.getOperatorId(), ctx.getOperatorName(),
                    "callback_failed", logMsg);

            result.put("success", true);
            result.put("message", "回调处理成功，任务已重置为「逻辑处理失败」状态");
            result.put("taskId", task.getId());
        }

        dynamicHandlerThreadLocal.remove();
        return result;
    }

    public void resetTaskStatus(Long taskId, Long userId, String reason) {
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

        logService.saveLog(instance.getId(), userId, sysUserService.getById(userId).getUsername(),
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
                                // 读取租户（支持新旧两种格式：新单选 tenantId、旧列表 tenantIds）
                                Long tenantId = null;
                                if (cfg.containsKey("tenantId") && cfg.get("tenantId") != null) {
                                    Object tid = cfg.get("tenantId");
                                    tenantId = tid instanceof Number ? ((Number) tid).longValue() : Long.parseLong(tid.toString());
                                } else if (cfg.containsKey("tenantIds") && cfg.get("tenantIds") != null) {
                                    // 兼容旧数据：取列表第一个
                                    @SuppressWarnings("unchecked")
                                    List<?> oldList = (List<?>) cfg.get("tenantIds");
                                    if (!oldList.isEmpty()) {
                                        Object tid = oldList.get(0);
                                        tenantId = tid instanceof Number ? ((Number) tid).longValue() : Long.parseLong(tid.toString());
                                    }
                                }
                                dto.setTenantId(tenantId);
                                // 恢复发起机构ID（机构相关审批）
                                Object sourceOrgId = cfg.get("sourceOrgId");
                                if (sourceOrgId != null) {
                                    dto.setSourceOrgId(sourceOrgId instanceof Number ? ((Number) sourceOrgId).longValue() : Long.parseLong(sourceOrgId.toString()));
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
                    List<Long> nodeTenants = objectMapper.convertValue(extraData.get("nodeTenants"),
                            new TypeReference<List<Long>>() {});
                    nodeTenantsThreadLocal.set(nodeTenants != null ? nodeTenants : new ArrayList<>());
                    System.out.println("===== [多租户审批] 审批时恢复租户列表 =====");
                    System.out.println("  租户IDs: " + nodeTenantsThreadLocal.get());
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
                                Long orgId = sourceOrgId instanceof Number ? ((Number) sourceOrgId).longValue() : Long.parseLong(sourceOrgId.toString());
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
    public void cancelFlow(Long instanceId, Long userId) {
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

        instance.setStatus(FlowInstanceStatus.CANCELED.getCode());
        flowInstanceMapper.updateById(instance);

        logService.saveLog(instanceId, userId, sysUserService.getById(userId).getUsername(),
                FlowOperationType.CANCEL.getCode(), "用户撤销流程");
    }

    @Override
    public void terminateFlow(Long instanceId, Long userId) {
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
    public IPage<FlowInstanceVO> myInitiated(Long userId, String moduleCode, Long tenantId, Long flowId, Integer pageNum, Integer pageSize) {
        Page<FlowInstance> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getApplicantId, userId)
                .eq(FlowInstance::getDeleted, 0)
                .orderByDesc(FlowInstance::getCreateTime);

        if (flowId != null) {
            wrapper.eq(FlowInstance::getFlowId, flowId);
        }

        if (tenantId != null) {
            wrapper.eq(FlowInstance::getTenantId, tenantId);
        }

        IPage<FlowInstance> instancePage = flowInstanceMapper.selectPage(page, wrapper);
        List<FlowInstance> instanceList = instancePage.getRecords();

        if (moduleCode != null && !moduleCode.isEmpty()) {
            List<FlowInstance> filteredList = new ArrayList<>();
            for (FlowInstance inst : instanceList) {
                FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowId());
                if (flow != null && moduleCode.equals(flow.getModuleCode())) {
                    filteredList.add(inst);
                }
            }
            instanceList = filteredList;
        }

        List<FlowInstanceVO> voList = new ArrayList<>();
        for (FlowInstance inst : instanceList) {
            FlowInstanceVO vo = new FlowInstanceVO();
            BeanUtils.copyProperties(inst, vo);

            FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowId());
            if (flow != null) {
                vo.setFlowName(flow.getFlowName());
                if (moduleCode != null && !moduleCode.isEmpty() && !moduleCode.equals(flow.getModuleCode())) {
                    continue;
                }
            }

            FlowInstanceStatus status = FlowInstanceStatus.fromCode(inst.getStatus());
            vo.setStatusName(status != null ? status.getName() : "");

            FlowTask latestTask = flowTaskMapper.selectOne(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, inst.getId())
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
                                .eq(FlowNodeConfig::getFlowId, inst.getFlowId())
                                .eq(FlowNodeConfig::getNodeKey, inst.getCurrentNodeKey())
                );
                if (currentNode != null && FlowNodeType.APPROVE.getCode().equals(currentNode.getNodeType())) {
                    vo.setEnableNotify(currentNode.getEnableNotify());
                    vo.setNotifyType(currentNode.getNotifyType());
                }
            }

            voList.add(vo);
        }

        Page<FlowInstanceVO> resultPage = new Page<>(instancePage.getCurrent(), instancePage.getSize(), voList.size());
        resultPage.setRecords(voList);
        resultPage.setTotal(instancePage.getTotal());

        return resultPage;
    }

    @Override
    public IPage<FlowTaskVO> myApproval(Long userId, Integer taskStatus, String moduleCode, Long tenantId, Long flowId, Integer pageNum, Integer pageSize) {
        Page<FlowTask> page = new Page<>(pageNum, pageSize);

        // 多租户过滤：如果传了 tenantId（前端指定了要查看的租户），只看该租户的任务
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getHandlerId, userId)
                .eq(FlowTask::getStatus, taskStatus)
                .eq(FlowTask::getDeleted, 0);
        if (tenantId != null) {
            wrapper.and(w -> w.eq(FlowTask::getTenantId, tenantId).or().isNull(FlowTask::getTenantId));
        }
        wrapper.orderByDesc(FlowTask::getCreateTime);

        IPage<FlowTask> taskPage = flowTaskMapper.selectPage(page, wrapper);
        List<FlowTask> taskList = taskPage.getRecords();

        // 按模块/流程过滤（用于前端筛选展示）
        if ((moduleCode != null && !moduleCode.isEmpty()) || tenantId != null || flowId != null) {
            List<FlowTask> filteredTasks = new ArrayList<>();
            for (FlowTask t : taskList) {
                FlowInstance inst = flowInstanceMapper.selectById(t.getInstanceId());
                if (inst == null) {
                    continue;
                }
                // 多租户审批节点过滤：有效租户 = 任务自己的租户 > 实例的租户
                Long effectiveTenantId = t.getTenantId();
                if (effectiveTenantId == null) {
                    effectiveTenantId = inst.getTenantId();
                }
                if (tenantId != null && !tenantId.equals(effectiveTenantId)) {
                    continue;
                }
                FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowId());
                if (flow != null) {
                    if (moduleCode != null && !moduleCode.isEmpty() && !moduleCode.equals(flow.getModuleCode())) {
                        continue;
                    }
                    if (flowId != null && !flowId.equals(flow.getId())) {
                        continue;
                    }
                    filteredTasks.add(t);
                }
            }
            taskList = filteredTasks;
        }

        // 机构层级过滤：仅当 sourceOrgId 不为空时生效。
        // 逻辑说明：从 sourceOrgId 出发，依次向上遍历（parentId），直到根（parentId=0）。
        // 若用户的授权机构在此路径上，则有权审批此任务。
        List<FlowTask> finalTaskList = new ArrayList<>();
        for (FlowTask t : taskList) {
            Long srcOrg = t.getSourceOrgId();
            if (srcOrg == null) {
                // 无机构要求，任何人都能看（已在任务分配时过滤过，这里安全放行）
                finalTaskList.add(t);
                continue;
            }
            // 获取该任务所属的流程模块与租户
            FlowInstance inst = flowInstanceMapper.selectById(t.getInstanceId());
            if (inst == null) {
                continue;
            }
            FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowId());
            if (flow == null) {
                continue;
            }
            Long effectiveTenantId = t.getTenantId() != null ? t.getTenantId() : inst.getTenantId();
            // 调用独立方法判断机构层级权限（便于后续扩展）
            boolean authorized = sysUserService.isUserAuthorizedForOrgLevel(
                    userId, flow.getModuleCode(), effectiveTenantId, srcOrg);
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
                FlowDefinition flow = flowDefinitionMapper.selectById(inst.getFlowId());
                vo.setFlowName(flow != null ? flow.getFlowName() : "");
                vo.setInstanceName(inst.getInstanceName());

                SysUser applicant = sysUserService.getById(inst.getApplicantId());
                vo.setApplicantName(applicant != null ? applicant.getUsername() : "");

                // 填充租户名称（优先用任务自己的租户ID，其次用实例的租户ID）
                Long effectiveTenantId = t.getTenantId();
                if (effectiveTenantId == null || effectiveTenantId == 0) {
                    effectiveTenantId = inst.getTenantId();
                }
                if (effectiveTenantId != null) {
                    SysTenant tenant = sysTenantService.getById(effectiveTenantId);
                    vo.setTenantName(tenant != null ? tenant.getTenantName() : "");
                }
                // 填充发起机构名称
                if (t.getSourceOrgId() != null) {
                    BankOrg srcOrg = bankOrgMapper.selectById(t.getSourceOrgId());
                    vo.setSourceOrgName(srcOrg != null ? srcOrg.getName() : "");
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
    public FlowDetailVO getFlowDetail(Long instanceId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return null;
        }

        FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
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
            List<FlowTemplateParam> templateParams = flowTemplateParamMapper.findByTemplateId(flow.getId());
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
                        .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
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
        if (flow != null && StringUtils.hasText(flow.getFlowJson())) {
            try {
                FlowJsonData flowJsonData = objectMapper.readValue(flow.getFlowJson(), FlowJsonData.class);
                if (flowJsonData != null && flowJsonData.getLines() != null) {
                    flowLines = flowJsonData.getLines();
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
            if (StringUtils.hasText(node.getUuid())) {
                nodeIdMap.put(node.getUuid(), node);
            }
        }

        // 【优化】根据拓扑排序重组节点列表，显示并行关系
        List<FlowNodeDetailVO> nodeDetailList = buildNodeDetailListWithParallelism(
                nodeConfigs, taskList, flowLines, nodeKeyMap, nodeIdMap, dynamicHandlerMap, instance.getStatus());

        detailVO.setNodeList(nodeDetailList);

        List<FlowOperationLog> logList = logService.listByInstanceId(instanceId);
        detailVO.setLogList(logList);

        return detailVO;
    }

    /**
     * 【优化】根据拓扑排序和并行关系构建节点详情列表
     * 1. 过滤掉逻辑与/逻辑或节点（不展示）
     * 2. 按拓扑分层，同一层级的审批节点并排显示
     * 3. end 节点：没有任务时显示"未执行"，有 status=1 任务才显示"已完成"
     * 4. 使用 nodeKey 作为唯一标识（优先 nodeKey，空时用 uuid），保证 start/end 等空 nodeKey 节点不冲突
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

        // 获取节点唯一标识：优先 nodeKey，空时用 uuid，保证 start/end 等节点不冲突
        java.util.function.Function<FlowNodeConfig, String> getNodeId = node -> {
            if (node.getNodeKey() != null && !node.getNodeKey().isEmpty()) {
                return node.getNodeKey();
            }
            return node.getUuid();
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
                        } else if (t.getId() != null && latestTask.getId() != null
                                && t.getId() > latestTask.getId()) {
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
                        String statusText;
                        switch (firstTask.getStatus()) {
                            case 0: statusText = "待处理"; break;
                            case 1: statusText = "已完成"; break;
                            case 2: statusText = "已驳回"; break;
                            case 3: statusText = "业务执行中"; break;
                            case 4: statusText = "逻辑处理失败"; break;
                            case 5: statusText = "已跳过"; break;
                            default: statusText = "未知"; break;
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

    private void createAutoTask(Long instanceId, FlowNodeConfig nodeConfig, String action, String comment) {
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

    private void fillTextNodeCustomFields(Long instanceId, List<FlowNodeConfig> nodeConfigs) {
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
    private boolean isOrgRelatedRole(String roleId) {
        if (!StringUtils.hasText(roleId)) return false;
        SysRole role = sysRoleService.getById(Long.parseLong(roleId.trim()));
        return role != null && Boolean.TRUE.equals(role.getOrgRelated());
    }

    private String getRealHandlerIds(FlowNodeConfig nodeConfig, Long tenantId, Map<String, DynamicHandlerDTO> dynamicHandlerMap) {
        return getRealHandlerIdsWithOrgFilter(nodeConfig, tenantId, null, dynamicHandlerMap);
    }

    /**
     * 获取符合条件的处理人ID，支持机构层级过滤
     * @param sourceOrgId 发起机构ID（机构相关审批时有效）
     */
    private String getRealHandlerIdsWithOrgFilter(FlowNodeConfig nodeConfig, Long tenantId, Long sourceOrgId,
            Map<String, DynamicHandlerDTO> dynamicHandlerMap) {
        if (!StringUtils.hasText(nodeConfig.getHandlerType())) {
            return "";
        }

        if ("dynamic".equals(nodeConfig.getHandlerType())) {
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
                queryWrapper.eq(SysUserRole::getRoleId, Long.parseLong(roleId.trim()));

                // 多租户模块需要按租户筛选角色成员
                if (isMultiTenantModule(nodeConfig.getModuleCode()) && tenantId != null) {
                    queryWrapper.eq(SysUserRole::getTenantId, tenantId);
                }

                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(queryWrapper);
                for (SysUserRole ur : userRoles) {
                    // 机构层级过滤：调用独立方法判断（便于后续扩展）
                    if (sourceOrgId != null) {
                        boolean authorized = sysUserService.isUserAuthorizedForOrgLevel(
                                ur.getUserId(), nodeConfig.getModuleCode(), tenantId, sourceOrgId);
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
    private Long getUserOrgId(Long userId, String moduleCode, Long tenantId) {
        LambdaQueryWrapper<com.rightmanage.entity.SysUserOrgAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.rightmanage.entity.SysUserOrgAuth::getUserId, userId)
                .eq(com.rightmanage.entity.SysUserOrgAuth::getModuleCode, moduleCode);
        if (tenantId == null) {
            wrapper.isNull(com.rightmanage.entity.SysUserOrgAuth::getTenantId);
        } else {
            wrapper.eq(com.rightmanage.entity.SysUserOrgAuth::getTenantId, tenantId);
        }
        wrapper.last("limit 1");
        com.rightmanage.entity.BankOrg org = sysUserService.getAuthorizedOrg(userId, moduleCode, tenantId);
        return org != null ? org.getId() : null;
    }

    private String getHandlerNames(FlowNodeConfig nodeConfig, String handlerIds, Map<String, DynamicHandlerDTO> dynamicHandlerMap) {
        if (!StringUtils.hasText(handlerIds)) {
            return "无";
        }

        StringBuilder names = new StringBuilder();

        if ("role".equals(nodeConfig.getHandlerType())) {
            for (String roleId : handlerIds.split(",")) {
                if (!StringUtils.hasText(roleId)) continue;
                SysRole role = sysRoleService.getById(Long.parseLong(roleId.trim()));
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
        } else if ("dynamic".equals(nodeConfig.getHandlerType())) {
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

    private void saveFlowInstanceParams(Long instanceId, Long flowId, Map<String, Object> params) {
        try {
            List<FlowTemplateParam> templateParams = flowTemplateParamMapper.findByTemplateId(flowId);
            if (templateParams == null || templateParams.isEmpty()) {
                return;
            }

            Map<String, Long> paramIdMap = new HashMap<>();
            Map<String, FlowTemplateParam> paramConfigMap = new HashMap<>();
            for (FlowTemplateParam tp : templateParams) {
                paramIdMap.put(tp.getParamCode(), tp.getId());
                paramConfigMap.put(tp.getParamCode(), tp);
            }

            List<FlowInstanceParam> instanceParams = new ArrayList<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String paramCode = entry.getKey();
                Object paramValue = entry.getValue();

                if (paramIdMap.containsKey(paramCode) && paramValue != null) {
                    FlowInstanceParam ip = new FlowInstanceParam();
                    ip.setInstanceId(instanceId);
                    ip.setTemplateParamId(paramIdMap.get(paramCode));
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
    public String triggerNodeNotify(Long instanceId, Long userId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return "流程实例不存在";
        }

        if (instance.getStatus() != FlowInstanceStatus.RUNNING.getCode()) {
            return "流程状态不是运行中，无法触发通知";
        }

        FlowNodeConfig currentNode = flowNodeConfigMapper.selectOne(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
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
        List<Long> nodeTenants = new ArrayList<>();
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
                        List<Long> nts = objectMapper.convertValue(extraData.get("nodeTenants"),
                                new TypeReference<List<Long>>() {});
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
            for (Long tenantId : nodeTenants) {
                String handlerIds = getRealHandlerIds(currentNode, tenantId, dynamicHandlerMap);
                if (StringUtils.hasText(handlerIds)) {
                    for (String hid : handlerIds.split(",")) {
                        if (StringUtils.hasText(hid)) {
                            allHandlerIds.add(hid.trim());
                        }
                    }
                }
            }
        } else {
            String handlerIds = getRealHandlerIds(currentNode, instance.getTenantId(), dynamicHandlerMap);
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
                SysTenant t = sysTenantService.getById(tid);
                return t != null ? t.getTenantName() : ("ID=" + tid);
            }).collect(Collectors.joining("、"));
            tenantInfo = "多租户审批，所选租户: " + names;
        } else if (instance.getTenantId() != null) {
            SysTenant tenant = sysTenantService.getById(instance.getTenantId());
            tenantInfo = tenant != null
                    ? "租户ID: " + tenant.getId() + "，租户名称: " + tenant.getTenantName()
                    : "租户ID: " + instance.getTenantId() + "（未找到）";
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
}
