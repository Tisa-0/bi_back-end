package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bmip_sys_usrinf")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（bmip_sys_usrinf.usrid）
     *
     * 注：bmip_sys_usrinf 的 usrid 为 VARCHAR。当前系统仍以 Long 作为用户ID类型，
     * 约定 usrid 存储可解析为 Long 的数字字符串（如 "1"、"2"）。
     */
    @TableId(value = "usrid", type = IdType.INPUT)
    private Long id;

    @TableField("staffid")
    private String staffId;

    @TableField("usrnam")
    private String username;

    @TableField("usrpwd")
    private String password;

    /**
     * 状态（1=启用，0=禁用），映射到 bmip_sys_usrinf.vipflg
     */
    @TableField("vipflg")
    private Integer status;

    @TableField("dptcod")
    private String deptCode;

    @TableField("dptnam")
    private String deptName;

    @TableField("dty")
    private String duty;

    @TableField("tel")
    private String telephone;

    @TableField("email")
    private String email;

    @TableField("mbl")
    private String mobile;

    @TableField("sex")
    private String sex;

    @TableField("rac")
    private String roleAuth;

    @TableField("rmk")
    private String remark;

    @TableField("defaultsbjcod")
    private String defaultSubjectCode;

    /**
     * 生效开始时间（字符串形式），前端展示为 createTime
     */
    @TableField("validbegin")
    private String createTime;

    /**
     * 生效结束时间（字符串形式），前端展示为 updateTime（如需要）
     */
    @TableField("validend")
    private String updateTime;

    @TableField("defaultregion")
    private String defaultRegion;

    @TableField("ehrcod")
    private String ehrCode;

    @TableField("dptcodold")
    private String deptCodeOld;
}
