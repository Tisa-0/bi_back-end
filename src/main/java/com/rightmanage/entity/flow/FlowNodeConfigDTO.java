package com.rightmanage.entity.flow;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 流程节点运行时配置DTO（由前端发起流程时传入）
 */
@Data
public class FlowNodeConfigDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nodeKey;   // 节点唯一标识
    private String nodeId;   // 节点uuid
    private String nodeName;  // 节点名称
    private String handlerType; // 处理人类型：user/role/dynamic

    private Long handlerId;   // dynamic节点：用户ID
    private String handlerName; // dynamic节点：用户姓名

    // 多租户审批节点：允许审批的租户ID（仅handlerType=role且模块为多租户时有效，单选）
    private Long tenantId;

    // 机构相关审批：发起机构ID（orgRelated=1的role节点，发起流程时由用户选择）
    private Long sourceOrgId;
}
