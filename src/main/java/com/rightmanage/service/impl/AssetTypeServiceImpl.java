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
                .orderByAsc(AssetType::getTypeCode));
    }

    @Override
    public List<AssetType> listByModuleCode(String moduleCode) {
        LambdaQueryWrapper<AssetType> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(AssetType::getModuleCode, moduleCode);
        }
        return assetTypeMapper.selectList(wrapper
                .orderByAsc(AssetType::getSort)
                .orderByAsc(AssetType::getTypeCode));
    }

    @Override
    public AssetType getByTypeCode(String typeCode) {
        return assetTypeMapper.selectById(typeCode);
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
    public void delete(String typeCode) {
        assetTypeMapper.deleteById(typeCode);
    }

    @Override
    @Transactional
    public void updateStatus(String typeCode, Integer status) {
        LambdaUpdateWrapper<AssetType> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AssetType::getTypeCode, typeCode)
                .set(AssetType::getStatus, status);
        assetTypeMapper.update(null, wrapper);
    }

    @Override
    public boolean checkCodeUnique(String typeCode, String excludeTypeCode) {
        LambdaQueryWrapper<AssetType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetType::getTypeCode, typeCode);
        if (excludeTypeCode != null && !excludeTypeCode.isEmpty()) {
            wrapper.ne(AssetType::getTypeCode, excludeTypeCode);
        }
        return assetTypeMapper.selectCount(wrapper) == 0;
    }
}
