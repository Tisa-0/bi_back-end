package com.rightmanage.service;

import com.rightmanage.entity.SysTenant;
import java.util.List;

public interface SysTenantService {
    List<SysTenant> listByModuleCode(String moduleCode);
    SysTenant getById(Long id);
    SysTenant getByTenantCode(String tenantCode);
    boolean save(SysTenant tenant);
    boolean updateById(SysTenant tenant);
    boolean deleteById(Long id);
}
