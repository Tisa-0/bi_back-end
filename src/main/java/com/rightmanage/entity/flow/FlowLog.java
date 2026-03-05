package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程日志实体
 * 记录流程操作历史
 */
@Data
public class FlowLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long processInstanceId;

    private String nodeId;

    private String nodeName;

    private String action;

    // 操作人（关联主项目sys_user）
    private Long operatorId;

    private String operatorName;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
