package com.rightmanage.entity.flow;

import lombok.Data;
import java.io.Serializable;

/**
 * 流程任务DTO
 */
@Data
public class FlowTaskDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskId;
    private String taskKey;
    private Long processInstanceId;
    private Long processDefinitionId;
    private String processTitle;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Long assigneeId;
    private String assigneeName;
    private Long initiatorId;
    private String initiatorName;
    private String status;
    private String comment;
    private String createTime;
    private String completeTime;
    private String dueTime;
}
