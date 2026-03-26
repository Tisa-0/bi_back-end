package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 资产类型实体
 */
@Data
@TableName("asset_type")
public class AssetType implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 资产类型名称
     */
    private String typeName;

    /**
     * 资产类型编码（全局唯一）
     */
    private String typeCode;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态 1=启用 0=禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新人
     */
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标志 0=未删除 1=已删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 所属模块编码
     */
    private String moduleCode;
}
