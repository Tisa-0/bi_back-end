package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysAsset;
import com.rightmanage.entity.SysUserAsset;
import com.rightmanage.mapper.SysAssetMapper;
import com.rightmanage.mapper.SysUserAssetMapper;
import com.rightmanage.service.SysUserAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysUserAssetServiceImpl implements SysUserAssetService {

    @Autowired
    private SysUserAssetMapper sysUserAssetMapper;

    @Autowired
    private SysAssetMapper sysAssetMapper;

    @Override
    public IPage<SysUserAsset> page(Integer pageNum, Integer pageSize, Long userId, String moduleCode, Long typeId, Long tenantId) {
        Page<SysUserAsset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUserAsset> wrapper = buildQueryWrapper(userId, moduleCode, typeId, tenantId);

        IPage<SysUserAsset> result = sysUserAssetMapper.selectPage(page, wrapper);

        // 填充关联的资产信息
        fillAssetInfo(result.getRecords());

        return result;
    }

    @Override
    public List<SysUserAsset> list(Long userId, String moduleCode, Long typeId, Long tenantId) {
        LambdaQueryWrapper<SysUserAsset> wrapper = buildQueryWrapper(userId, moduleCode, typeId, tenantId);
        List<SysUserAsset> list = sysUserAssetMapper.selectList(wrapper);

        // 填充关联的资产信息
        fillAssetInfo(list);

        return list;
    }

    private LambdaQueryWrapper<SysUserAsset> buildQueryWrapper(Long userId, String moduleCode, Long typeId, Long tenantId) {
        LambdaQueryWrapper<SysUserAsset> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(SysUserAsset::getUserId, userId);
        }
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysUserAsset::getModuleCode, moduleCode);
        }
        if (typeId != null) {
            wrapper.eq(SysUserAsset::getTypeId, typeId);
        }
        if (tenantId != null) {
            wrapper.eq(SysUserAsset::getTenantId, tenantId);
        }

        wrapper.orderByDesc(SysUserAsset::getCreateTime);
        return wrapper;
    }

    private void fillAssetInfo(List<SysUserAsset> userAssets) {
        if (userAssets == null || userAssets.isEmpty()) {
            return;
        }
        
        // 收集所有资产ID
        List<Long> assetIds = userAssets.stream()
                .map(SysUserAsset::getAssetId)
                .collect(Collectors.toList());
        
        // 批量查询资产信息
        if (!assetIds.isEmpty()) {
            List<SysAsset> assets = sysAssetMapper.selectBatchIds(assetIds);
            
            // 构建资产ID到资产的映射
            java.util.Map<Long, SysAsset> assetMap = assets.stream()
                    .collect(Collectors.toMap(SysAsset::getId, a -> a));
            
            // 填充资产信息
            for (SysUserAsset userAsset : userAssets) {
                SysAsset asset = assetMap.get(userAsset.getAssetId());
                if (asset != null) {
                    userAsset.setAssetName(asset.getAssetName());
                    userAsset.setAssetCode(asset.getAssetCode());
                    userAsset.setTypeId(asset.getTypeId());
                    userAsset.setAssetDesc(asset.getAssetDesc());
                    userAsset.setAssetStatus(asset.getStatus());
                }
            }
        }
    }

    @Override
    @Transactional
    public boolean bindAsset(Long userId, Long assetId, String moduleCode) {
        // 检查是否已绑定
        LambdaQueryWrapper<SysUserAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserAsset::getUserId, userId)
               .eq(SysUserAsset::getAssetId, assetId);
        
        Long count = sysUserAssetMapper.selectCount(wrapper);
        if (count > 0) {
            return false; // 已绑定
        }
        
        SysUserAsset userAsset = new SysUserAsset();
        userAsset.setUserId(userId);
        userAsset.setAssetId(assetId);
        userAsset.setModuleCode(moduleCode);
        
        return sysUserAssetMapper.insert(userAsset) > 0;
    }

    @Override
    @Transactional
    public boolean bindAssets(Long userId, List<Long> assetIds, String moduleCode, Long typeId, Long tenantId) {
        if (assetIds == null || assetIds.isEmpty()) {
            return true;
        }

        List<SysUserAsset> userAssets = new ArrayList<>();

        for (Long assetId : assetIds) {
            // 检查是否已绑定
            LambdaQueryWrapper<SysUserAsset> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserAsset::getUserId, userId)
                   .eq(SysUserAsset::getAssetId, assetId);

            Long count = sysUserAssetMapper.selectCount(wrapper);
            if (count == 0) {
                SysUserAsset userAsset = new SysUserAsset();
                userAsset.setUserId(userId);
                userAsset.setAssetId(assetId);
                userAsset.setModuleCode(moduleCode);
                userAsset.setTypeId(typeId);
                userAsset.setTenantId(tenantId);
                userAssets.add(userAsset);
            }
        }

        if (!userAssets.isEmpty()) {
            for (SysUserAsset userAsset : userAssets) {
                sysUserAssetMapper.insert(userAsset);
            }
        }

        return true;
    }

    @Override
    @Transactional
    public boolean unbindAsset(Long id) {
        return sysUserAssetMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean unbindAllAssets(Long userId, String moduleCode) {
        LambdaQueryWrapper<SysUserAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserAsset::getUserId, userId);
        
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysUserAsset::getModuleCode, moduleCode);
        }
        
        return sysUserAssetMapper.delete(wrapper) > 0;
    }

    @Override
    public List<Long> getBoundAssetIds(Long userId, String moduleCode, Long typeId, Long tenantId) {
        LambdaQueryWrapper<SysUserAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserAsset::getUserId, userId);

        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysUserAsset::getModuleCode, moduleCode);
        }
        if (typeId != null) {
            wrapper.eq(SysUserAsset::getTypeId, typeId);
        }
        if (tenantId != null) {
            wrapper.eq(SysUserAsset::getTenantId, tenantId);
        }

        List<SysUserAsset> list = sysUserAssetMapper.selectList(wrapper);

        return list.stream()
                .map(SysUserAsset::getAssetId)
                .collect(Collectors.toList());
    }
}
