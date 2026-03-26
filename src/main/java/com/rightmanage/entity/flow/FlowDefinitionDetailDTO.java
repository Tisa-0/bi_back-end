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
    private List<Object> lines; // 连线配置（用于排序节点）
    private Boolean needTenant; // 是否需要租户（判断是否包含产品智能定制角色）
    private Integer canInitiate; // 是否允许主动发起（1允许，0不允许）
    private Integer needAttachment; // 是否需要上传凭证（1需要，0不需要）
    private String moduleCode; // 流程所属模块（A/B/C）
    private Long assetTypeId; // 流程所属资产类型ID
}
