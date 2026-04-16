package com.rightmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.entity.SysTenant;
import java.util.List;

public interface SysTenantService {
    List<SysTenant> listByModuleCode(String moduleCode);
    SysTenant getByTenantCode(String tenantCode);
    boolean save(SysTenant tenant);
    boolean updateByTenantCode(SysTenant tenant);
    boolean deleteByTenantCode(String tenantCode);
    IPage<SysTenant> page(String moduleCode, String tenantName, Integer pageNum, Integer pageSize);
}
