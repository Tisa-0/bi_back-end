package com.rightmanage.entity.flow;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 流程节点DTO（用于流程设计器）
 */
@Data
public class FlowNodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String type;
    private String assigneeType;
    private String assigneeId;
    private String assigneeName;
    private Map<String, Object> properties;
    private Double positionX;
    private Double positionY;
}
