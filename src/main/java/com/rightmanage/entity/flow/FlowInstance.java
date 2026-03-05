package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例实体
 * 存储流程运行实例
 */
@Data
public class FlowInstance implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String instanceKey;

    private String instanceName;

    private Long flowDefinitionId;

    private String flowName;

    // 流程状态
    private String status;

    private String currentNodeIds;

    private String currentNodeNames;

    // 申请人（关联主项目sys_user）
    private Long applicantId;

    private String applicantName;

    // 当前处理人（关联主项目sys_user）
    private String currentHandlerIds;

    private String currentHandlerNames;

    // 流程变量
    private String variables;

    // 租户
    private Long tenantId;

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private Date endTime;

    @TableLogic
    private Integer deleted;
}
