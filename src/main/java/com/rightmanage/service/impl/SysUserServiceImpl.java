package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.mapper.SysUserMapper;
import com.rightmanage.mapper.SysUserRoleMapper;
import com.rightmanage.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class SysUserServiceImpl implements SysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Override
    public List<SysUser> list() {
        return sysUserMapper.selectList(null);
    }
    @Override
    public IPage<SysUser> page(Integer pageNum, Integer pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectPage(page, wrapper);
    }
    @Override
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }
    @Override
    public boolean save(SysUser user) {
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return sysUserMapper.insert(user) > 0;
    }
    @Override
    public boolean updateById(SysUser user) {
        // 如果密码有值则加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return sysUserMapper.updateById(user) > 0;
    }
    @Override
    public boolean deleteById(Long id) {
        return sysUserMapper.deleteById(id) > 0;
    }
    @Override
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id)
               .set(SysUser::getStatus, status);
        return sysUserMapper.update(null, wrapper) > 0;
    }
    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId, String moduleCode, Long tenantId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysUserRole::getModuleCode, moduleCode);
        }
        if (tenantId != null) {
            wrapper.eq(SysUserRole::getTenantId, tenantId);
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindRoles(Long userId, List<Long> roleIds, String moduleCode, Long tenantId) {
        // 删除原有角色（如果指定了moduleCode，则只删除该模块的）
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysUserRole::getModuleCode, moduleCode);
            if (tenantId != null) {
                deleteWrapper.eq(SysUserRole::getTenantId, tenantId);
            }
        }
        sysUserRoleMapper.delete(deleteWrapper);
        // 添加新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setModuleCode(moduleCode);
                userRole.setTenantId(tenantId);
                sysUserRoleMapper.insert(userRole);
            }
        }
        return true;
    }
}
