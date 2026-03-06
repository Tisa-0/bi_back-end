package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 流程详情VO（前端可视化专用）
 */
@Data
public class FlowDetailVO {
    private Long instanceId;
    private String flowName;
    private String instanceName;
    private String applicantName;
    private String statusName;
    private Date createTime;
    private List<FlowNodeDetailVO> nodeList;
    private List<FlowOperationLog> logList;
}
