package com.rightmanage.service;

import com.rightmanage.dto.AssetTypeTreeVO;
import com.rightmanage.entity.AssetType;

import java.util.List;

/**
 * 资产类型 Service 接口
 */
public interface AssetTypeService {

    /**
     * 获取所有（不分页，用于树形表格展示）
     */
    List<AssetType> listAll();

    /**
     * 按模块编码获取所有
     */
    List<AssetType> listByModuleCode(String moduleCode);

    /**
     * 获取树形结构（全局）
     */
    List<AssetTypeTreeVO> getTree();

    /**
     * 按模块编码获取树形结构
     */
    List<AssetTypeTreeVO> getTreeByModuleCode(String moduleCode);

    /**
     * 获取下拉树选项（平铺，带children）
     */
    List<AssetTypeTreeVO> getTreeOptions(String moduleCode);

    /**
     * 根据ID查询详情
     */
    AssetType getById(Long id);

    /**
     * 新增
     */
    void save(AssetType assetType);

    /**
     * 更新
     */
    void update(AssetType assetType);

    /**
     * 删除（逻辑删除）
     */
    void delete(Long id);

    /**
     * 级联删除（含所有子节点）
     */
    void deleteCascade(Long id);

    /**
     * 切换状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 检查编码是否已存在（排除自身）
     */
    boolean checkCodeUnique(String typeCode, Long excludeId);
}
