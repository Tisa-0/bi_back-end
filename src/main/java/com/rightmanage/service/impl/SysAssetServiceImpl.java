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
                    .or().like(SysAsset::getAssetType, keyword));
        }
        
        wrapper.orderByDesc(SysAsset::getCreateTime);
        return sysAssetMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysAsset> listAvailableAssets(String moduleCode, Long userId) {
        // 查询指定模块下的所有可用资产
        LambdaQueryWrapper<SysAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAsset::getModuleCode, moduleCode)
               .eq(SysAsset::getStatus, 1)
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
}
