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
}
