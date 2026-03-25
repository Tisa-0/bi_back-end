package com.rightmanage.entity.flow;

import lombok.Data;

/**
 * 动态用户处理人DTO
 * 用于 dynamic 和 role_dynamic_user 两种节点类型：
 * - dynamic：直接指定审批人（moduleCode 为 null）
 * - role_dynamic_user：从角色候选列表中选审批人（moduleCode 不为空）
 */
@Data
public class DynamicHandlerDTO {
    private String nodeKey; // 节点key
    private Long handlerId; // 处理人ID（dynamic/role_dynamic_user 选中的用户ID）
    private String handlerName; // 处理人名称
    private Long tenantId; // 节点专属的允许审批租户ID（非多租户节点为 null，单选）
    private Long sourceOrgId; // 发起机构ID（orgRelated 节点有效）
    private String moduleCode; // role_dynamic_user 节点：审批人模块编码（用于查询候选用户列表）
}
