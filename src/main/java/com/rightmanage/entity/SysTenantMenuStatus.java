package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysTenantMenuStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 租户编码
     */
    @TableField("tenant_code")
    private String tenantCode;

    /**
     * 关联sys_menu的菜单ID
     */
    @TableField("menu_id")
    private String menuId;

    /**
     * 菜单状态（1启用0禁用，仅对该租户生效）
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
