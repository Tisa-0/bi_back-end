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

@Service
@Transactional
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance> implements FlowInstanceService {

    @Autowired
    private FlowInstanceMapper flowInstanceMapper;
    @Autowired
    private FlowDefinitionMapper flowDefinitionMapper;
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

        // 2. 校验发起权限
        SysUser applicant = sysUserService.getById(userId);
        if (!hasStartRolePermission(flow, applicant)) {
            throw new RuntimeException("当前角色无权限发起该流程");
        }

        // 3. 创建流程实例
        FlowInstance instance = new FlowInstance();
        instance.setFlowId(dto.getFlowId());
        instance.setInstanceName(dto.getInstanceName());
        instance.setApplicantId(userId);
        instance.setCurrentNodeName("开始节点");
        instance.setStatus(FlowInstanceStatus.RUNNING.getCode());
        flowInstanceMapper.insert(instance);

        // 4. 记录发起日志
        logService.saveLog(instance.getId(), userId, applicant.getUsername(),
                FlowOperationType.INIT.getCode(), "用户" + applicant.getUsername() + "发起流程：" + dto.getInstanceName());

        // 5. 获取节点配置
        List<FlowNodeConfig> nodeConfigs = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, dto.getFlowId())
                        .orderByAsc(FlowNodeConfig::getSort)
        );

        if (nodeConfigs.isEmpty()) {
            throw new RuntimeException("流程未配置节点，请先设计流程");
        }

        // 6. 处理第一个节点（开始节点自动流转）
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
            String handlerIds = getRealHandlerIds(nextNode);
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
            String handlerIds = getRealHandlerIds(nextNode);
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
    public List<FlowInstanceVO> myInitiated(Long userId) {
        List<FlowInstance> instanceList = flowInstanceMapper.selectList(
                new LambdaQueryWrapper<FlowInstance>()
                        .eq(FlowInstance::getApplicantId, userId)
                        .eq(FlowInstance::getDeleted, 0)
                        .orderByDesc(FlowInstance::getCreateTime)
        );

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
    public List<FlowTaskVO> myApproval(Long userId, Integer taskStatus) {
        List<FlowTask> taskList = flowTaskMapper.selectList(
                new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getHandlerId, userId)
                        .eq(FlowTask::getStatus, taskStatus)
                        .eq(FlowTask::getDeleted, 0)
                        .orderByDesc(FlowTask::getCreateTime)
        );

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

        // 3. 获取节点执行记录
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
     * 解析处理人ID（角色→用户）
     */
    private String getRealHandlerIds(FlowNodeConfig nodeConfig) {
        if (!StringUtils.hasText(nodeConfig.getHandlerType()) || !StringUtils.hasText(nodeConfig.getHandlerIds())) {
            return "";
        }

        Set<String> userIds = new HashSet<>();

        if ("role".equals(nodeConfig.getHandlerType())) {
            // 角色转用户 - 通过SysUserRole关联表查询
            for (String roleId : nodeConfig.getHandlerIds().split(",")) {
                if (!StringUtils.hasText(roleId)) continue;
                // 查询拥有该角色的用户ID
                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, Long.parseLong(roleId.trim()))
                );
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
}
