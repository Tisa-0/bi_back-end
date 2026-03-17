package com.rightmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysTenant;
import java.util.List;

public interface SysTenantService {
    List<SysTenant> listByModuleCode(String moduleCode);
    SysTenant getById(Long id);
    SysTenant getByTenantCode(String tenantCode);
    boolean save(SysTenant tenant);
    boolean updateById(SysTenant tenant);
    boolean deleteById(Long id);
    IPage<SysTenant> page(String moduleCode, String tenantName, Integer pageNum, Integer pageSize);
}
