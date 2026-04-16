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
        wrapper.eq(SysTenant::getTenantEnableSwitch, "1")
                .orderByAsc(SysTenant::getDisplayOrder)
                .orderByAsc(SysTenant::getTenantCode);
        return sysTenantMapper.selectList(wrapper);
    }

    @Override
    public SysTenant getByTenantCode(String tenantCode) {
        return sysTenantMapper.selectById(tenantCode);
    }

    @Override
    public boolean save(SysTenant tenant) {
        return sysTenantMapper.insert(tenant) > 0;
    }

    @Override
    public boolean updateByTenantCode(SysTenant tenant) {
        return sysTenantMapper.updateById(tenant) > 0;
    }

    @Override
    public boolean deleteByTenantCode(String tenantCode) {
        return sysTenantMapper.deleteById(tenantCode) > 0;
    }

    @Override
    public IPage<SysTenant> page(String moduleCode, String tenantName, Integer pageNum, Integer pageSize) {
        Page<SysTenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (tenantName != null && !tenantName.isEmpty()) {
            wrapper.like(SysTenant::getTenantName, tenantName);
        }
        wrapper.orderByAsc(SysTenant::getDisplayOrder)
                .orderByAsc(SysTenant::getTenantCode);
        return sysTenantMapper.selectPage(page, wrapper);
    }
}
