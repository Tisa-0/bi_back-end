package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysModule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "module_code", type = IdType.INPUT)
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

    /**
     * 模块回调地址（用于流程驳回通知等跨模块 HTTP 调用）
     * 例如：http://192.168.1.100:8080/api/flow
     */
    private String moduleUrl;
}
