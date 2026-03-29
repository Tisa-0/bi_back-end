package com.rightmanage.service.flow;

import com.rightmanage.entity.flow.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.Map;
import com.rightmanage.entity.SysUser;

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
    IPage<FlowInstanceVO> myInitiated(Long userId, String moduleCode, String tenantCode, Long flowId, String typeCode, String currentNodeKey, Integer pageNum, Integer pageSize);

    /**
     * 获取我的审批（待处理/已处理任务）- 分页
     */
    IPage<FlowTaskVO> myApproval(Long userId, Integer taskStatus, String moduleCode, String tenantCode, Long flowId, String typeCode, String nodeKey, Integer pageNum, Integer pageSize);

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

    /**
     * 【新增】获取角色+动态用户（role_dynamic_user）节点的候选用户列表
     * @param moduleCode 模块编码
     * @param roleIds 逗号分隔的角色ID
     * @param tenantId 租户ID（可为null）
     * @param sourceOrgId 发起机构ID（可为null）
     * @return 符合条件的用户列表（包含 id, username）
     */
    List<Map<String, Object>> getRoleDynamicUsers(String moduleCode, String roleIds, Long tenantId, Long sourceOrgId);

    /**
     * 【新增】统一查询接口
     * @param dto 查询参数（包含 queryType 决定查询类型）
     * @return 统一查询结果
     */
    FlowQueryResultVO<?> queryFlow(FlowQueryDTO dto);

    /**
     * 【管理员】查询所有在途流程任务（不分用户权限限制）
     * @param taskStatus 任务状态（0=待处理 1=已处理 2=已驳回 3=业务执行中 4=逻辑处理失败 5=已跳过）
     * @param moduleCode 模块编码（可选）
     * @param tenantCode 租户编码（可选）
     * @param flowId 流程定义ID（可选）
     * @param flowCode 流程定义编码（可选，与flowId二选一）
     * @param typeCode 资产类型编码（可选）
     * @param nodeKey 审批节点Key（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页任务列表
     */
    IPage<FlowTaskVO> adminAllTasks(Integer taskStatus, String moduleCode, String tenantCode,
                                     Long flowId, String flowCode, String typeCode, String nodeKey,
                                     Integer pageNum, Integer pageSize);

    /**
     * 【新增】回退流程到上一步（强制回退，支持多次回退）
     * 无论当前流程处于什么状态，都强制将流程回退到前一步，且前一步为"待处理"状态
     * 可多次回退，直到第一个审批节点为待处理状态
     * @param instanceId 流程实例ID
     * @param userId 操作人ID（管理员）
     * @return 回退结果描述（成功或"已回退至流程初始状态"）
     */
    String rollbackFlow(Long instanceId, Long userId);

    /**
     * 【新增】驳回时通知业务执行模块
     * 当流程被驳回时，通知当前审批节点配置的executeModules，告知流程已被驳回及被驳回的节点信息
     * @param instanceId 流程实例ID
     * @param flowId 流程定义ID
     * @param nodeKey 被驳回的节点key
     * @param nodeName 被驳回的节点名称
     * @param operator 操作人信息
     * @param comment 驳回意见
     */
    void notifyModulesOnReject(Long instanceId, Long flowId, String nodeKey, String nodeName, SysUser operator, String comment);
}
