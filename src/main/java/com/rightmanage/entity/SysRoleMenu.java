package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysRoleMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField("role_code")
    private String roleCode;

    private String menuId;

    private String moduleCode;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
