package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysRole implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roleName;

    @TableId(value = "role_code", type = IdType.INPUT)
    private String roleCode;

    private String description;

    private String moduleCode;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("org_related")
    private Boolean orgRelated;
}
