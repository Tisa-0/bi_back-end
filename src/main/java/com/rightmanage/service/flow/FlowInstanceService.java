package com.rightmanage.service.flow;

import com.rightmanage.entity.flow.*;
import java.util.List;

public interface FlowInstanceService {
    /**
     * 发起流程
     */
    Long startFlow(FlowStartDTO dto, Long userId);

    /**
     * 审批流程任务
     */
    void approveFlow(Long taskId, String action, String comment, Long userId);

    /**
     * 撤销流程
     */
    void cancelFlow(Long instanceId, Long userId);

    /**
     * 终止流程（管理员）
     */
    void terminateFlow(Long instanceId, Long userId);

    /**
     * 获取我的流转（发起的流程）
     */
    List<FlowInstanceVO> myInitiated(Long userId, String moduleCode);

    /**
     * 获取我的审批（待处理/已处理任务）
     */
    List<FlowTaskVO> myApproval(Long userId, Integer taskStatus, String moduleCode);

    /**
     * 获取流程详情
     */
    FlowDetailVO getFlowDetail(Long instanceId);

    /**
     * 获取所有流程实例列表
     */
    List<FlowInstance> list();
}
