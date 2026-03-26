package com.rightmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.entity.SysAsset;
import java.util.List;

public interface SysAssetService {
    
    /**
     * 分页查询资产列表
     */
    IPage<SysAsset> page(Integer pageNum, Integer pageSize, String moduleCode, String keyword);
    
    /**
     * 查询指定模块下的可用资产（未被当前用户绑定的）
     */
    List<SysAsset> listAvailableAssets(String moduleCode, Long typeId, Long tenantId, Long userId);
    
    /**
     * 根据ID查询资产
     */
    SysAsset getById(Long id);
    
    /**
     * 新增资产
     */
    boolean save(SysAsset asset);
    
    /**
     * 修改资产
     */
    boolean updateById(SysAsset asset);
    
    /**
     * 删除资产
     */
    boolean deleteById(Long id);
    
    /**
     * 校验资产编码是否唯一（模块+租户下唯一）
     */
    boolean isAssetCodeUnique(String moduleCode, String assetCode, Long excludeId);

    /**
     * 分页查询指定模块+资产类型下已分配的资产
     * @param pageNum     页码
     * @param pageSize    每页条数
     * @param moduleCode  模块编码
     * @param typeId      资产类型ID
     * @param tenantId    租户ID（可为null）
     * @return IPage<SysAsset>
     */
    IPage<SysAsset> pageAllocatedAssets(Integer pageNum, Integer pageSize,
            String moduleCode, Long typeId, Long tenantId);

    /**
     * 新增分配资产（按模块+资产类型，可指定租户）
     */
    boolean saveAllocatedAsset(SysAsset asset);

    /**
     * 删除已分配的资产
     */
    boolean deleteAllocatedAsset(Long id);
}
