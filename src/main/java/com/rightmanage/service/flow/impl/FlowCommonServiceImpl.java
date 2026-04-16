package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.flow.*;
import com.rightmanage.enums.FlowInstanceStatus;
import com.rightmanage.enums.FlowOperationType;
import com.rightmanage.mapper.flow.*;
import com.rightmanage.service.SysUserService;
import com.rightmanage.service.flow.FlowCommonService;
import com.rightmanage.service.flow.FlowInstanceService;
import com.rightmanage.service.flow.FlowOperationLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 流程通用服务实现类
 * 提供流程管理的通用方法，供其他模块调用
 */
@Service
@Transactional
public class FlowCommonServiceImpl implements FlowCommonService {

    @Autowired
    private FlowInstanceMapper flowInstanceMapper;
    @Autowired
    private FlowDefinitionMapper flowDefinitionMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTemplateParamMapper flowTemplateParamMapper;
    @Autowired
    private FlowInstanceParamMapper flowInstanceParamMapper;
    @Autowired
    private FlowOperationLogMapper flowOperationLogMapper;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private FlowOperationLogService logService;
    @Autowired
    private FlowInstanceService flowInstanceService;

    // ==================== 审批记录查询 ====================

    @Override
    public IPage<FlowInstanceVO> getApprovalRecords(String moduleCode, String flowCode, String tenantCode,
                                                      Integer status, Long applicantId, Integer pageNum, Integer pageSize) {
        Page<FlowInstance> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getDeleted, 0)
                .orderByDesc(FlowInstance::getCreateTime);

        if (flowCode != null) {
            wrapper.eq(FlowInstance::getFlowCode, flowCode);
        }
        if (tenantCode != null && !tenantCode.trim().isEmpty()) {
            wrapper.eq(FlowInstance::getTenantCode, tenantCode);
        }
        if (status != null) {
            wrapper.eq(FlowInstance::getStatus, status);
        }
        if (applicantId != null) {
            wrapper.eq(FlowInstance::getApplicantId, applicantId);
        }

        IPage<FlowInstance> instancePage = flowInstanceMapper.selectPage(page, wrapper);
        List<FlowInstance> instanceList = instancePage.getRecords();

        // 如果指定了模块编码，需要过滤
        if (StringUtils.hasText(moduleCode)) {
            instanceList = instanceList.stream()
                    .filter(instance -> moduleCode.equals(instance.getModuleCode()))
                    .collect(Collectors.toList());
        }

        // 转换为VO
        List<FlowInstanceVO> voList = convertToVOList(instanceList);

        // 重新构建分页结果
        Page<FlowInstanceVO> resultPage = new Page<>(instancePage.getCurrent(), instancePage.getSize(), voList.size());
        resultPage.setRecords(voList);
        resultPage.setTotal(instancePage.getTotal());

