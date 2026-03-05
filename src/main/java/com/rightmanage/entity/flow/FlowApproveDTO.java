package com.rightmanage.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * 审批任务DTO
 */
@Data
public class FlowApproveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String action; // APPROVE批准/REJECT拒绝/DELEGATE转办
    private String comment;
    private Long delegateUserId; // 转办时指定的用户ID
}
