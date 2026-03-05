package com.rightmanage.service.flow;

import com.rightmanage.entity.FlowTask;
import com.rightmanage.entity.FlowTaskDTO;
import com.rightmanage.entity.FlowApproveDTO;
import java.util.List;

public interface FlowTaskService {
    // 获取待办任务
    List<FlowTask> getTodoTasks(Long userId);
    
    // 获取已办任务
    List<FlowTask> getDoneTasks(Long userId);
    
    // 审批任务
    void approve(FlowApproveDTO dto, Long currentUserId);
    
    // 转办任务
    void delegate(Long taskId, Long delegateUserId, Long currentUserId);
    
    // 获取任务详情
    FlowTaskDTO getTaskDetail(Long taskId);
    
    // 获取所有任务（带分页）
    List<FlowTask> getAllTasks(Integer pageNum, Integer pageSize);
}
