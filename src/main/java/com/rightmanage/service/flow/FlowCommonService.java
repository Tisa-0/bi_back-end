package com.rightmanage.service.flow;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.entity.flow.*;

import java.util.List;
import java.util.Map;

/**
 * 流程通用服务接口
 * 提供流程管理的通用方法，供其他模块调用
 */
public interface FlowCommonService {

    // ==================== 审批记录查询 ====================

    /**
     * 获取指定模块、指定流程类型、指定租户下的所有审批记录（分页）
     * @param moduleCode 模块编码（如：A、B、C）
     * @param flowId 流程定义ID（可选）
     * @param tenantCode 租户编码（可选）
     * @param status 流程状态（可选，0运行中/1已完成/2已驳回/3已撤销/4已终止）
     * @param applicantId 申请人ID（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的审批记录
     */
    IPage<FlowInstanceVO> getApprovalRecords(String moduleCode, String flowCode, String tenantCode, 
                                             Integer status, Long applicantId, Integer pageNum, Integer pageSize);

    /**
     * 获取指定模块、指定流程类型、指定租户下的所有审批记录（不分页）
     * @param moduleCode 模块编码
     * @param flowId 流程定义ID（可选）
     * @param tenantCode 租户编码（可选）
     * @param status 流程状态（可选）
     * @return 审批记录列表
     */
    List<FlowInstanceVO> getApprovalRecords(String moduleCode, String flowCode, String tenantCode, Integer status);

    /**
     * 获取用户发起的所有流程实例
     * @param userId 用户ID
     * @param moduleCode 模块编码（可选）
     * @param status 流程状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的流程实例
     */
    IPage<FlowInstanceVO> getUserInitiatedFlows(Long userId, String moduleCode, Integer status, 
                                                  Integer pageNum, Integer pageSize);

    // ==================== 审批记录详情 ====================

    /**
     * 获取审批记录当前节点信息
     * @param instanceId 流程实例ID
     * @return 当前节点信息
     */
    FlowTaskVO getCurrentNode(String instanceId);

    /**
     * 获取审批流程的所有节点信息
     * @param instanceId 流程实例ID
     * @return 节点列表
     */
    List<FlowTaskVO> getFlowNodes(String instanceId);

    /**
     * 获取审批流程详情
     * @param instanceId 流程实例ID
     * @return 流程详情
     */
    FlowDetailVO getFlowDetail(String instanceId);

    // ==================== 自定义参数 ====================

    /**
     * 获取审批流程中的自定义参数值
     * @param instanceId 流程实例ID
     * @return 自定义参数字典
     */
    Map<String, Object> getFlowParams(String instanceId);

    /**
     * 获取审批流程中的指定参数值
     * @param instanceId 流程实例ID
     * @param paramCode 参数编码
     * @return 参数值
     */
    Object getFlowParamValue(String instanceId, String paramCode);

    /**
     * 设置审批流程的自定义参数值
     * @param instanceId 流程实例ID
     * @param paramCode 参数编码
     * @param paramValue 参数值
     */
    void setFlowParamValue(String instanceId, String paramCode, Object paramValue);

    /**
     * 批量设置审批流程的自定义参数值
     * @param instanceId 流程实例ID
     * @param params 参数字典
     */
    void setFlowParams(String instanceId, Map<String, Object> params);

    // ==================== 流程操作 ====================

    /**
     * 推进流程到下一步（审批通过）
     * @param instanceId 流程实例ID
     * @param userId 处理人ID
     * @param comment 审批意见
     */
    void approveFlow(String instanceId, Long userId, String comment);

    /**
     * 推进流程到下一步（审批驳回）
     * @param instanceId 流程实例ID
     * @param userId 处理人ID
     * @param comment 驳回意见
     */
    void rejectFlow(String instanceId, Long userId, String comment);

    /**
     * 审批任务（通过或驳回）
     * @param taskId 任务ID
     * @param action 操作类型（approve/reject）
     * @param comment 审批意见
     * @param userId 处理人ID
     */
    void approveTask(String taskId, String action, String comment, Long userId);

