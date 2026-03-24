package com.rightmanage.service;

import com.rightmanage.dto.BankOrgTreeVO;
import com.rightmanage.entity.BankOrg;

import java.util.List;

public interface BankOrgService {

    /**
     * 获取完整机构树（根节点为总行）
     */
    List<BankOrgTreeVO> getTree();

    /**
     * 获取指定机构完整路径（总行 -> ... -> 当前）
     */
    List<BankOrg> getPath(Long id);

    /**
     * 获取父机构（无父则返回 null）
     */
    BankOrg getParent(Long id);

    /**
     * 获取直接子机构
     */
    List<BankOrg> getChildren(Long id);

    /**
     * 获取全部后代机构（包含多级）
     */
    List<BankOrg> getDescendants(Long id);

    /**
     * 获取机构平铺列表
     */
    List<BankOrg> getAll();
}
