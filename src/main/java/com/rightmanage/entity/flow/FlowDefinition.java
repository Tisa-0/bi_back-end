package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义实体
 * 存储流程模板信息，关联主项目角色控制发起权限
 */
@Data
public class FlowDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String flowKey;

    private String flowName;

    private String flowCategory;

    private String description;

    private Integer version;

    private String status;

    // 流程设计JSON（节点和连线）
    private String nodesJson;

    private String edgesJson;

    // 发起权限控制（关联主项目sys_role.id，多个用逗号分隔）
    private String starterRoleIds;

    // 表单配置
    private String formType;

    private String formConfig;

    // 关联主项目用户（创建者）
    private Long creatorId;

    // 租户
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
