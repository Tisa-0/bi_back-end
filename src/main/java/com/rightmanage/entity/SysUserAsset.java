package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysUserAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long assetId;

    private String moduleCode;

    /** 关联资产类型ID（冗余存储，方便按类型过滤） */
    private Long typeId;

    /** 所属租户ID（冗余存储，方便按租户隔离查询） */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    // 关联的资产信息（非数据库字段，用于前端展示）
    @TableField(exist = false)
    private String assetName;

    @TableField(exist = false)
    private String assetCode;

    @TableField(exist = false)
    private String assetDesc;

    @TableField(exist = false)
    private Integer assetStatus;
}
