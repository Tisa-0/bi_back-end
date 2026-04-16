package com.rightmanage.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class SysMenuVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String menuId;

    private String id;

    private String menuName;

    private String parentId;

    private String path;

    private String component;

    private String moduleCode;

    private Integer sort;

    private Integer status;

    private Integer horizontalMenu;

    private Date createTime;

    private Date updateTime;

    private List<SysMenuVO> children;
}
