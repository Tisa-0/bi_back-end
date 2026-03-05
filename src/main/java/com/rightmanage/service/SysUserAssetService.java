package com.rightmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.entity.SysUserAsset;
import java.util.List;

public interface SysUserAssetService {
    
    /**
     * 分页查询用户资产列表
     */
    IPage<SysUserAsset> page(Integer pageNum, Integer pageSize, Long userId, String moduleCode);
    
    /**
     * 查询用户所有资产（不分页）
     */
    List<SysUserAsset> list(Long userId, String moduleCode);
    
    /**
     * 绑定资产给用户
     */
    boolean bindAsset(Long userId, Long assetId, String moduleCode);
    
    /**
     * 批量绑定资产给用户
     */
    boolean bindAssets(Long userId, List<Long> assetIds, String moduleCode);
    
    /**
     * 解除用户资产绑定
     */
    boolean unbindAsset(Long id);
    
    /**
     * 解除用户所有资产绑定
     */
    boolean unbindAllAssets(Long userId, String moduleCode);
    
    /**
     * 查询用户已绑定的资产ID列表
     */
    List<Long> getBoundAssetIds(Long userId, String moduleCode);
}
