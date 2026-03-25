package com.rightmanage.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 资产类型树节点 VO
 */
@Data
public class AssetTypeTreeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String typeName;

    private String typeCode;

    private Integer sort;

    private Integer status;

    private String remark;

    /**
     * 所属模块编码
     */
    private String moduleCode;

    private List<AssetTypeTreeVO> children;
}
