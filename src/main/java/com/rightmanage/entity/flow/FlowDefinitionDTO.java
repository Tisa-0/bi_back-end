package com.rightmanage.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 流程定义DTO（用于流程设计和返回）
 */
@Data
public class FlowDefinitionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String flowKey;
    private String flowName;
    private String flowCategory;
    private String description;
    private Integer version;
    private String status;
    private String nodesJson;
    private String edgesJson;
    private String starterRoleIds;
    private String formType;
    private String formConfig;
    private Long creatorId;
    private String creatorName;
    private Long tenantId;
    
    // 节点列表（解析nodesJson获得）
    private List<FlowNodeDTO> nodes;
    
    // 发起角色名称列表
    private List<String> starterRoleNames;
}
