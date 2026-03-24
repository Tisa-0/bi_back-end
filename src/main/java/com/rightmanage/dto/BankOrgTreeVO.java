package com.rightmanage.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 银行机构树节点
 */
@Data
public class BankOrgTreeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private Integer level;
    private List<BankOrgTreeVO> children;
}
