package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.dto.BindResultVO;
import com.rightmanage.entity.*;
import com.rightmanage.mapper.*;
import com.rightmanage.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Autowired
    private SysRoleApiMapper sysRoleApiMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<SysRole> pageByModuleCode(Integer pageNum, Integer pageSize, String moduleCode, Long tenantId) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRole::getModuleCode, moduleCode);
        }
        // 模块C需要按租户筛选
        if (tenantId != null) {
            wrapper.eq(SysRole::getTenantId, tenantId);
        } else if (moduleCode != null && !moduleCode.equals("C")) {
            // 非模块C，tenantId必须为空
            wrapper.isNull(SysRole::getTenantId);
        }
        wrapper.orderByDesc(SysRole::getCreateTime);
        IPage<SysRole> result = sysRoleMapper.selectPage(page, wrapper);
        return result;
    }

    @Override
    public List<SysRole> listByModuleCode(String moduleCode) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRole::getModuleCode, moduleCode);
        }
        wrapper.isNull(SysRole::getTenantId);
        wrapper.orderByDesc(SysRole::getCreateTime);
        return sysRoleMapper.selectList(wrapper);
    }

    @Override
    public List<SysRole> listByModuleCodeAndTenantId(String moduleCode, Long tenantId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRole::getModuleCode, moduleCode);
        }
        if (tenantId != null) {
            wrapper.eq(SysRole::getTenantId, tenantId);
        }
        wrapper.orderByDesc(SysRole::getCreateTime);
        return sysRoleMapper.selectList(wrapper);
    }

    @Override
    public SysRole getById(Long id) {
        return sysRoleMapper.selectById(id);
    }

    @Override
    public boolean save(SysRole role) {
        return sysRoleMapper.insert(role) > 0;
    }

    @Override
    public boolean updateById(SysRole role) {
        return sysRoleMapper.updateById(role) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return sysRoleMapper.deleteById(id) > 0;
    }

    @Override
    public List<SysRole> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return sysRoleMapper.selectBatchIds(ids);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId, String moduleCode) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRoleMenu::getModuleCode, moduleCode);
        }
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindMenus(Long roleId, List<Long> menuIds, String moduleCode) {
        // 删除原有菜单
        LambdaQueryWrapper<SysRoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleMenu::getRoleId, roleId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysRoleMenu::getModuleCode, moduleCode);
        }
        sysRoleMenuMapper.delete(deleteWrapper);
        // 添加新菜单
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenu.setModuleCode(moduleCode);
                sysRoleMenuMapper.insert(roleMenu);
            }
        }
        return true;
    }

    @Override
    public List<Long> getApiIdsByRoleId(Long roleId, String moduleCode) {
        LambdaQueryWrapper<SysRoleApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleApi::getRoleId, roleId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRoleApi::getModuleCode, moduleCode);
        }
        List<SysRoleApi> roleApis = sysRoleApiMapper.selectList(wrapper);
        return roleApis.stream().map(SysRoleApi::getApiId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindApis(Long roleId, List<Long> apiIds, String moduleCode) {
        // 删除原有接口
        LambdaQueryWrapper<SysRoleApi> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleApi::getRoleId, roleId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysRoleApi::getModuleCode, moduleCode);
        }
        sysRoleApiMapper.delete(deleteWrapper);
        // 添加新接口
        if (apiIds != null && !apiIds.isEmpty()) {
            for (Long apiId : apiIds) {
                SysRoleApi roleApi = new SysRoleApi();
                roleApi.setRoleId(roleId);
                roleApi.setApiId(apiId);
                roleApi.setModuleCode(moduleCode);
                sysRoleApiMapper.insert(roleApi);
            }
        }
        return true;
    }

    @Override
    public List<Long> getUserIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleId, roleId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getUserId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindUsers(Long roleId, List<Long> userIds) {
        // 删除原有用户
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getRoleId, roleId);
        sysUserRoleMapper.delete(deleteWrapper);
        // 添加新用户
        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
        return true;
    }

    // ============ 角色成员维护相关方法 ============

    @Override
    public List<SysUser> getRoleUsers(Long roleId) {
        // 获取角色已绑定的所有用户ID
        List<Long> userIds = getUserIdsByRoleId(roleId);
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        // 查询用户信息
        return sysUserMapper.selectBatchIds(userIds);
    }

    @Override
    public List<SysUser> getOptionalUsers(Long roleId, String keyword, Integer status, Integer pageNum, Integer pageSize) {
        // 获取角色已绑定的用户ID
        Set<Long> boundUserIds = new HashSet<>(getUserIdsByRoleId(roleId));

        // 构建查询条件
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.notIn(boundUserIds.size() > 0, SysUser::getId, boundUserIds);
        wrapper.eq(status != null, SysUser::getStatus, status);
        wrapper.like(keyword != null && !keyword.isEmpty(), SysUser::getUsername, keyword);
        wrapper.orderByDesc(SysUser::getCreateTime);

        // 分页查询
        Page<SysUser> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        IPage<SysUser> result = sysUserMapper.selectPage(page, wrapper);

        return result.getRecords();
    }

    @Override
    @Transactional
    public BindResultVO bindUsersBatch(Long roleId, List<Long> userIds, String moduleCode) {
        if (userIds == null || userIds.isEmpty()) {
            return BindResultVO.success(0, "未选择用户");
        }

        // 获取已绑定的用户ID
        Set<Long> boundUserIds = new HashSet<>(getUserIdsByRoleId(roleId));

        List<Long> failUserIds = new ArrayList<>();
        int successCount = 0;

        for (Long userId : userIds) {
            // 检查是否已绑定
            if (boundUserIds.contains(userId)) {
                failUserIds.add(userId);
                continue;
            }
            // 绑定用户
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
            boundUserIds.add(userId); // 添加到已绑定集合，避免重复插入
            successCount++;
        }

        if (failUserIds.isEmpty()) {
            return BindResultVO.success(successCount, "成功绑定 " + successCount + " 个用户");
        } else {
            return BindResultVO.partial(successCount, failUserIds.size(), failUserIds,
                "成功绑定 " + successCount + " 个用户，" + failUserIds.size() + " 个用户已绑定");
        }
    }

    @Override
    @Transactional
    public BindResultVO unbindUsers(Long roleId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return BindResultVO.success(0, "未选择用户");
        }

        // 获取该角色所有用户
        List<Long> allUserIds = getUserIdsByRoleId(roleId);
        Set<Long> allUserIdSet = new HashSet<>(allUserIds);

        List<Long> failUserIds = new ArrayList<>();
        int successCount = 0;

        for (Long userId : userIds) {
            // 检查用户是否绑定该角色
            if (!allUserIdSet.contains(userId)) {
                failUserIds.add(userId);
                continue;
            }

            // 删除用户角色关联
            LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserRole::getRoleId, roleId);
            deleteWrapper.eq(SysUserRole::getUserId, userId);
            sysUserRoleMapper.delete(deleteWrapper);
            successCount++;
        }

        if (failUserIds.isEmpty()) {
            return BindResultVO.success(successCount, "成功移除 " + successCount + " 个成员");
        } else {
            return BindResultVO.partial(successCount, failUserIds.size(), failUserIds,
                "成功移除 " + successCount + " 个成员，" + failUserIds.size() + " 个成员不在该角色中");
        }
    }

    @Override
    @Transactional
    public boolean clearRoleUsers(Long roleId) {
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getRoleId, roleId);
        sysUserRoleMapper.delete(deleteWrapper);
        return true;
    }
}
