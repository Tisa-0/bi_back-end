package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysTenant;
import com.rightmanage.mapper.SysTenantMapper;
import com.rightmanage.service.SysTenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysTenantServiceImpl implements SysTenantService {

    @Autowired
    private SysTenantMapper sysTenantMapper;

    @Override
    public List<SysTenant> listByModuleCode(String moduleCode) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysTenant::getModuleCode, moduleCode);
        }
        wrapper.eq(SysTenant::getStatus, 1);
        wrapper.orderByAsc(SysTenant::getId);
        return sysTenantMapper.selectList(wrapper);
    }

    @Override
    public SysTenant getById(Long id) {
        return sysTenantMapper.selectById(id);
    }

    @Override
    public SysTenant getByTenantCode(String tenantCode) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTenant::getTenantCode, tenantCode);
        return sysTenantMapper.selectOne(wrapper);
    }

    @Override
    public boolean save(SysTenant tenant) {
        return sysTenantMapper.insert(tenant) > 0;
    }

    @Override
    public boolean updateById(SysTenant tenant) {
        return sysTenantMapper.updateById(tenant) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return sysTenantMapper.deleteById(id) > 0;
    }

    @Override
    public IPage<SysTenant> page(String moduleCode, String tenantName, Integer pageNum, Integer pageSize) {
        Page<SysTenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysTenant::getModuleCode, moduleCode);
        }
        if (tenantName != null && !tenantName.isEmpty()) {
            wrapper.like(SysTenant::getTenantName, tenantName);
        }
        wrapper.orderByDesc(SysTenant::getId);
        return sysTenantMapper.selectPage(page, wrapper);
    }
}
