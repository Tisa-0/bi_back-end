package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 流程发起DTO
 */
@Data
public class FlowStartDTO {
    private Long flowId;
    private String instanceName;
    private Long tenantId; // 租户ID（产品智能定制模块需要）
    private String attachmentUrl; // 凭证文件URL
    private String attachmentName; // 凭证文件名
    private Map<String, Object> params; // 流程参数
    private List<DynamicHandlerDTO> dynamicHandlers; // 动态用户处理人列表
    // 多租户审批节点租户选择：发起流程时选择的租户列表
    // 用于过滤多租户审批节点的审批人：仅在这些租户下拥有对应角色的用户才能审批
    private List<Long> nodeTenants;
}
