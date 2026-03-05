package com.rightmanage.service;

import com.rightmanage.dto.BindResultVO;
import com.rightmanage.entity.SysRole;
import com.rightmanage.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface SysRoleService {

    IPage<SysRole> pageByModuleCode(Integer pageNum, Integer pageSize, String moduleCode, Long tenantId);

    List<SysRole> listByModuleCode(String moduleCode);

    List<SysRole> listByModuleCodeAndTenantId(String moduleCode, Long tenantId);

    SysRole getById(Long id);

    boolean save(SysRole role);

    boolean updateById(SysRole role);

    boolean deleteById(Long id);

    List<SysRole> listByIds(List<Long> ids);

    List<Long> getMenuIdsByRoleId(Long roleId, String moduleCode);

    boolean bindMenus(Long roleId, List<Long> menuIds, String moduleCode);

    List<Long> getApiIdsByRoleId(Long roleId, String moduleCode);

    boolean bindApis(Long roleId, List<Long> apiIds, String moduleCode);

    List<Long> getUserIdsByRoleId(Long roleId);

    boolean bindUsers(Long roleId, List<Long> userIds);

    List<SysUser> getRoleUsers(Long roleId);

    List<SysUser> getOptionalUsers(Long roleId, String keyword, Integer status, Integer pageNum, Integer pageSize);

    BindResultVO bindUsersBatch(Long roleId, List<Long> userIds, String moduleCode);

    BindResultVO unbindUsers(Long roleId, List<Long> userIds);

    boolean clearRoleUsers(Long roleId);
}
