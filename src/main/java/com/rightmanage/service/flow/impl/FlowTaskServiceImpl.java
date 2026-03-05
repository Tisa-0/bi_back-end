package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rightmanage.entity.*;
import com.rightmanage.mapper.flow.FlowTaskMapper;
import com.rightmanage.mapper.flow.FlowInstanceMapper;
import com.rightmanage.mapper.flow.FlowLogMapper;
import com.rightmanage.service.flow.FlowTaskService;
import com.rightmanage.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskMapper, FlowTask> implements FlowTaskService {

    @Autowired
    private FlowInstanceMapper flowInstanceMapper;
    
    @Autowired
    private FlowLogMapper flowLogMapper;
    
    @Autowired
    private SysUserService sysUserService;

    @Override
    public List<FlowTask> getTodoTasks(Long userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getAssigneeId, userId)
                .eq(FlowTask::getStatus, "PENDING")
                .orderByDesc(FlowTask::getCreateTime));
    }

    @Override
    public List<FlowTask> getDoneTasks(Long userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getAssigneeId, userId)
                .in(FlowTask::getStatus, "APPROVED", "REJECTED", "DELEGATED")
                .orderByDesc(FlowTask::getCompleteTime));
    }

    @Override
    public List<FlowTask> getAllTasks(Integer pageNum, Integer pageSize) {
        Page<FlowTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FlowTask::getCreateTime);
        return baseMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    @Transactional
    public void approve(FlowApproveDTO dto, Long currentUserId) {
        FlowTask task = baseMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务已处理");
        }
        
        // 获取当前用户
        SysUser currentUser = sysUserService.getById(currentUserId);
        
        // 更新任务状态
        task.setStatus("APPROVED".equals(dto.getAction()) ? "APPROVED" : "REJECTED");
        task.setComment(dto.getComment());
        task.setCompleteTime(new Date());
        baseMapper.updateById(task);
        
        // 更新流程实例
        FlowInstance instance = flowInstanceMapper.selectById(task.getProcessInstanceId());
        if (instance != null) {
            if ("REJECTED".equals(dto.getAction())) {
                // 驳回则流程结束
                instance.setStatus("REJECTED");
                instance.setEndTime(new Date());
                instance.setCurrentNodeIds("");
                instance.setCurrentHandlerIds("");
            } else {
                // 通过则继续流转（简化处理：直接到结束）
                instance.setStatus("COMPLETED");
                instance.setEndTime(new Date());
                instance.setCurrentNodeIds("");
                instance.setCurrentHandlerIds("");
            }
            flowInstanceMapper.updateById(instance);
        }
        
        // 记录日志
        FlowLog log = new FlowLog();
        log.setProcessInstanceId(task.getProcessInstanceId());
        log.setNodeId(task.getNodeId());
        log.setNodeName(task.getNodeName());
        log.setAction("APPROVED".equals(dto.getAction()) ? "APPROVED" : "REJECTED");
        log.setOperatorId(currentUserId);
        log.setOperatorName(currentUser != null ? currentUser.getUsername() : "");
        log.setComment(dto.getComment());
        log.setCreateTime(new Date());
        flowLogMapper.insert(log);
    }

    @Override
    @Transactional
    public void delegate(Long taskId, Long delegateUserId, Long currentUserId) {
        FlowTask task = baseMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("任务已处理");
        }
        
        // 获取被转办人
        SysUser delegateUser = sysUserService.getById(delegateUserId);
        
        // 更新任务处理人
        task.setAssigneeId(delegateUserId);
        task.setAssigneeName(delegateUser != null ? delegateUser.getUsername() : "");
        baseMapper.updateById(task);
        
        // 记录日志
        SysUser currentUser = sysUserService.getById(currentUserId);
        FlowLog log = new FlowLog();
        log.setProcessInstanceId(task.getProcessInstanceId());
        log.setNodeId(task.getNodeId());
        log.setNodeName(task.getNodeName());
        log.setAction("DELEGATED");
        log.setOperatorId(currentUserId);
        log.setOperatorName(currentUser != null ? currentUser.getUsername() : "");
        log.setComment("转办给: " + (delegateUser != null ? delegateUser.getUsername() : ""));
        log.setCreateTime(new Date());
        flowLogMapper.insert(log);
    }

    @Override
    public FlowTaskDTO getTaskDetail(Long taskId) {
        FlowTask task = baseMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        
        FlowTaskDTO dto = new FlowTaskDTO();
        BeanUtils.copyProperties(task, dto);
        
        return dto;
    }
}
