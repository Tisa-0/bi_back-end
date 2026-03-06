package com.rightmanage.entity.flow;

import lombok.Data;

/**
 * 流程发起DTO
 */
@Data
public class FlowStartDTO {
    private Long flowId;
    private String instanceName;
}
