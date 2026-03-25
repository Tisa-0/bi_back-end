package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rightmanage.dto.AssetTypeTreeVO;
import com.rightmanage.entity.AssetType;
import com.rightmanage.mapper.AssetTypeMapper;
import com.rightmanage.service.AssetTypeService;
import com.rightmanage.util.AssetTypeTreeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public List<AssetTypeTreeVO> getTree() {
        return AssetTypeTreeUtils.buildTree(listAll());
    }

    @Override
    public List<AssetTypeTreeVO> getTreeByModuleCode(String moduleCode) {
        return AssetTypeTreeUtils.buildTree(listByModuleCode(moduleCode));
    }

    @Override
    public List<AssetTypeTreeVO> getTreeOptions(String moduleCode) {
        List<AssetType> list = listByModuleCode(moduleCode);
        AssetType root = new AssetType();
        root.setId(0L);
        root.setParentId(0L);
        root.setTypeName("顶级类型");
        root.setTypeCode("TOP");
        root.setSort(0);
        list.add(0, root);
        return AssetTypeTreeUtils.buildTree(list);
    }

    @Override
    public AssetType getById(Long id) {
        return assetTypeMapper.selectById(id);
    }

    @Override
    @Transactional
    public void save(AssetType assetType) {
        if (assetType.getParentId() == null) {
            assetType.setParentId(0L);
        }
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
    @Transactional
    public void deleteCascade(Long id) {
        AssetType target = assetTypeMapper.selectById(id);
        if (target == null) return;
        List<AssetType> descendants = getDescendantsByModule(id, target.getModuleCode());
        for (AssetType item : descendants) {
            assetTypeMapper.deleteById(item.getId());
        }
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

    private List<AssetType> getDescendantsByModule(Long id, String moduleCode) {
        List<AssetType> all = listByModuleCode(moduleCode);
        Map<Long, List<AssetType>> childrenMap = AssetTypeTreeUtils.toChildrenMap(all);
        List<AssetType> result = AssetTypeTreeUtils.getDescendants(id, childrenMap);
        AssetType self = assetTypeMapper.selectById(id);
        if (self != null) {
            result.add(self);
        }
        return result;
    }
}
