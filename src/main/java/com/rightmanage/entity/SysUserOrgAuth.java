package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户授权机构（按模块、租户隔离）
 */
@Data
@TableName("sys_user_org_auth")
public class SysUserOrgAuth implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String moduleCode;

    @TableField("tenant_code")
    private String tenantCode;

    private String orgId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private Integer deleted;
}
