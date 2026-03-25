package com.rightmanage.entity.flow;

import lombok.Data;

/**
 * 动态用户处理人DTO
 */
@Data
public class DynamicHandlerDTO {
    private String nodeKey; // 节点key
    private Long handlerId; // 处理人ID
    private String handlerName; // 处理人名称
    private Long tenantId; // 节点专属的允许审批租户ID（非多租户节点为 null，单选）
    private Long sourceOrgId; // 发起机构ID（orgRelated 节点有效）
}
