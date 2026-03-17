package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.SysRole;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.entity.flow.*;
import com.rightmanage.mapper.flow.*;
import com.rightmanage.mapper.SysUserRoleMapper;
import com.rightmanage.service.flow.FlowInstanceService;
import com.rightmanage.service.flow.FlowDefinitionService;
import com.rightmanage.service.flow.FlowOperationLogService;
import com.rightmanage.service.SysUserService;
import com.rightmanage.service.SysRoleService;
import com.rightmanage.enums.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance> implements FlowInstanceService {

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
    private FlowTemplateParamMapper flowTemplateParamMapper;
    @Autowired
    private FlowInstanceParamMapper flowInstanceParamMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<FlowInstance> list() {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowInstance>()
                .orderByDesc(FlowInstance::getCreateTime));
    }

    @Override
    public Long startFlow(FlowStartDTO dto, Long userId) {
        // 1. 校验流程定义
        FlowDefinition flow = flowDefinitionMapper.selectById(dto.getFlowId());
        if (flow == null || flow.getStatus() != 1) {
            throw new RuntimeException("流程不存在或未启用");
        }

        // 2. 获取申请人信息（用于记录日志）
        SysUser applicant = sysUserService.getById(userId);

        // 3. 校验租户必填（如果流程包含产品智能定制角色）
        boolean needTenant = flowDefinitionService.checkFlowNeedTenant(dto.getFlowId());
        if (needTenant && dto.getTenantId() == null) {
            throw new RuntimeException("该流程包含产品智能定制审批节点，必须选择租户");
        }

        // 4. 校验凭证必填（如果流程需要上传凭证）
        if (flow.getNeedAttachment() != null && flow.getNeedAttachment() == 1) {
            if (dto.getAttachmentUrl() == null || dto.getAttachmentUrl().isEmpty()) {
                throw new RuntimeException("该流程需要上传凭证，请上传凭证文件（支持Excel、PDF、WPS格式）");
            }
        }

        // 5. 创建流程实例
        FlowInstance instance = new FlowInstance();
        instance.setFlowId(dto.getFlowId());
        instance.setInstanceName(dto.getInstanceName());
        instance.setApplicantId(userId);
        instance.setTenantId(dto.getTenantId()); // 设置租户ID
        instance.setCurrentNodeName("开始节点");
        instance.setStatus(FlowInstanceStatus.RUNNING.getCode());
        // 保存凭证信息
        instance.setAttachmentUrl(dto.getAttachmentUrl());
        instance.setAttachmentName(dto.getAttachmentName());

        // 生成额外信息（当流程模板ID为5时，生成灰度发布链接）
        if (dto.getFlowId() != null && dto.getFlowId() == 5L) {
            String randomUrl = "https://gray.example.com/release/" + UUID.randomUUID().toString().substring(0, 8);
            instance.setExtraInfo("请访问灰度发布链接：" + randomUrl);
        }

        flowInstanceMapper.insert(instance);

        // 5. 记录发起日志
        logService.saveLog(instance.getId(), userId, applicant.getUsername(),
                FlowOperationType.INIT.getCode(), "用户" + applicant.getUsername() + "发起流程：" + dto.getInstanceName());

        // 6. 获取节点配置
        List<FlowNodeConfig> nodeConfigs = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, dto.getFlowId())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        if (nodeConfigs.isEmpty()) {
            throw new RuntimeException("流程未配置节点，请先设计流程");
        }

        // 7. 处理第一个节点（开始节点自动流转）
        FlowNodeConfig firstNode = nodeConfigs.get(0);
        if (FlowNodeType.START.getCode().equals(firstNode.getNodeType())) {
            // 开始节点：自动完成，记录任务
            createAutoTask(instance.getId(), firstNode, "auto", "开始节点自动执行");
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.NOTIFY.getCode(), "开始节点自动触发，流转至下一个节点");

            // 流转到下一个节点
            if (nodeConfigs.size() > 1) {
                processNextNode(instance, nodeConfigs.get(1));
            } else {
                throw new RuntimeException("流程仅配置开始节点，无后续节点");
            }
        } else {
            // 第一个节点非开始节点，正常处理
            processNextNode(instance, firstNode);
        }

        // 8. 处理 REPORT_RELEASE 流程：填充文本节点的自定义字段值
        if ("REPORT_RELEASE".equals(flow.getFlowCode())) {
            fillTextNodeCustomFields(instance.getId(), nodeConfigs);
        }

        // 9. 保存流程参数
        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            saveFlowInstanceParams(instance.getId(), dto.getFlowId(), dto.getParams());
        }

        return instance.getId();
    }

    /**
     * 校验用户是否有发起权限
     */
    private boolean hasStartRolePermission(FlowDefinition flow, SysUser applicant) {
        if (applicant == null || !StringUtils.hasText(flow.getStartRoleIds())) {
            return false;
        }
        List<Long> userRoleIds = sysUserService.getRoleIdsByUserId(applicant.getId());
        String[] startRoles = flow.getStartRoleIds().split(",");
        for (Long roleId : userRoleIds) {
            for (String startRole : startRoles) {
                if (roleId.toString().equals(startRole.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 处理下一个节点
     */
    private void processNextNode(FlowInstance instance, FlowNodeConfig nextNode) {
        // 更新当前节点信息
        instance.setCurrentNodeKey(nextNode.getNodeKey());
        instance.setCurrentNodeName(nextNode.getNodeName());
        flowInstanceMapper.updateById(instance);

        if (FlowNodeType.APPROVE.getCode().equals(nextNode.getNodeType())) {
            // 审批节点：生成待办任务
            // 传递租户ID，用于产品智能定制模块的过滤
            String handlerIds = getRealHandlerIds(nextNode, instance.getTenantId());
            String handlerNames = getHandlerNames(nextNode, handlerIds);

            // 创建待办任务
            for (String handlerId : handlerIds.split(",")) {
                if (!StringUtils.hasText(handlerId)) continue;
                SysUser handler = sysUserService.getById(Long.parseLong(handlerId.trim()));
                FlowTask task = new FlowTask();
                task.setInstanceId(instance.getId());
                task.setNodeKey(nextNode.getNodeKey());
                task.setNodeName(nextNode.getNodeName());
                task.setNodeType(nextNode.getNodeType());
                task.setHandlerId(Long.parseLong(handlerId.trim()));
                task.setHandlerName(handler != null ? handler.getUsername() : "");
                task.setStatus(0); // 待处理
                flowTaskMapper.insert(task);
            }

            // 记录日志
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.INIT.getCode(),
                    "审批节点[" + nextNode.getNodeName() + "]已分配处理人：" + handlerNames);

        } else if (FlowNodeType.NOTIFY.getCode().equals(nextNode.getNodeType())) {
            // 通知节点：自动触发（日志模拟）
            String handlerIds = getRealHandlerIds(nextNode, instance.getTenantId());
            String handlerNames = getHandlerNames(nextNode, handlerIds);
            String notifyContent = nextNode.getNotifyContent();

            // 模拟通知：打印日志
            String notifyLog = "【流程通知】节点：" + nextNode.getNodeName() + "，通知对象：" + handlerNames + "，内容：" + notifyContent;
            System.out.println(notifyLog);

            // 记录通知任务（自动完成）
            createAutoTask(instance.getId(), nextNode, "notify", notifyContent);

            // 记录操作日志
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.NOTIFY.getCode(), notifyLog);

            // 自动流转到下一个节点
            flowToNextNode(instance, nextNode);

        } else if (FlowNodeType.END.getCode().equals(nextNode.getNodeType())) {
            // 结束节点：流程完成
            instance.setStatus(FlowInstanceStatus.COMPLETED.getCode());
            flowInstanceMapper.updateById(instance);

            // 记录结束任务
            createAutoTask(instance.getId(), nextNode, "auto", "流程完成");

            // 记录日志
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.COMPLETE.getCode(), "流程到达结束节点，状态改为已完成");

        } else if (FlowNodeType.TEXT.getCode().equals(nextNode.getNodeType())) {
            // 文本节点：自动通过，仅用于配置自定义字段做数据提示
            // 创建自动任务记录（以便后续填充自定义字段值）
            createAutoTask(instance.getId(), nextNode, "auto", "文本节点自动通过");

            // 记录日志
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.PASS.getCode(), "文本节点[" + nextNode.getNodeName() + "]自动通过");

            // 自动流转到下一个节点
            flowToNextNode(instance, nextNode);
        }
    }

    @Override
    public void approveFlow(Long taskId, String action, String comment, Long userId) {
        // 1. 任务校验
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (task == null || !task.getHandlerId().equals(userId) || task.getStatus() != 0) {
            throw new RuntimeException("无权限处理该任务或任务已处理");
        }

        // 2. 更新任务状态
        task.setAction(action);
        task.setComment(comment);
        task.setExecuteTime(new Date());
        task.setStatus("approve".equals(action) ? 1 : 2);
        flowTaskMapper.updateById(task);

        // 3. 获取流程实例
        FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
        SysUser operator = sysUserService.getById(userId);
        String actionName = "approve".equals(action) ? "审批通过" : "审批驳回";

        // 4. 记录操作日志
        logService.saveLog(instance.getId(), userId, operator.getUsername(),
                action, "用户" + operator.getUsername() + "对节点[" + task.getNodeName() + "]" + actionName + "，意见：" + comment);

        // 5. 处理审批结果
        if ("reject".equals(action)) {
            // 驳回：流程终止
            instance.setStatus(FlowInstanceStatus.REJECTED.getCode());
            flowInstanceMapper.updateById(instance);
        } else if ("approve".equals(action)) {
            // 通过：流转到下一个节点
            FlowNodeConfig currentNode = flowNodeConfigMapper.selectOne(
                    new LambdaQueryWrapper<FlowNodeConfig>()
                            .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
                            .eq(FlowNodeConfig::getNodeKey, task.getNodeKey())
            );
            flowToNextNode(instance, currentNode);
        }
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
    public List<FlowInstanceVO> myInitiated(Long userId, String moduleCode) {
        // 查询用户发起的流程
        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getApplicantId, userId)
                .eq(FlowInstance::getDeleted, 0)
                .orderByDesc(FlowInstance::getCreateTime);

        List<FlowInstance> instanceList = flowInstanceMapper.selectList(wrapper);

        // 如果指定了模块编码，需要过滤
        if (moduleCode != null && !moduleCode.isEmpty()) {
            List<FlowInstance> filteredList = new ArrayList<>();
            for (FlowInstance instance : instanceList) {
                FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
                if (flow != null && moduleCode.equals(flow.getModuleCode())) {
                    filteredList.add(instance);
                }
            }
            instanceList = filteredList;
        }

        List<FlowInstanceVO> voList = new ArrayList<>();
        for (FlowInstance instance : instanceList) {
            FlowInstanceVO vo = new FlowInstanceVO();
            BeanUtils.copyProperties(instance, vo);

            // 补充流程名称
            FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
            vo.setFlowName(flow != null ? flow.getFlowName() : "");

            // 补充状态名称
            FlowInstanceStatus status = FlowInstanceStatus.fromCode(instance.getStatus());
            vo.setStatusName(status != null ? status.getName() : "");

            voList.add(vo);
        }

        return voList;
    }

    @Override
    public List<FlowTaskVO> myApproval(Long userId, Integer taskStatus, String moduleCode) {
        // 先查询用户待办任务
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getHandlerId, userId)
                .eq(FlowTask::getStatus, taskStatus)
                .eq(FlowTask::getDeleted, 0)
                .orderByDesc(FlowTask::getCreateTime);

        List<FlowTask> taskList = flowTaskMapper.selectList(wrapper);

        // 如果指定了模块编码，需要过滤
        if (moduleCode != null && !moduleCode.isEmpty()) {
            List<FlowTask> filteredTasks = new ArrayList<>();
            for (FlowTask task : taskList) {
                FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
                if (instance != null) {
                    FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
                    if (flow != null && moduleCode.equals(flow.getModuleCode())) {
                        filteredTasks.add(task);
                    }
                }
            }
            taskList = filteredTasks;
        }

        List<FlowTaskVO> voList = new ArrayList<>();
        for (FlowTask task : taskList) {
            FlowTaskVO vo = new FlowTaskVO();
            BeanUtils.copyProperties(task, vo);

            // 补充流程信息
            FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
            if (instance != null) {
                FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
                vo.setFlowName(flow != null ? flow.getFlowName() : "");
                vo.setInstanceName(instance.getInstanceName());

                SysUser applicant = sysUserService.getById(instance.getApplicantId());
                vo.setApplicantName(applicant != null ? applicant.getUsername() : "");
            }

            voList.add(vo);
        }

        return voList;
    }

    @Override
    public FlowDetailVO getFlowDetail(Long instanceId) {
        // 1. 获取流程基本信息
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return null;
        }

        FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
        SysUser applicant = sysUserService.getById(instance.getApplicantId());

        // 2. 组装基本信息
        FlowDetailVO detailVO = new FlowDetailVO();
        detailVO.setInstanceId(instanceId);
        detailVO.setFlowName(flow != null ? flow.getFlowName() : "");
        detailVO.setInstanceName(instance.getInstanceName());
        detailVO.setApplicantName(applicant != null ? applicant.getUsername() : "");

        FlowInstanceStatus status = FlowInstanceStatus.fromCode(instance.getStatus());
        detailVO.setStatusName(status != null ? status.getName() : "");
        detailVO.setCreateTime(instance.getCreateTime());
        
        // 设置凭证信息
        detailVO.setAttachmentUrl(instance.getAttachmentUrl());
        detailVO.setAttachmentName(instance.getAttachmentName());

        // 设置额外信息
        detailVO.setExtraInfo(instance.getExtraInfo());

        // 3. 获取流程参数信息
        List<FlowInstanceParamVO> flowParamVOList = new ArrayList<>();
        // 查询实例参数
        FlowInstanceParam[] instanceParams = flowInstanceParamMapper.findByInstanceId(instanceId);
        if (instanceParams != null && instanceParams.length > 0 && flow != null) {
            // 获取流程模板参数配置，获取参数名称
            List<FlowTemplateParam> templateParams = flowTemplateParamMapper.findByTemplateId(flow.getId());
            final Map<String, String> paramNameMap = new HashMap<>();
            if (templateParams != null && !templateParams.isEmpty()) {
                for (FlowTemplateParam tp : templateParams) {
                    paramNameMap.put(tp.getParamCode(), tp.getParamName());
                }
            }
            // 组装参数VO
            for (FlowInstanceParam ip : instanceParams) {
                FlowInstanceParamVO paramVO = new FlowInstanceParamVO();
                paramVO.setParamName(paramNameMap.getOrDefault(ip.getParamCode(), ip.getParamCode()));
                paramVO.setParamValue(ip.getParamValue());
                paramVO.setParamValueLabel(ip.getParamValueLabel()); // 设置参数值的中文翻译
                flowParamVOList.add(paramVO);
            }
        }
        detailVO.setFlowParams(flowParamVOList);

        // 4. 获取节点执行记录
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

        // 4. 组装节点详情
        List<FlowNodeDetailVO> nodeDetailList = new ArrayList<>();
        for (FlowNodeConfig nodeConfig : nodeConfigs) {
            FlowNodeDetailVO nodeVO = new FlowNodeDetailVO();
            nodeVO.setNodeKey(nodeConfig.getNodeKey());
            nodeVO.setNodeName(nodeConfig.getNodeName());

            FlowNodeType nodeType = FlowNodeType.fromCode(nodeConfig.getNodeType());
            nodeVO.setNodeType(nodeType != null ? nodeType.getName() : "");

            // 设置节点的自定义字段配置
            nodeVO.setCustomFields(nodeConfig.getCustomFields());

            // 匹配节点任务
            List<FlowTask> nodeTasks = taskList.stream()
                    .filter(t -> t.getNodeKey().equals(nodeConfig.getNodeKey()))
                    .collect(Collectors.toList());

            if (!nodeTasks.isEmpty()) {
                FlowTask firstTask = nodeTasks.get(0);
                // 处理人（多个用逗号分隔）
                String handlerNames = nodeTasks.stream().map(FlowTask::getHandlerName).collect(Collectors.joining(","));
                nodeVO.setHandlerNames(handlerNames);

                if (firstTask.getAction() == null) {
                    nodeVO.setAction("自动");
                } else if ("approve".equals(firstTask.getAction())) {
                    nodeVO.setAction("通过");
                } else if ("reject".equals(firstTask.getAction())) {
                    nodeVO.setAction("驳回");
                } else if ("notify".equals(firstTask.getAction())) {
                    nodeVO.setAction("通知");
                } else {
                    nodeVO.setAction(firstTask.getAction());
                }

                nodeVO.setComment(firstTask.getComment());
                nodeVO.setExecuteTime(firstTask.getExecuteTime());
                nodeVO.setStatus(firstTask.getStatus() == 0 ? "待处理" : "已完成");

                // 设置任务的自定义字段值
                nodeVO.setCustomFieldValues(firstTask.getCustomFieldValues());
            } else {
                // 未执行的节点
                nodeVO.setHandlerNames(getHandlerNames(nodeConfig, nodeConfig.getHandlerIds()));
                nodeVO.setStatus("未执行");
            }

            nodeDetailList.add(nodeVO);
        }
        detailVO.setNodeList(nodeDetailList);

        // 5. 获取操作日志
        List<FlowOperationLog> logList = logService.listByInstanceId(instanceId);
        detailVO.setLogList(logList);

        return detailVO;
    }

    /**
     * 流转到下一个节点
     */
    private void flowToNextNode(FlowInstance instance, FlowNodeConfig currentNode) {
        List<FlowNodeConfig> allNodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        int currentIndex = -1;
        for (int i = 0; i < allNodes.size(); i++) {
            if (allNodes.get(i).getNodeKey().equals(currentNode.getNodeKey())) {
                currentIndex = i;
                break;
            }
        }

        // 存在下一个节点则继续流转
        if (currentIndex < allNodes.size() - 1) {
            processNextNode(instance, allNodes.get(currentIndex + 1));
        } else {
            // 无下一个节点，流程完成
            instance.setStatus(FlowInstanceStatus.COMPLETED.getCode());
            flowInstanceMapper.updateById(instance);
            logService.saveLog(instance.getId(), null, null,
                    FlowOperationType.COMPLETE.getCode(), "流程无后续节点，自动完成");
        }
    }

    /**
     * 创建自动任务（开始/通知/结束节点）
     */
    private void createAutoTask(Long instanceId, FlowNodeConfig nodeConfig, String action, String comment) {
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
        task.setStatus(1); // 已完成
        flowTaskMapper.insert(task);
    }

    /**
     * 填充文本节点的自定义字段值（模拟 REPORT_RELEASE 流程）
     * 当流程编码为 REPORT_RELEASE 时，随机生成 URL 并赋值给 REPORT_GREY_URL 字段
     */
    private void fillTextNodeCustomFields(Long instanceId, List<FlowNodeConfig> nodeConfigs) {
        try {
            // 1. 查找文本节点
            FlowNodeConfig textNode = nodeConfigs.stream()
                    .filter(node -> FlowNodeType.TEXT.getCode().equals(node.getNodeType()))
                    .findFirst()
                    .orElse(null);

            if (textNode == null || !StringUtils.hasText(textNode.getCustomFields())) {
                return;
            }

            // 2. 解析自定义字段配置
            List<Map<String, String>> customFieldsConfig = objectMapper.readValue(
                    textNode.getCustomFields(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );

            // 3. 检查是否包含 REPORT_GREY_URL 字段
            boolean hasGreyUrlField = customFieldsConfig.stream()
                    .anyMatch(field -> "REPORT_GREY_URL".equals(field.get("fieldName")));

            if (!hasGreyUrlField) {
                return;
            }

            // 4. 随机生成 URL
            String randomUrl = "https://gray.example.com/report/" + UUID.randomUUID().toString().substring(0, 8);

            // 5. 查找该文本节点对应的任务
            List<FlowTask> tasks = flowTaskMapper.selectList(
                    new LambdaQueryWrapper<FlowTask>()
                            .eq(FlowTask::getInstanceId, instanceId)
                            .eq(FlowTask::getNodeKey, textNode.getNodeKey())
            );

            if (tasks.isEmpty()) {
                return;
            }

            // 6. 填充自定义字段值
            Map<String, String> customFieldValues = new HashMap<>();
            customFieldValues.put("REPORT_GREY_URL", randomUrl);

            // 更新任务的自定义字段值
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
     * 解析处理人ID（角色→用户）
     * @param nodeConfig 节点配置
     * @param tenantId 租户ID（产品智能定制模块需要）
     */
    private String getRealHandlerIds(FlowNodeConfig nodeConfig, Long tenantId) {
        if (!StringUtils.hasText(nodeConfig.getHandlerType()) || !StringUtils.hasText(nodeConfig.getHandlerIds())) {
            return "";
        }

        Set<String> userIds = new HashSet<>();

        if ("role".equals(nodeConfig.getHandlerType())) {
            // 角色转用户 - 通过SysUserRole关联表查询
            for (String roleId : nodeConfig.getHandlerIds().split(",")) {
                if (!StringUtils.hasText(roleId)) continue;

                // 如果是产品智能定制模块（C）的角色，需要按租户过滤
                LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(SysUserRole::getRoleId, Long.parseLong(roleId.trim()));

                // 产品智能定制模块按租户过滤
                if ("C".equals(nodeConfig.getModuleCode()) && tenantId != null) {
                    queryWrapper.eq(SysUserRole::getTenantId, tenantId);
                }

                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(queryWrapper);
                for (SysUserRole ur : userRoles) {
                    userIds.add(ur.getUserId().toString());
                }
            }
        } else if ("user".equals(nodeConfig.getHandlerType())) {
            // 直接使用用户ID
            userIds.addAll(Arrays.asList(nodeConfig.getHandlerIds().split(",")));
        }

        return String.join(",", userIds);
    }

    /**
     * 获取处理人名称（用于展示）
     */
    private String getHandlerNames(FlowNodeConfig nodeConfig, String handlerIds) {
        if (!StringUtils.hasText(handlerIds)) {
            return "无";
        }

        StringBuilder names = new StringBuilder();

        if ("role".equals(nodeConfig.getHandlerType())) {
            // 角色名称
            for (String roleId : nodeConfig.getHandlerIds().split(",")) {
                if (!StringUtils.hasText(roleId)) continue;
                SysRole role = sysRoleService.getById(Long.parseLong(roleId.trim()));
                if (role != null) {
                    names.append("角色：").append(role.getRoleName()).append(",");
                }
            }
        } else if ("user".equals(nodeConfig.getHandlerType())) {
            // 用户名称
            for (String userId : handlerIds.split(",")) {
                if (!StringUtils.hasText(userId)) continue;
                SysUser user = sysUserService.getById(Long.parseLong(userId.trim()));
                if (user != null) {
                    names.append(user.getUsername()).append(",");
                }
            }
        }

        return names.length() > 0 ? names.substring(0, names.length() - 1) : "无";
    }

    /**
     * 保存流程实例参数
     */
    private void saveFlowInstanceParams(Long instanceId, Long flowId, Map<String, Object> params) {
        try {
            // 获取模板参数配置
            List<FlowTemplateParam> templateParams = flowTemplateParamMapper.findByTemplateId(flowId);
            if (templateParams == null || templateParams.isEmpty()) {
                return;
            }

            // 构建参数映射
            Map<String, Long> paramIdMap = new HashMap<>();
            Map<String, FlowTemplateParam> paramConfigMap = new HashMap<>();
            for (FlowTemplateParam tp : templateParams) {
                paramIdMap.put(tp.getParamCode(), tp.getId());
                paramConfigMap.put(tp.getParamCode(), tp);
            }

            // 批量保存实例参数
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

                    // 计算 paramValueLabel：如果是下拉框类型，从 optionJson 中匹配 label
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
            // 记录错误但不影响流程发起
            System.err.println("保存流程参数失败: " + e.getMessage());
        }
    }
}
