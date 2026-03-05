package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程节点权限实体
 * 存储每个节点的审批角色配置
 */
@Data
public class FlowNodePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long flowDefinitionId;

    private String nodeId;

    private String nodeName;

    private String nodeType;

    // 审批人类型（关联主项目角色/用户）
    private String assigneeType;

    // 处理人ID（角色时存sys_role.id，用户时存sys_user.id，多个逗号分隔）
    private String assigneeIds;

    // 审批配置
    private Integer allowDelegate;

    private Integer allowReject;

    private Integer dueHours;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
