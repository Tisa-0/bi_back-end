package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.Date;

/**
 * 流程任务VO
 */
@Data
public class FlowTaskVO {
    private String taskId;
    private String instanceId;
    private String flowCode;  // 流程定义编码（用于前端查询流程详情）
    private String moduleCode; // 发起时选择的模块编码
    private String assetTypeId; // 发起时选择的资产类型编码
    private String flowName;
    private String instanceName;
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
    private String tenantCode; // 多租户审批节点的任务归属租户编码
    private String tenantName; // 多租户审批节点的任务归属租户名称
    private String sourceOrgId; // 发起机构ID（orgRelated节点有效）
    private String sourceOrgName; // 发起机构名称（前端展示用）
    private String assetTypeName; // 资产类型名称
    private String typeCode;       // 资产类型编码
}
