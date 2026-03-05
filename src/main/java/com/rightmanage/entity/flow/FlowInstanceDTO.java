package com.rightmanage.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * 流程实例DTO
 */
@Data
public class FlowInstanceDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String instanceKey;
    private String instanceName;
    private Long flowDefinitionId;
    private String flowName;
    private String status;
    private String currentNodeIds;
    private String currentNodeNames;
    private Long applicantId;
    private String applicantName;
    private String currentHandlerIds;
    private String currentHandlerNames;
    private String variables;
    private Long tenantId;
    private String createTime;
    private String updateTime;
    private String endTime;
}
