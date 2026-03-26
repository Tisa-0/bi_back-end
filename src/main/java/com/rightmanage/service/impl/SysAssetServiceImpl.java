package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysAsset;
import com.rightmanage.mapper.SysAssetMapper;
import com.rightmanage.service.SysAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysAssetServiceImpl implements SysAssetService {

    @Autowired
    private SysAssetMapper sysAssetMapper;

    @Override
    public IPage<SysAsset> page(Integer pageNum, Integer pageSize, String moduleCode, String keyword) {
        Page<SysAsset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysAsset> wrapper = new LambdaQueryWrapper<>();
        
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysAsset::getModuleCode, moduleCode);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysAsset::getAssetName, keyword)
                    .or().like(SysAsset::getAssetCode, keyword)
                    .or().like(SysAsset::getAssetDesc, keyword));
        }
        
        wrapper.orderByDesc(SysAsset::getCreateTime);
        return sysAssetMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysAsset> listAvailableAssets(String moduleCode, Long typeId, Long tenantId, Long userId) {
        LambdaQueryWrapper<SysAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAsset::getModuleCode, moduleCode)
               .eq(SysAsset::getStatus, 1)
               .eq(typeId != null, SysAsset::getTypeId, typeId)
               .eq(tenantId != null, SysAsset::getTenantId, tenantId)
               .orderByAsc(SysAsset::getAssetCode);

        return sysAssetMapper.selectList(wrapper);
    }

    @Override
    public SysAsset getById(Long id) {
        return sysAssetMapper.selectById(id);
    }

    @Override
    public boolean save(SysAsset asset) {
        return sysAssetMapper.insert(asset) > 0;
    }

    @Override
    public boolean updateById(SysAsset asset) {
        return sysAssetMapper.updateById(asset) > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return sysAssetMapper.deleteById(id) > 0;
    }

    @Override
    public boolean isAssetCodeUnique(String moduleCode, String assetCode, Long excludeId) {
        LambdaQueryWrapper<SysAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAsset::getModuleCode, moduleCode)
               .eq(SysAsset::getAssetCode, assetCode);
        
        if (excludeId != null) {
            wrapper.ne(SysAsset::getId, excludeId);
        }
        
        return sysAssetMapper.selectCount(wrapper) == 0;
    }

    @Override
    public IPage<SysAsset> pageAllocatedAssets(Integer pageNum, Integer pageSize,
            String moduleCode, Long typeId, Long tenantId) {
        Page<SysAsset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysAsset> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(moduleCode != null, SysAsset::getModuleCode, moduleCode);
        wrapper.eq(typeId != null, SysAsset::getTypeId, typeId);
        wrapper.eq(tenantId != null, SysAsset::getTenantId, tenantId);
        
        wrapper.orderByDesc(SysAsset::getCreateTime);
        return sysAssetMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean saveAllocatedAsset(SysAsset asset) {
        if (asset.getStatus() == null) {
            asset.setStatus(1);
        }
        return sysAssetMapper.insert(asset) > 0;
    }

    @Override
    public boolean deleteAllocatedAsset(Long id) {
        return sysAssetMapper.deleteById(id) > 0;
    }
}
