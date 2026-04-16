package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.Date;

/**
 * 流程实例VO
 */
@Data
public class FlowInstanceVO {
    private String instanceId;
    private String flowCode;
    private String moduleCode;
    private String assetTypeId;
    private String flowName;
    private String instanceName;
    private Long applicantId;
    private String applicantName;
    private String currentNodeKey;
    private String currentNodeName;
    private Integer status;
    private String statusName;
    private Date createTime;
    private Date updateTime;
    private String executeLog; // 外部模块执行日志
    private String enableNotify; // 当前节点是否开启通知（"1"是，"0"否）
    private String notifyType;   // 通知方式
    private String assetTypeName; // 资产类型名称
    private String typeCode;       // 资产类型编码
    private Boolean canCancel;    // 是否可撤回（没有任何节点审批通过）
    private String currentNodeId;    // 当前节点数据库主键（FlowNodeConfig.node_id，用于前端获取节点详情）
}
