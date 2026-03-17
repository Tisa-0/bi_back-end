package com.rightmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.entity.SysUser;

import java.util.List;

public interface SysUserService {

    List<SysUser> list();

    IPage<SysUser> page(Integer pageNum, Integer pageSize, String username);

    SysUser getById(Long id);

    boolean save(SysUser user);

    boolean updateById(SysUser user);

    boolean deleteById(Long id);

    boolean updateStatus(Long id, Integer status);

    List<Long> getRoleIdsByUserId(Long userId);

    List<Long> getRoleIdsByUserId(Long userId, String moduleCode, Long tenantId);

    boolean bindRoles(Long userId, List<Long> roleIds, String moduleCode, Long tenantId);
}
