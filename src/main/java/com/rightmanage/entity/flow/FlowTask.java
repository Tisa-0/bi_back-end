package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务实体（增强版）
 */
@Data
@TableName("flow_task")
public class FlowTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private Long handlerId;

    private String handlerName;

    private String action;

    private String comment;

    private Date executeTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableLogic
    private Integer deleted;
}
