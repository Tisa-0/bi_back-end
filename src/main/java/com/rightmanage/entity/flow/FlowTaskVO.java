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
    private String nodeType;
    private Long handlerId;
    private String handlerName;
    private Long applicantId;
    private String applicantName;
    private String action;
    private String comment;
    private Date executeTime;
    private Integer status;
}
