package com.rightmanage.service;

import com.rightmanage.entity.AssetType;

import java.util.List;

/**
 * 资产类型 Service 接口
 */
public interface AssetTypeService {

    /**
     * 获取所有（不分页）
     */
    List<AssetType> listAll();

    /**
     * 按模块编码获取所有
     */
    List<AssetType> listByModuleCode(String moduleCode);

    /**
     * 根据编码查询详情
     */
    AssetType getByTypeCode(String typeCode);

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
    void delete(String typeCode);

    /**
     * 切换状态
     */
    void updateStatus(String typeCode, Integer status);

    /**
     * 检查编码是否已存在（排除自身）
     */
    boolean checkCodeUnique(String typeCode, String excludeTypeCode);
}
