package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "menu_id", type = IdType.INPUT)
    private String menuId;

    @TableField(exist = false)
    private String id;

    private String menuName;

    private String parentId;

    private String path;

    private String component;

    private String moduleCode;

    private Integer sort;

    private Integer status;

    private Integer horizontalMenu;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
