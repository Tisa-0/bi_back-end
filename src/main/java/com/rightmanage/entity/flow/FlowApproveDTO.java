package com.rightmanage.entity.flow;

import lombok.Data;

/**
 * 流程审批DTO
 */
@Data
public class FlowApproveDTO {
    private Long taskId;
    private String action;
    private String comment;
    private Long userId;
    /**
     * 驳回节点的编码（驳回时传递给各业务模块，用于通知）
     */
    private String rejectNodeKey;
    /**
     * 驳回节点的名称（驳回时传递给各业务模块，用于通知）
     */
    private String rejectNodeName;
}
