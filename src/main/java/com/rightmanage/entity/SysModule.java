package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysModule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String moduleCode;

    private String moduleName;

    /**
     * 是否多租户模块（0=否，1=是）
     * 如果是多租户模块，则该模块下的角色管理等数据仅对当前租户生效
     */
    private Integer multiTenant;

    /**
     * 是否启用（0=禁用，1=启用）
     */
    private Integer status;

    /**
     * 模块描述
     */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
