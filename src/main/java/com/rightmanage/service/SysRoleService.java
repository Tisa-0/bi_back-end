package com.rightmanage.service;

import com.rightmanage.dto.BindResultVO;
import com.rightmanage.entity.SysRole;
import com.rightmanage.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface SysRoleService {

    IPage<SysRole> pageByModuleCode(Integer pageNum, Integer pageSize, String moduleCode, String tenantCode);

    List<SysRole> listByModuleCode(String moduleCode);

    List<SysRole> listByModuleCodeAndTenantCode(String moduleCode, String tenantCode);

    SysRole getById(String roleCode);

    boolean save(SysRole role);

    boolean updateById(SysRole role);

    boolean deleteById(String roleCode);

    List<SysRole> listByIds(List<String> roleCodes);

    List<String> getMenuIdsByRoleId(String roleCode, String moduleCode, String tenantCode);

    boolean bindMenus(String roleCode, List<String> menuIds, String moduleCode, String tenantCode);

    List<Long> getApiIdsByRoleId(String roleCode, String moduleCode);

    boolean bindApis(String roleCode, List<Long> apiIds, String moduleCode);

    List<Long> getUserIdsByRoleId(String roleCode, String moduleCode, String tenantCode);

    boolean bindUsers(String roleCode, List<Long> userIds, String moduleCode, String tenantCode);

    List<SysUser> getRoleUsers(String roleCode, String moduleCode, String tenantCode);

    List<SysUser> getOptionalUsers(String roleCode, String keyword, Integer status, Integer pageNum, Integer pageSize, String tenantCode);

    BindResultVO bindUsersBatch(String roleCode, List<Long> userIds, String moduleCode, String tenantCode);

    BindResultVO unbindUsers(String roleCode, List<Long> userIds, String moduleCode, String tenantCode);

    boolean clearRoleUsers(String roleCode, String moduleCode, String tenantCode);
}
