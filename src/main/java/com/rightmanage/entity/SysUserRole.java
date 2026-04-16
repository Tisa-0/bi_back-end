package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    @TableField("role_code")
    private String roleCode;

    private String moduleCode;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
