package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务实体
 * 存储待办/已办任务
 */
@Data
public class FlowTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String taskKey;

    private Long processInstanceId;

    private Long processDefinitionId;

    private String processTitle;

    // 节点信息
    private String nodeId;

    private String nodeName;

    private String nodeType;

    // 处理人（关联主项目sys_user）
    private Long assigneeId;

    private String assigneeName;

    // 申请人
    private Long initiatorId;

    private String initiatorName;

    // 任务状态
    private String status;

    // 审批意见
    private String comment;

    // 租户
    private Long tenantId;

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Date completeTime;

    private Date dueTime;

    @TableLogic
    private Integer deleted;
}
