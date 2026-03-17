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
    
    // 凭证信息
    private String attachmentUrl; // 凭证文件URL
    private String attachmentName; // 凭证文件名

    // 额外信息
    private String extraInfo; // 额外信息

    // 流程参数（自定义参数）
    private List<FlowInstanceParamVO> flowParams;
}
