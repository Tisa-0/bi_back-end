package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rightmanage.entity.AssetType;
import com.rightmanage.mapper.AssetTypeMapper;
import com.rightmanage.service.AssetTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssetTypeServiceImpl implements AssetTypeService {

    @Autowired
    private AssetTypeMapper assetTypeMapper;

    @Override
    public List<AssetType> listAll() {
        return assetTypeMapper.selectList(new LambdaQueryWrapper<AssetType>()
                .orderByAsc(AssetType::getSort)
                .orderByAsc(AssetType::getId));
    }

    @Override
    public List<AssetType> listByModuleCode(String moduleCode) {
        LambdaQueryWrapper<AssetType> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(AssetType::getModuleCode, moduleCode);
        }
        return assetTypeMapper.selectList(wrapper
                .orderByAsc(AssetType::getSort)
                .orderByAsc(AssetType::getId));
    }

    @Override
    public AssetType getById(Long id) {
        return assetTypeMapper.selectById(id);
    }

    @Override
    @Transactional
    public void save(AssetType assetType) {
        if (assetType.getSort() == null) {
            assetType.setSort(0);
        }
        if (assetType.getStatus() == null) {
            assetType.setStatus(1);
        }
        assetTypeMapper.insert(assetType);
    }

    @Override
    @Transactional
    public void update(AssetType assetType) {
        assetTypeMapper.updateById(assetType);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        assetTypeMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<AssetType> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AssetType::getId, id)
                .set(AssetType::getStatus, status);
        assetTypeMapper.update(null, wrapper);
    }

    @Override
    public boolean checkCodeUnique(String typeCode, Long excludeId) {
        LambdaQueryWrapper<AssetType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetType::getTypeCode, typeCode);
        if (excludeId != null) {
            wrapper.ne(AssetType::getId, excludeId);
        }
        return assetTypeMapper.selectCount(wrapper) == 0;
    }
}
