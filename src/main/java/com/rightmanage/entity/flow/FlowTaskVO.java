package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.Date;

/**
 * 流程任务VO
 */
@Data
public class FlowTaskVO {
    private Long id;
    private Long instanceId;
    private String instanceName;
    private String flowName;
    private String nodeKey;
    private String nodeName;
    private String currentNodeName; // 当前节点名称（前端显示用）
    private String nodeType;
    private Long handlerId;
    private String handlerName;
    private Long applicantId;
    private String applicantName;
    private String action;
    private String comment;
    private Date executeTime;
    private Date createTime; // 任务创建时间（到达时间）
    private Integer status;
    private String executeLog; // 外部模块执行日志
    private Long tenantId; // 多租户审批节点的任务归属租户ID
    private String tenantName; // 多租户审批节点的任务归属租户名称
    private Long sourceOrgId; // 发起机构ID（orgRelated节点有效）
    private String sourceOrgName; // 发起机构名称（前端展示用）
}