        return resultPage;
    }

    @Override
    public List<FlowInstanceVO> getApprovalRecords(String moduleCode, String flowCode, String tenantCode, Integer status) {
        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getDeleted, 0)
                .orderByDesc(FlowInstance::getCreateTime);

        if (flowCode != null) {
            wrapper.eq(FlowInstance::getFlowCode, flowCode);
        }
        if (tenantCode != null && !tenantCode.trim().isEmpty()) {
            wrapper.eq(FlowInstance::getTenantCode, tenantCode);
        }
        if (status != null) {
            wrapper.eq(FlowInstance::getStatus, status);
        }

        List<FlowInstance> instanceList = flowInstanceMapper.selectList(wrapper);

        // 如果指定了模块编码，需要过滤
        if (StringUtils.hasText(moduleCode)) {
            instanceList = instanceList.stream()
                    .filter(instance -> moduleCode.equals(instance.getModuleCode()))
                    .collect(Collectors.toList());
        }

        return convertToVOList(instanceList);
    }

    @Override
    public IPage<FlowInstanceVO> getUserInitiatedFlows(Long userId, String moduleCode, Integer status,
                                                        Integer pageNum, Integer pageSize) {
        Page<FlowInstance> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getApplicantId, userId)
                .eq(FlowInstance::getDeleted, 0)
                .orderByDesc(FlowInstance::getCreateTime);

        if (status != null) {
            wrapper.eq(FlowInstance::getStatus, status);
        }

        IPage<FlowInstance> instancePage = flowInstanceMapper.selectPage(page, wrapper);
        List<FlowInstance> instanceList = instancePage.getRecords();

        // 如果指定了模块编码，需要过滤
        if (StringUtils.hasText(moduleCode)) {
            instanceList = instanceList.stream()
                    .filter(instance -> moduleCode.equals(instance.getModuleCode()))
                    .collect(Collectors.toList());
        }

        List<FlowInstanceVO> voList = convertToVOList(instanceList);

        Page<FlowInstanceVO> resultPage = new Page<>(instancePage.getCurrent(), instancePage.getSize(), voList.size());
        resultPage.setRecords(voList);
        resultPage.setTotal(instancePage.getTotal());

        return resultPage;
    }

    private List<FlowInstanceVO> convertToVOList(List<FlowInstance> instanceList) {
        List<FlowInstanceVO> voList = new ArrayList<>();
        for (FlowInstance instance : instanceList) {
            FlowInstanceVO vo = new FlowInstanceVO();
            BeanUtils.copyProperties(instance, vo);

            FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowCode());
            vo.setFlowName(flow != null ? flow.getFlowName() : "");

            FlowInstanceStatus statusEnum = FlowInstanceStatus.fromCode(instance.getStatus());
            vo.setStatusName(statusEnum != null ? statusEnum.getName() : "");

            voList.add(vo);
        }
        return voList;
    }

    // ==================== 审批记录详情 ====================

    @Override
    public FlowTaskVO getCurrentNode(String instanceId) {
        // 查询该实例下状态为待处理的任务（当前节点）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getInstanceId, instanceId)
                .eq(FlowTask::getStatus, 0) // 待处理
                .eq(FlowTask::getDeleted, 0)
                .orderByDesc(FlowTask::getCreateTime)
                .last("LIMIT 1");

        FlowTask task = flowTaskMapper.selectOne(wrapper);
        if (task == null) {
            return null;
        }

        FlowTaskVO vo = new FlowTaskVO();
        BeanUtils.copyProperties(task, vo);
        vo.setCurrentNodeName(task.getNodeName());
        vo.setCreateTime(task.getCreateTime());

        // 补充流程信息
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance != null) {
            FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowCode());
            vo.setFlowName(flow != null ? flow.getFlowName() : "");
            vo.setInstanceName(instance.getInstanceName());

            SysUser applicant = sysUserService.getById(instance.getApplicantId());
            vo.setApplicantName(applicant != null ? applicant.getUsername() : "");
        }

        return vo;
    }

    @Override
    public List<FlowTaskVO> getFlowNodes(String instanceId) {
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getInstanceId, instanceId)
                .eq(FlowTask::getDeleted, 0)
                .orderByAsc(FlowTask::getCreateTime);

        List<FlowTask> taskList = flowTaskMapper.selectList(wrapper);

        List<FlowTaskVO> voList = new ArrayList<>();
        for (FlowTask task : taskList) {
            FlowTaskVO vo = new FlowTaskVO();
            BeanUtils.copyProperties(task, vo);
            vo.setCurrentNodeName(task.getNodeName());
            vo.setCreateTime(task.getCreateTime());

            // 补充处理人名称
            if (task.getHandlerId() != null) {
                SysUser handler = sysUserService.getById(task.getHandlerId());
                vo.setHandlerName(handler != null ? handler.getUsername() : "");
            }

            voList.add(vo);
        }

        return voList;
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

        return detailVO;
    }

    // ==================== 自定义参数 ====================

    @Override
    public Map<String, Object> getFlowParams(String instanceId) {
        List<FlowInstanceParam> params = Arrays.asList(flowInstanceParamMapper.findByInstanceId(instanceId));

        Map<String, Object> result = new HashMap<>();
        for (FlowInstanceParam param : params) {
            result.put(param.getParamCode(), param.getParamValue());
        }

        return result;
    }

    @Override
    public Object getFlowParamValue(String instanceId, String paramCode) {
        FlowInstanceParam param = flowInstanceParamMapper.findByInstanceIdAndParamCode(instanceId, paramCode);
        return param != null ? param.getParamValue() : null;
    }

    @Override
    public void setFlowParamValue(String instanceId, String paramCode, Object paramValue) {
        FlowInstanceParam param = flowInstanceParamMapper.findByInstanceIdAndParamCode(instanceId, paramCode);

        if (param == null) {
            param = new FlowInstanceParam();
            param.setInstanceParamId(UUID.randomUUID().toString().replace("-", ""));
            param.setInstanceId(instanceId);
            String definitionParamId = resolveDefinitionParamId(instanceId, paramCode);
            if (definitionParamId != null) {
                param.setDefinitionParamId(definitionParamId);
            }
            param.setParamCode(paramCode);
            param.setParamValue(paramValue != null ? paramValue.toString() : null);
            param.setCreateTime(new Date());
            flowInstanceParamMapper.insert(param);
        } else {
            param.setParamValue(paramValue != null ? paramValue.toString() : null);
            flowInstanceParamMapper.updateById(param);
        }
    }

    @Override
    public void setFlowParams(String instanceId, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            setFlowParamValue(instanceId, entry.getKey(), entry.getValue());
        }
    }

    private String resolveDefinitionParamId(String instanceId, String paramCode) {
        if (!StringUtils.hasText(instanceId) || !StringUtils.hasText(paramCode)) {
            return null;
        }
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null || !StringUtils.hasText(instance.getFlowCode())) {
            return null;
        }
        List<FlowTemplateParam> definitionParams = flowTemplateParamMapper.findByTemplateId(instance.getFlowCode());
        if (definitionParams == null || definitionParams.isEmpty()) {
            return null;
        }
        for (FlowTemplateParam definitionParam : definitionParams) {
            if (paramCode.equals(definitionParam.getParamCode())) {
                return definitionParam.getDefinitionParamId();
            }
        }
        return null;
    }

    // ==================== 流程操作 ====================

    @Override
    public void approveFlow(String instanceId, Long userId, String comment) {
        // 获取当前待处理任务
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getInstanceId, instanceId)
                .eq(FlowTask::getStatus, 0)
                .eq(FlowTask::getHandlerId, userId)
                .eq(FlowTask::getDeleted, 0)
                .orderByDesc(FlowTask::getCreateTime)
                .last("LIMIT 1");

        FlowTask task = flowTaskMapper.selectOne(wrapper);
        if (task == null) {
            throw new RuntimeException("没有找到待处理的任务");
        }

        approveTask(task.getTaskId(), "approve", comment, userId);
    }

    @Override
    public void rejectFlow(String instanceId, Long userId, String comment) {
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getInstanceId, instanceId)
                .eq(FlowTask::getStatus, 0)
                .eq(FlowTask::getHandlerId, userId)
                .eq(FlowTask::getDeleted, 0)
                .orderByDesc(FlowTask::getCreateTime)
                .last("LIMIT 1");

        FlowTask task = flowTaskMapper.selectOne(wrapper);
        if (task == null) {
            throw new RuntimeException("没有找到待处理的任务");
        }

        approveTask(task.getTaskId(), "reject", comment, userId);
    }

    @Override
    public void approveTask(String taskId, String action, String comment, Long userId) {
        flowInstanceService.approveFlow(taskId, action, comment, userId);
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
    public String startFlow(String flowCode, String instanceName, Long userId, String tenantCode) {
        return startFlow(flowCode, instanceName, userId, tenantCode, null);
    }

    @Override
    public String startFlow(String flowCode, String instanceName, Long userId, String tenantCode, Map<String, Object> params) {
        FlowStartDTO dto = new FlowStartDTO();
        dto.setFlowCode(flowCode);
        dto.setInstanceName(instanceName);
        dto.setTenantCode(tenantCode);
        dto.setParams(params);

        // 这里需要调用 FlowInstanceService 的 startFlow 方法
        // 为了避免循环依赖，这里通过 Controller 来调用
        // 或者可以注入 FlowInstanceServiceImpl
        return flowInstanceService.startFlow(dto, userId);
    }

    // ==================== 流程状态查询 ====================

    @Override
    public boolean isRunning(String instanceId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        return instance != null && FlowInstanceStatus.RUNNING.getCode().equals(instance.getStatus());
    }

    @Override
    public boolean isCompleted(String instanceId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        return instance != null && FlowInstanceStatus.COMPLETED.getCode().equals(instance.getStatus());
    }

    @Override
    public Integer getFlowStatus(String instanceId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        return instance != null ? instance.getStatus() : null;
    }

    @Override
    public String getFlowStatusName(String instanceId) {
        FlowInstance instance = flowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return null;
        }
        FlowInstanceStatus status = FlowInstanceStatus.fromCode(instance.getStatus());
        return status != null ? status.getName() : null;
    }

    // ==================== 待办任务 ====================

    @Override
    public IPage<FlowTaskVO> getPendingTasks(Long userId, String moduleCode, String flowCode, String tenantCode,
                                               Integer pageNum, Integer pageSize) {
        return getTasks(userId, 0, moduleCode, flowCode, tenantCode, pageNum, pageSize);
    }

    @Override
    public IPage<FlowTaskVO> getProcessedTasks(Long userId, String moduleCode, String flowCode, String tenantCode,
                                                  Integer pageNum, Integer pageSize) {
        return getTasks(userId, 1, moduleCode, flowCode, tenantCode, pageNum, pageSize);
    }

    private IPage<FlowTaskVO> getTasks(Long userId, Integer taskStatus, String moduleCode, String flowCode,
                                         String tenantCode, Integer pageNum, Integer pageSize) {
        Page<FlowTask> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getHandlerId, userId)
                .eq(FlowTask::getStatus, taskStatus)
                .eq(FlowTask::getDeleted, 0)
                .orderByDesc(FlowTask::getCreateTime);

        IPage<FlowTask> taskPage = flowTaskMapper.selectPage(page, wrapper);
        List<FlowTask> taskList = taskPage.getRecords();

        // 过滤
        if (StringUtils.hasText(moduleCode) || (tenantCode != null && !tenantCode.trim().isEmpty()) || flowCode != null) {
            taskList = taskList.stream()
                    .filter(task -> {
                        FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
                        if (instance == null) {
                            return false;
                        }
                        if (tenantCode != null && !tenantCode.trim().isEmpty() && !tenantCode.equals(instance.getTenantCode())) {
                            return false;
                        }
                        if (StringUtils.hasText(moduleCode) && !moduleCode.equals(instance.getModuleCode())) {
                            return false;
                        }
                        if (flowCode != null && !flowCode.equals(instance.getFlowCode())) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        List<FlowTaskVO> voList = convertToTaskVOList(taskList);

        Page<FlowTaskVO> resultPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), voList.size());
        resultPage.setRecords(voList);
        resultPage.setTotal(taskPage.getTotal());

        return resultPage;
    }

    private List<FlowTaskVO> convertToTaskVOList(List<FlowTask> taskList) {
        List<FlowTaskVO> voList = new ArrayList<>();
        for (FlowTask task : taskList) {
            FlowTaskVO vo = new FlowTaskVO();
            BeanUtils.copyProperties(task, vo);
            vo.setCurrentNodeName(task.getNodeName());
            vo.setCreateTime(task.getCreateTime());

            FlowInstance instance = flowInstanceMapper.selectById(task.getInstanceId());
            if (instance != null) {
                FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowCode());
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
    public Long getPendingTaskCount(Long userId) {
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getHandlerId, userId)
                .eq(FlowTask::getStatus, 0)
                .eq(FlowTask::getDeleted, 0);

        return flowTaskMapper.selectCount(wrapper);
    }

    // ==================== 流程定义查询 ====================

    @Override
    public List<FlowDefinition> getFlowsByModule(String moduleCode) {
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<FlowDefinition>()
                .eq(FlowDefinition::getStatus, 1)
                .orderByDesc(FlowDefinition::getCreateTime);

        return flowDefinitionMapper.selectList(wrapper);
    }

    @Override
    public FlowDefinition getFlowDefinition(String flowCode) {
        return flowDefinitionMapper.selectById(flowCode);
    }

    // ==================== 流程日志 ====================

    @Override
    public List<FlowOperationLog> getFlowLogs(String instanceId) {
        LambdaQueryWrapper<FlowOperationLog> wrapper = new LambdaQueryWrapper<FlowOperationLog>()
                .eq(FlowOperationLog::getInstanceId, instanceId)
                .orderByAsc(FlowOperationLog::getOperationTime);

        return flowOperationLogMapper.selectList(wrapper);
    }

    @Override
    public void saveFlowLog(String instanceId, Long userId, String operationType, String operationDesc) {
        logService.saveLog(instanceId, userId, sysUserService.getById(userId).getUsername(),
                operationType, operationDesc);
    }

    @Override
    public FlowQueryResultVO<?> queryFlow(FlowQueryDTO dto) {
        return flowInstanceService.queryFlow(dto);
    }
}



