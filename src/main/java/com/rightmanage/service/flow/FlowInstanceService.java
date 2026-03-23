package com.rightmanage.service.flow;

import com.rightmanage.entity.flow.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.Map;

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
     * 获取我的流转（发起的流程）- 分页
     */
    IPage<FlowInstanceVO> myInitiated(Long userId, String moduleCode, Long tenantId, Long flowId, Integer pageNum, Integer pageSize);

    /**
     * 获取我的审批（待处理/已处理任务）- 分页
     */
    IPage<FlowTaskVO> myApproval(Long userId, Integer taskStatus, String moduleCode, Long tenantId, Long flowId, Integer pageNum, Integer pageSize);

    /**
     * 获取流程详情
     */
    FlowDetailVO getFlowDetail(Long instanceId);

    /**
     * 获取所有流程实例列表
     */
    List<FlowInstance> list();

    /**
     * 【新增】处理外部模块回调
     * @param callbackToken 回调令牌
     * @param success 是否成功
     * @param message 回调消息
     * @param extraData 额外数据
     * @return 处理结果
     */
    Map<String, Object> handleModuleCallback(String callbackToken, boolean success, String message, String extraData);

    /**
     * 【新增】重置任务状态（允许重新审批）
     * @param taskId 任务ID
     * @param userId 操作人ID
     * @param reason 重置原因
     */
    void resetTaskStatus(Long taskId, Long userId, String reason);

    /**
     * 【新增】获取任务状态文本
     * @param status 状态码
     * @return 状态说明文本
     */
    String getTaskStatusText(Integer status);

    /**
     * 【新增】更新外部模块执行日志
     * @param callbackToken 回调令牌
     * @param logContent 日志内容（将追加到现有日志后面）
     * @return 处理结果
     */
    Map<String, Object> updateExecuteLog(String callbackToken, String logContent);

    /**
     * 【新增】主动触发流程节点通知
     * @param instanceId 流程实例ID
     * @param userId 操作人ID
     * @return 通知结果描述
     */
    String triggerNodeNotify(Long instanceId, Long userId);
}
