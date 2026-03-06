package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.List;

/**
 * 流程定义详情DTO（包含节点配置）
 */
@Data
public class FlowDefinitionDetailDTO {
    private Long id;
    private String flowName;
    private String flowCode;
    private String flowJson;
    private String startRoleIds;
    private Integer status;
    private Long creatorId;
    private List<FlowNodeConfig> nodes;
}
