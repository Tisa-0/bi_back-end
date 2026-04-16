package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 机构基础信息（bmip_sys_ttlorginf）
 */
@Data
@TableName("bmip_sys_ttlorginf")
public class BankOrg implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "orgcod")
    private String id;

    @TableField("suporgcod")
    private String parentId;

    @TableField("orgnam")
    private String name;

    @TableField("orgcod")
    private String code;

    @TableField("orglvl")
    private Integer level;

    @TableField("dspsqn")
    private Integer sort;

    private String strdte;

    private String enddte;
}
