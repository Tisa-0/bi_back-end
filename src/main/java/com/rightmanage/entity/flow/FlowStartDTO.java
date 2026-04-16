package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 流程发起DTO
 */
@Data
public class FlowStartDTO {
    private String flowCode;
    private String instanceName;
    private String tenantCode; // 租户编码（产品智能定制模块需要）
    private String attachmentUrl; // 凭证文件URL
    private String attachmentName; // 凭证文件名
    private Map<String, Object> params; // 流程参数

    /**
     * 节点配置列表（运行时配置）
     * - dynamic 节点：handlerId + handlerName
     * - role 多租户节点：tenantCode（允许审批的租户，单选）
     * 其他节点可留空
     * 后端会自动从 nodeConfigs 汇总所有 tenantCodes 作为 nodeTenants，无需单独传参
     */
    private List<FlowNodeConfigDTO> nodeConfigs;

    /**
     * 全局节点租户列表（兼容旧逻辑，可不传）
     */
    private List<String> nodeTenants;
}
