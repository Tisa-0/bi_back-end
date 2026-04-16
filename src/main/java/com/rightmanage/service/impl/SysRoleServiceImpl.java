package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.dto.BindResultVO;
import com.rightmanage.entity.*;
import com.rightmanage.mapper.*;
import com.rightmanage.service.SysModuleService;
import com.rightmanage.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.StringUtils;

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
    @Autowired
    private SysModuleService sysModuleService;

    /**
     * 判断是否是多租户模块
     */
    private boolean isMultiTenantModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isEmpty()) {
            return false;
        }
        return sysModuleService.isMultiTenant(moduleCode);
    }

    private String normalizeTenantCode(String moduleCode, String tenantCode) {
        if (isMultiTenantModule(moduleCode)) {
            return tenantCode != null ? tenantCode : null;
        }
        return "";
    }

    @Override
    public IPage<SysRole> pageByModuleCode(Integer pageNum, Integer pageSize, String moduleCode, String tenantCode) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRole::getModuleCode, moduleCode);
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
        wrapper.orderByDesc(SysRole::getCreateTime);
        return sysRoleMapper.selectList(wrapper);
    }

    @Override
    public List<SysRole> listByModuleCodeAndTenantCode(String moduleCode, String tenantCode) {
        // 多租户模块：所有租户共用同一套角色，直接返回角色列表
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRole::getModuleCode, moduleCode);
        }
        wrapper.orderByDesc(SysRole::getCreateTime);
        return sysRoleMapper.selectList(wrapper);
    }

    @Override
    public SysRole getById(String roleCode) {
        return sysRoleMapper.selectById(roleCode);
    }

    @Override
    public boolean save(SysRole role) {
        return sysRoleMapper.insert(role) > 0;
    }

    @Override
    public boolean updateById(SysRole role) {
        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode())
               .set(role.getOrgRelated() != null, SysRole::getOrgRelated, role.getOrgRelated())
               .set(SysRole::getTenantCode, role.getTenantCode())
               .set(SysRole::getRoleName, role.getRoleName())
               .set(SysRole::getDescription, role.getDescription())
               .set(SysRole::getModuleCode, role.getModuleCode());
        return sysRoleMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean deleteById(String roleCode) {
        return sysRoleMapper.deleteById(roleCode) > 0;
    }

    @Override
    public List<SysRole> listByIds(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return sysRoleMapper.selectBatchIds(roleCodes);
    }

    @Override
    public List<String> getMenuIdsByRoleId(String roleCode, String moduleCode, String tenantCode) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleCode, roleCode);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRoleMenu::getModuleCode, moduleCode);
        }
        // 多租户模块需要按租户筛选
        if (isMultiTenantModule(moduleCode)) {
            if (tenantCode != null) {
                wrapper.eq(SysRoleMenu::getTenantCode, tenantCode);
            }
        } else if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRoleMenu::getTenantCode, "");
        }
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindMenus(String roleCode, List<String> menuIds, String moduleCode, String tenantCode) {
        // 删除原有菜单（多租户模块需要区分租户）
        LambdaQueryWrapper<SysRoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleMenu::getRoleCode, roleCode);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysRoleMenu::getModuleCode, moduleCode);
        }
        // 多租户模块需要按租户删除
        if (isMultiTenantModule(moduleCode)) {
            if (tenantCode != null) {
                deleteWrapper.eq(SysRoleMenu::getTenantCode, tenantCode);
            }
        } else if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysRoleMenu::getTenantCode, "");
        }
        sysRoleMenuMapper.delete(deleteWrapper);
        // 添加新菜单
        if (menuIds != null && !menuIds.isEmpty()) {
            for (String menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleCode(roleCode);
                roleMenu.setMenuId(menuId);
                roleMenu.setModuleCode(moduleCode);
                roleMenu.setTenantCode(normalizeTenantCode(moduleCode, tenantCode));
                sysRoleMenuMapper.insert(roleMenu);
            }
        }
        return true;
    }

    @Override
    public List<Long> getApiIdsByRoleId(String roleCode, String moduleCode) {
        LambdaQueryWrapper<SysRoleApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleApi::getRoleCode, roleCode);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysRoleApi::getModuleCode, moduleCode);
        }
        List<SysRoleApi> roleApis = sysRoleApiMapper.selectList(wrapper);
        return roleApis.stream().map(SysRoleApi::getApiId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindApis(String roleCode, List<Long> apiIds, String moduleCode) {
        // 删除原有接口
        LambdaQueryWrapper<SysRoleApi> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleApi::getRoleCode, roleCode);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysRoleApi::getModuleCode, moduleCode);
        }
        sysRoleApiMapper.delete(deleteWrapper);
        // 添加新接口
        if (apiIds != null && !apiIds.isEmpty()) {
            for (Long apiId : apiIds) {
                SysRoleApi roleApi = new SysRoleApi();
                roleApi.setRoleCode(roleCode);
                roleApi.setApiId(apiId);
                roleApi.setModuleCode(moduleCode);
                sysRoleApiMapper.insert(roleApi);
            }
        }
        return true;
    }

    @Override
    public List<Long> getUserIdsByRoleId(String roleCode, String moduleCode, String tenantCode) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleCode, roleCode);
        if (StringUtils.hasText(moduleCode)) {
            wrapper.eq(SysUserRole::getModuleCode, moduleCode);
            if (isMultiTenantModule(moduleCode)) {
                if (tenantCode != null) {
                    wrapper.eq(SysUserRole::getTenantCode, tenantCode);
                }
            } else {
                wrapper.eq(SysUserRole::getTenantCode, "");
            }
        } else if (tenantCode != null) {
            wrapper.eq(SysUserRole::getTenantCode, tenantCode);
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getUserId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindUsers(String roleCode, List<Long> userIds, String moduleCode, String tenantCode) {
        // 删除原有用户（多租户模块需要区分租户）
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getRoleCode, roleCode);
        if (StringUtils.hasText(moduleCode)) {
            deleteWrapper.eq(SysUserRole::getModuleCode, moduleCode);
            if (isMultiTenantModule(moduleCode)) {
                if (tenantCode != null) {
                    deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
                }
            } else {
                deleteWrapper.eq(SysUserRole::getTenantCode, "");
            }
        } else if (tenantCode != null) {
            deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
        }
        sysUserRoleMapper.delete(deleteWrapper);
        // 添加新用户
        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleCode(roleCode);
                userRole.setModuleCode(moduleCode);
                userRole.setTenantCode(normalizeTenantCode(moduleCode, tenantCode));
                sysUserRoleMapper.insert(userRole);
            }
        }
        return true;
    }

    // ============ 角色成员维护相关方法 ============

    @Override
    public List<SysUser> getRoleUsers(String roleCode, String moduleCode, String tenantCode) {
        // 获取角色已绑定的所有用户ID（多租户模块需要区分租户）
        List<Long> userIds = getUserIdsByRoleId(roleCode, moduleCode, tenantCode);
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        // 查询用户信息
        return sysUserMapper.selectBatchIds(userIds);
    }

    @Override
    public List<SysUser> getOptionalUsers(String roleCode, String keyword, Integer status, Integer pageNum, Integer pageSize, String tenantCode) {
        // 获取角色已绑定的用户ID（多租户模块需要区分租户）
        Set<Long> boundUserIds = new HashSet<>(getUserIdsByRoleId(roleCode, "bi_wx_product", tenantCode));

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
    public BindResultVO bindUsersBatch(String roleCode, List<Long> userIds, String moduleCode, String tenantCode) {
        if (userIds == null || userIds.isEmpty()) {
            return BindResultVO.success(0, "未选择用户");
        }

        // 获取已绑定的用户ID（多租户模块需要区分租户）
        Set<Long> boundUserIds = new HashSet<>(getUserIdsByRoleId(roleCode, moduleCode, tenantCode));

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
            userRole.setRoleCode(roleCode);
            userRole.setModuleCode(moduleCode);
            userRole.setTenantCode(normalizeTenantCode(moduleCode, tenantCode));
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
    public BindResultVO unbindUsers(String roleCode, List<Long> userIds, String moduleCode, String tenantCode) {
        if (userIds == null || userIds.isEmpty()) {
            return BindResultVO.success(0, "未选择用户");
        }

        // 获取该角色所有用户（多租户模块需要区分租户）
        List<Long> allUserIds = getUserIdsByRoleId(roleCode, moduleCode, tenantCode);
        Set<Long> allUserIdSet = new HashSet<>(allUserIds);

        List<Long> failUserIds = new ArrayList<>();
        int successCount = 0;

        for (Long userId : userIds) {
            // 检查用户是否绑定该角色
            if (!allUserIdSet.contains(userId)) {
                failUserIds.add(userId);
                continue;
            }

            // 删除用户角色关联（多租户模块需要区分租户）
            LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserRole::getRoleCode, roleCode);
            deleteWrapper.eq(SysUserRole::getUserId, userId);
            if (StringUtils.hasText(moduleCode)) {
                deleteWrapper.eq(SysUserRole::getModuleCode, moduleCode);
                if (isMultiTenantModule(moduleCode)) {
                    if (tenantCode != null) {
                        deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
                    }
                } else {
                    deleteWrapper.eq(SysUserRole::getTenantCode, "");
                }
            } else if (tenantCode != null) {
                deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
            }
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
    public boolean clearRoleUsers(String roleCode, String moduleCode, String tenantCode) {
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getRoleCode, roleCode);
        if (StringUtils.hasText(moduleCode)) {
            deleteWrapper.eq(SysUserRole::getModuleCode, moduleCode);
            if (isMultiTenantModule(moduleCode)) {
                if (tenantCode != null) {
                    deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
                }
            } else {
                deleteWrapper.eq(SysUserRole::getTenantCode, "");
            }
        } else if (tenantCode != null) {
            deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
        }
        sysUserRoleMapper.delete(deleteWrapper);
        return true;
    }
}
