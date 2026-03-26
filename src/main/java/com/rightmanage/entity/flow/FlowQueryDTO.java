package com.rightmanage.entity.flow;

import lombok.Data;

/**
 * 流程统一查询请求参数
 */
@Data
public class FlowQueryDTO {

    /**
     * 查询类型：pending | myApproval | myInitiated
     */
    private String queryType;

    /**
     * 当前登录用户ID（必填）
     */
    private Long userId;

    /**
     * 任务状态（0=待处理 1=已处理 3=业务执行中 4=逻辑处理失败）
     * 用于 pending 和 myApproval
     */
    private Integer taskStatus;

    /**
     * 模块编码（可选）
     */
    private String moduleCode;

    /**
     * 租户编码（可选）
     */
    private String tenantCode;

    /**
     * 流程定义ID（可选，与 flowCode 二选一）
     */
    private Long flowId;

    /**
     * 流程定义编码（可选，与 flowId 二选一，优先级高于 flowId）
     */
    private String flowCode;

    /**
     * 资产类型编码（可选）
     */
    private String typeCode;

    /**
     * 审批节点Key（可选，用于筛选流程卡在哪一步）
     * 对应 FlowInstance.currentNodeKey 或 FlowTask.nodeKey
     */
    private String nodeKey;

    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10）
     */
    private Integer pageSize = 10;
}
