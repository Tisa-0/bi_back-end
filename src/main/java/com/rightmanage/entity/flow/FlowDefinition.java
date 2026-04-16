package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义实体（增强版）
 */
@Data
@TableName("flow_definition")
public class FlowDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "flow_code", type = IdType.INPUT)
    private String flowCode;

    private String flowName;

    private String flowJson;

    private String startRoleIds;

    private Integer status;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;

    // 非数据库字段，用于返回前端是否需要租户
    @TableField(exist = false)
    private Boolean needTenant;

    // 是否允许主动发起（1允许，0不允许）
    private Integer canInitiate;

    // 是否需要上传凭证（1需要，0不需要）
    private Integer needAttachment;

}
