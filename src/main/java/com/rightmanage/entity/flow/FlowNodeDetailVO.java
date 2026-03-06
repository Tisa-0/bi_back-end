package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.Date;

/**
 * 节点详情VO
 */
@Data
public class FlowNodeDetailVO {
    private String nodeKey;
    private String nodeName;
    private String nodeType;
    private String handlerNames;
    private String action;
    private String comment;
    private Date executeTime;
    private String status;
}
