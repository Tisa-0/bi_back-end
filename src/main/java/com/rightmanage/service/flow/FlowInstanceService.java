package com.rightmanage.service.flow;

import com.rightmanage.entity.FlowInstance;
import com.rightmanage.entity.FlowInstanceDTO;
import com.rightmanage.entity.FlowStartDTO;
import java.util.List;

public interface FlowInstanceService {
    // 流程实例列表
    List<FlowInstance> list();
    
    List<FlowInstance> getMyInstances(Long userId);
    
    FlowInstance getById(Long id);
    
    // 发起流程
    Long start(FlowStartDTO dto, Long currentUserId);
    
    // 撤回流程
    void cancel(Long instanceId, Long currentUserId);
    
    // 获取流程实例详情（含日志）
    FlowInstanceDTO getDetail(Long instanceId);
    
    // 获取流程实例列表（带分页）
    List<FlowInstance> getAllInstances(Integer pageNum, Integer pageSize);
}
