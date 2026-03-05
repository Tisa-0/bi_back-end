package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rightmanage.entity.*;
import com.rightmanage.mapper.flow.FlowInstanceMapper;
import com.rightmanage.mapper.flow.FlowLogMapper;
import com.rightmanage.mapper.flow.FlowTaskMapper;
import com.rightmanage.mapper.flow.FlowDefinitionMapper;
import com.rightmanage.service.flow.FlowInstanceService;
import com.rightmanage.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance> implements FlowInstanceService {

    @Autowired
    private FlowDefinitionMapper flowDefinitionMapper;
    
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    
    @Autowired
    private FlowLogMapper flowLogMapper;
    
    @Autowired
    private SysUserService sysUserService;

    @Override
    public List<FlowInstance> list() {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowInstance>()
                .orderByDesc(FlowInstance::getCreateTime));
    }

    @Override
    public List<FlowInstance> getAllInstances(Integer pageNum, Integer pageSize) {
        Page<FlowInstance> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FlowInstance::getCreateTime);
        return baseMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public List<FlowInstance> getMyInstances(Long userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getApplicantId, userId)
                .orderByDesc(FlowInstance::getCreateTime));
    }

    @Override
    public FlowInstance getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional
    public Long start(FlowStartDTO dto, Long currentUserId) {
        // 获取流程定义
        FlowDefinition definition = flowDefinitionMapper.selectById(dto.getFlowDefinitionId());
        if (definition == null) {
            throw new RuntimeException("流程定义不存在");
        }
        
        if (!"PUBLISHED".equals(definition.getStatus())) {
            throw new RuntimeException("流程未发布，无法发起");
        }
        
        // 获取当前用户
        SysUser currentUser = sysUserService.getById(currentUserId);
        
        // 创建流程实例
        FlowInstance instance = new FlowInstance();
        instance.setInstanceKey("PI_" + UUID.randomUUID().toString().replace("-", "").substring(16));
        instance.setInstanceName(dto.getInstanceName());
        instance.setFlowDefinitionId(dto.getFlowDefinitionId());
        instance.setFlowName(definition.getFlowName());
        instance.setStatus("RUNNING");
        
        // 解析节点JSON获取开始节点
        String startNodeId = "node_1";
        String startNodeName = "开始";
        
        instance.setCurrentNodeIds(startNodeId);
        instance.setCurrentNodeNames(startNodeName);
        
        // 设置申请人和处理人
        instance.setApplicantId(currentUserId);
        instance.setApplicantName(currentUser != null ? currentUser.getUsername() : "");
        
        // 查找开始节点的下一个审批节点
        List<FlowTask> nextTasks = createTasks(instance, definition, currentUser);
        
        if (!nextTasks.isEmpty()) {
            String handlerIds = nextTasks.stream()
                    .map(t -> t.getAssigneeId() != null ? t.getAssigneeId().toString() : "")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            String handlerNames = nextTasks.stream()
                    .map(FlowTask::getAssigneeName)
                    .filter(n -> n != null && !n.isEmpty())
                    .collect(Collectors.joining(","));
            instance.setCurrentHandlerIds(handlerIds);
            instance.setCurrentHandlerNames(handlerNames);
        }
        
        instance.setVariables(dto.getVariables());
        
        baseMapper.insert(instance);
        
        // 记录日志
        FlowLog log = new FlowLog();
        log.setProcessInstanceId(instance.getId());
        log.setNodeId(startNodeId);
        log.setNodeName(startNodeName);
        log.setAction("START");
        log.setOperatorId(currentUserId);
        log.setOperatorName(currentUser != null ? currentUser.getUsername() : "");
        log.setComment("发起流程");
        log.setCreateTime(new Date());
        flowLogMapper.insert(log);
        
        return instance.getId();
    }

    @Override
    @Transactional
    public void cancel(Long instanceId, Long currentUserId) {
        FlowInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }
        
        if (!"RUNNING".equals(instance.getStatus())) {
            throw new RuntimeException("只有运行中的流程才能撤回");
        }
        
        if (!instance.getApplicantId().equals(currentUserId)) {
            throw new RuntimeException("只有申请人才能撤回流程");
        }
        
        // 更新流程实例状态
        instance.setStatus("CANCELLED");
        instance.setEndTime(new Date());
        baseMapper.updateById(instance);
        
        // 删除相关任务
        flowTaskMapper.delete(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getProcessInstanceId, instanceId));
        
        // 记录日志
        SysUser currentUser = sysUserService.getById(currentUserId);
        FlowLog log = new FlowLog();
        log.setProcessInstanceId(instanceId);
        log.setAction("CANCELLED");
        log.setOperatorId(currentUserId);
        log.setOperatorName(currentUser != null ? currentUser.getUsername() : "");
        log.setComment("撤回流程");
        log.setCreateTime(new Date());
        flowLogMapper.insert(log);
    }

    @Override
    public FlowInstanceDTO getDetail(Long instanceId) {
        FlowInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            return null;
        }
        
        FlowInstanceDTO dto = new FlowInstanceDTO();
        BeanUtils.copyProperties(instance, dto);
        
        // 获取流程日志
        List<FlowLog> logs = flowLogMapper.selectList(new LambdaQueryWrapper<FlowLog>()
                .eq(FlowLog::getProcessInstanceId, instanceId)
                .orderByAsc(FlowLog::getCreateTime));
        
        return dto;
    }
    
    /**
     * 为流程实例创建任务
     */
    private List<FlowTask> createTasks(FlowInstance instance, FlowDefinition definition, SysUser currentUser) {
        // 这里简化处理，实际需要解析nodesJson获取审批节点
        // 返回空列表表示自动通过到结束
        return new ArrayList<>();
    }
}
