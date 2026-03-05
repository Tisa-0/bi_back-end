package com.rightmanage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.mapper.SysUserMapper;
import com.rightmanage.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

public interface SysUserService {

    List<SysUser> list();

    IPage<SysUser> page(Integer pageNum, Integer pageSize);

    SysUser getById(Long id);

    boolean save(SysUser user);

    boolean updateById(SysUser user);

    boolean deleteById(Long id);

    boolean updateStatus(Long id, Integer status);

    List<Long> getRoleIdsByUserId(Long userId);

    List<Long> getRoleIdsByUserId(Long userId, String moduleCode, Long tenantId);

    boolean bindRoles(Long userId, List<Long> roleIds, String moduleCode, Long tenantId);
}
