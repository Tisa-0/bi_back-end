package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.Date;

/**
 * 流程实例VO
 */
@Data
public class FlowInstanceVO {
    private Long id;
    private Long flowId;
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
}