    /**
     * 撤销流程
     * @param instanceId 流程实例ID
     * @param userId 操作人ID
     */
    void cancelFlow(String instanceId, Long userId);

    /**
     * 终止流程（管理员操作）
     * @param instanceId 流程实例ID
     * @param userId 操作人ID
     */
    void terminateFlow(String instanceId, Long userId);

    /**
     * 发起新流程
     * @param flowId 流程定义ID
     * @param instanceName 实例名称
     * @param userId 申请人ID
     * @param tenantCode 租户编码（可选）
     * @return 流程实例ID
     */
    String startFlow(String flowCode, String instanceName, Long userId, String tenantCode);

    /**
     * 发起新流程（带自定义参数）
     * @param flowId 流程定义ID
     * @param instanceName 实例名称
     * @param userId 申请人ID
     * @param tenantCode 租户编码（可选）
     * @param params 自定义参数
     * @return 流程实例ID
     */
    String startFlow(String flowCode, String instanceName, Long userId, String tenantCode, Map<String, Object> params);

    // ==================== 流程状态查询 ====================

    /**
     * 判断流程是否在运行中
     * @param instanceId 流程实例ID
     * @return 是否运行中
     */
    boolean isRunning(String instanceId);

    /**
     * 判断流程是否已完成
     * @param instanceId 流程实例ID
     * @return 是否已完成
     */
    boolean isCompleted(String instanceId);

    /**
     * 获取流程当前状态
     * @param instanceId 流程实例ID
     * @return 状态码
     */
    Integer getFlowStatus(String instanceId);

    /**
     * 获取流程状态名称
     * @param instanceId 流程实例ID
     * @return 状态名称
     */
    String getFlowStatusName(String instanceId);

    // ==================== 待办任务 ====================

    /**
     * 获取用户的待办任务列表（分页）
     * @param userId 用户ID
     * @param moduleCode 模块编码（可选）
     * @param flowId 流程定义ID（可选）
     * @param tenantCode 租户编码（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的待办任务
     */
    IPage<FlowTaskVO> getPendingTasks(Long userId, String moduleCode, String flowCode, String tenantCode, 
                                        Integer pageNum, Integer pageSize);

    /**
     * 获取用户已处理的任务列表（分页）
     * @param userId 用户ID
     * @param moduleCode 模块编码（可选）
     * @param flowId 流程定义ID（可选）
     * @param tenantCode 租户编码（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的已处理任务
     */
    IPage<FlowTaskVO> getProcessedTasks(Long userId, String moduleCode, String flowCode, String tenantCode, 
                                          Integer pageNum, Integer pageSize);

    /**
     * 获取用户的待办任务数量
     * @param userId 用户ID
     * @return 待办任务数量
     */
    Long getPendingTaskCount(Long userId);

    // ==================== 流程定义查询 ====================

    /**
     * 获取指定模块的流程定义列表
     * @param moduleCode 模块编码
     * @return 流程定义列表
     */
    List<FlowDefinition> getFlowsByModule(String moduleCode);

    /**
     * 获取流程定义详情
     * @param flowId 流程定义ID
     * @return 流程定义
     */
    FlowDefinition getFlowDefinition(String flowCode);

    // ==================== 流程日志 ====================

    /**
     * 获取流程操作日志
     * @param instanceId 流程实例ID
     * @return 日志列表
     */
    List<FlowOperationLog> getFlowLogs(String instanceId);

    /**
     * 记录流程操作日志
     * @param instanceId 流程实例ID
     * @param userId 操作人ID
     * @param operationType 操作类型
     * @param operationDesc 操作描述
     */
    void saveFlowLog(String instanceId, Long userId, String operationType, String operationDesc);

    /**
     * 【新增】统一查询接口
     * 根据 queryType 分发到待办任务/我的审批/我的流转
     * @param dto 查询参数
     * @return 统一查询结果
     */
    FlowQueryResultVO<?> queryFlow(FlowQueryDTO dto);
}
