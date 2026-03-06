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
}
