package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属模块编码 */
    private String moduleCode;

    /** 关联资产类型ID（asset_type.id） */
    private Long typeId;

    /** 所属租户编码（多租户模块使用） */
    @TableField("tenant_code")
    private String tenantCode;

    /** 资产名称 */
    private String assetName;

    /** 资产编码 */
    private String assetCode;

    /** 资产描述 */
    private String assetDesc;

    /** 自定义参数（JSON格式，如 {"cpu":"8核","mem":"16G"}） */
    private String customParams;

    /** 状态（1启用 0禁用） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
