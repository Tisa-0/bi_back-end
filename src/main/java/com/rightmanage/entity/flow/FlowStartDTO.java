package com.rightmanage.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * 发起流程DTO
 */
@Data
public class FlowStartDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long flowDefinitionId;
    private String instanceName;
    private String variables;
}
