package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rightmanage.dto.SysMenuVO;
import com.rightmanage.entity.SysMenu;
import com.rightmanage.entity.SysRoleMenu;
import com.rightmanage.entity.SysTenantMenuStatus;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.mapper.SysMenuMapper;
import com.rightmanage.mapper.SysRoleMenuMapper;
import com.rightmanage.mapper.SysUserRoleMapper;
import com.rightmanage.mapper.SysTenantMenuStatusMapper;
import com.rightmanage.service.SysMenuService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    /**
     * 产品智能定制模块编码
     */
    private static final String PRODUCT_CUSTOM_MODULE_CODE = "C";

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysTenantMenuStatusMapper sysTenantMenuStatusMapper;

    @Override
    public List<SysMenu> list() {
        return sysMenuMapper.selectList(null);
    }

    @Override
    public List<SysMenu> listByModuleCode(String moduleCode) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getModuleCode, moduleCode)
               .orderByAsc(SysMenu::getSort);
        return sysMenuMapper.selectList(wrapper);
    }

    @Override
    public List<SysMenuVO> listTreeByModuleCode(String moduleCode) {
        List<SysMenu> menuList = listByModuleCode(moduleCode);
        return buildMenuTree(menuList, 0L);
    }

    @Override
    public List<SysMenuVO> listTreeByUserIdAndModuleCode(Long userId, String moduleCode) {
        System.out.println("listTreeByUserIdAndModuleCode - userId: " + userId + ", moduleCode: " + moduleCode);

        // 1. 获取用户在指定模块下的角色ID列表
        List<Long> roleIds = getUserRoleIdsByModuleCode(userId, moduleCode);
        System.out.println("listTreeByUserIdAndModuleCode - roleIds: " + roleIds);

        if (roleIds.isEmpty()) {
            System.out.println("listTreeByUserIdAndModuleCode - No roles found for user in module: " + moduleCode);
            return new ArrayList<>();
        }

        // 2. 获取这些角色绑定的菜单ID
        List<Long> menuIds = getMenuIdsByRoleIds(roleIds, moduleCode);
        System.out.println("listTreeByUserIdAndModuleCode - menuIds: " + menuIds);

        if (menuIds.isEmpty()) {
            System.out.println("listTreeByUserIdAndModuleCode - No menus found for roles in module: " + moduleCode);
            return new ArrayList<>();
        }

        // 3. 获取所有父菜单ID（确保子菜单的父菜单也被包含）
        List<Long> allMenuIds = new ArrayList<>(menuIds);
        List<Long> parentMenuIds = new ArrayList<>(menuIds);
        while (!parentMenuIds.isEmpty()) {
            // 查询这些菜单的父菜单
            LambdaQueryWrapper<SysMenu> parentWrapper = new LambdaQueryWrapper<>();
            parentWrapper.in(SysMenu::getId, parentMenuIds)
                        .eq(SysMenu::getModuleCode, moduleCode);
            List<SysMenu> parentMenus = sysMenuMapper.selectList(parentWrapper);

            parentMenuIds.clear();
            for (SysMenu menu : parentMenus) {
                if (menu.getParentId() != null && menu.getParentId() != 0 && !allMenuIds.contains(menu.getParentId())) {
                    parentMenuIds.add(menu.getParentId());
                    allMenuIds.add(menu.getParentId());
                }
            }
        }
        System.out.println("listTreeByUserIdAndModuleCode - allMenuIds (with parents): " + allMenuIds);

        // 4. 获取菜单列表（包含所有父菜单和子菜单）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getId, allMenuIds)
               .orderByAsc(SysMenu::getSort);
        List<SysMenu> menuList = sysMenuMapper.selectList(wrapper);
        System.out.println("listTreeByUserIdAndModuleCode - menuList size: " + menuList.size());

        // 5. 构建菜单树
        return buildMenuTree(menuList, 0L);
    }

    @Override
    public List<SysMenuVO> listTreeByUserIdAndModuleCodeAndTenant(Long userId, String moduleCode, Long tenantId) {
        System.out.println("listTreeByUserIdAndModuleCodeAndTenant - userId: " + userId + ", moduleCode: " + moduleCode + ", tenantId: " + tenantId);

        // 1. 获取用户在指定模块、指定租户下的角色ID列表
        List<Long> roleIds = getUserRoleIdsByModuleCodeAndTenant(userId, moduleCode, tenantId);
        System.out.println("listTreeByUserIdAndModuleCodeAndTenant - roleIds: " + roleIds);

        if (roleIds.isEmpty()) {
            System.out.println("listTreeByUserIdAndModuleCodeAndTenant - No roles found for user in module: " + moduleCode + ", tenant: " + tenantId);
            return new ArrayList<>();
        }

        // 2. 获取这些角色绑定的菜单ID（需要同时匹配模块和租户）
        List<Long> menuIds = getMenuIdsByRoleIdsAndTenant(roleIds, moduleCode, tenantId);
        System.out.println("listTreeByUserIdAndModuleCodeAndTenant - menuIds: " + menuIds);

        if (menuIds.isEmpty()) {
            System.out.println("listTreeByUserIdAndModuleCodeAndTenant - No menus found for roles in module: " + moduleCode);
            return new ArrayList<>();
        }

        // 3. 获取所有父菜单ID（确保子菜单的父菜单也被包含）
        List<Long> allMenuIds = new ArrayList<>(menuIds);
        List<Long> parentMenuIds = new ArrayList<>(menuIds);
        while (!parentMenuIds.isEmpty()) {
            // 查询这些菜单的父菜单
            LambdaQueryWrapper<SysMenu> parentWrapper = new LambdaQueryWrapper<>();
            parentWrapper.in(SysMenu::getId, parentMenuIds)
                        .eq(SysMenu::getModuleCode, moduleCode);
            List<SysMenu> parentMenus = sysMenuMapper.selectList(parentWrapper);

            parentMenuIds.clear();
            for (SysMenu menu : parentMenus) {
                if (menu.getParentId() != null && menu.getParentId() != 0 && !allMenuIds.contains(menu.getParentId())) {
                    parentMenuIds.add(menu.getParentId());
                    allMenuIds.add(menu.getParentId());
                }
            }
        }
        System.out.println("listTreeByUserIdAndModuleCodeAndTenant - allMenuIds (with parents): " + allMenuIds);

        // 4. 获取菜单列表（包含所有父菜单和子菜单）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getId, allMenuIds)
               .orderByAsc(SysMenu::getSort);
        List<SysMenu> menuList = sysMenuMapper.selectList(wrapper);
        System.out.println("listTreeByUserIdAndModuleCodeAndTenant - menuList size: " + menuList.size());

        // 5. 构建菜单树
        return buildMenuTree(menuList, 0L);
    }

    /**
     * 获取用户在指定模块、指定租户下的角色ID列表
     */
    private List<Long> getUserRoleIdsByModuleCodeAndTenant(Long userId, String moduleCode, Long tenantId) {
        // 通过SysUserRole表查询用户角色
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleWrapper);

        System.out.println("getUserRoleIdsByModuleCodeAndTenant - userRoles count: " + userRoles.size());

        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        System.out.println("getUserRoleIdsByModuleCodeAndTenant - all roleIds: " + roleIds);

        // 进一步过滤：只获取指定模块的角色（通过SysRoleMenu表查询）
        // 注意：模块C需要匹配tenantId，但也要允许tenantId为NULL的情况（表示该角色在该模块下有权限）
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(SysRoleMenu::getRoleId, roleIds)
                       .eq(SysRoleMenu::getModuleCode, moduleCode);
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(roleMenuWrapper);

        System.out.println("getUserRoleIdsByModuleCodeAndTenant - roleMenus in module " + moduleCode + ": " + roleMenus.size());

        // 过滤出与租户匹配的角色（匹配tenantId，或者tenantId为NULL表示该模块下的通用权限）
        return roleMenus.stream()
                .filter(rm -> tenantId.equals(rm.getTenantId()) || rm.getTenantId() == null)
                .map(SysRoleMenu::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取用户在指定模块下的角色ID列表
     */
    private List<Long> getUserRoleIdsByModuleCode(Long userId, String moduleCode) {
        // 通过SysUserRole表查询用户角色
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleWrapper);

        System.out.println("getUserRoleIdsByModuleCode - userRoles count: " + userRoles.size());

        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        System.out.println("getUserRoleIdsByModuleCode - all roleIds: " + roleIds);

        // 进一步过滤：只获取指定模块的角色（通过SysRoleMenu表查询）
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(SysRoleMenu::getRoleId, roleIds)
                       .eq(SysRoleMenu::getModuleCode, moduleCode);
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(roleMenuWrapper);

        System.out.println("getUserRoleIdsByModuleCode - roleMenus in module " + moduleCode + ": " + roleMenus.size());

        return roleMenus.stream()
                .map(SysRoleMenu::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取角色绑定的菜单ID列表
     */
    private List<Long> getMenuIdsByRoleIds(List<Long> roleIds, String moduleCode) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRoleMenu::getRoleId, roleIds)
               .eq(SysRoleMenu::getModuleCode, moduleCode);
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(wrapper);
        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取角色绑定的菜单ID（同时匹配模块和租户）
     * 注意：也返回tenantId为NULL的菜单（表示该模块下的通用菜单）
     */
    private List<Long> getMenuIdsByRoleIdsAndTenant(List<Long> roleIds, String moduleCode, Long tenantId) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRoleMenu::getRoleId, roleIds)
               .eq(SysRoleMenu::getModuleCode, moduleCode);
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(wrapper);
        
        // 过滤出与租户匹配的菜单（匹配tenantId，或者tenantId为NULL表示通用菜单）
        return roleMenus.stream()
                .filter(rm -> tenantId.equals(rm.getTenantId()) || rm.getTenantId() == null)
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public SysMenu getById(Long id) {
        return sysMenuMapper.selectById(id);
    }

    @Override
    public boolean save(SysMenu menu) {
        return sysMenuMapper.insert(menu) > 0;
    }

    @Override
    public boolean updateById(SysMenu menu) {
        return sysMenuMapper.updateById(menu) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return sysMenuMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean deleteWithChildren(Long id) {
        // 获取所有子菜单ID（包括自身）
        List<Long> menuIds = getAllChildIds(id);
        // 删除角色菜单关联数据
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRoleMenu::getMenuId, menuIds);
        sysRoleMenuMapper.delete(wrapper);
        // 批量删除菜单
        return sysMenuMapper.deleteBatchIds(menuIds) > 0;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SysMenu> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysMenu::getId, id)
               .set(SysMenu::getStatus, status);
        return sysMenuMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean validateParentId(Long id, Long parentId) {
        // 如果是新增（id为null）或者设置parentId为0（顶级菜单），直接通过
        if (id == null || parentId == null || parentId == 0) {
            return true;
        }
        // 不能将自身设置为父菜单
        if (id.equals(parentId)) {
            return false;
        }
        // 不能将子菜单设置为父菜单（避免循环引用）
        List<Long> childIds = getAllChildIds(id);
        return !childIds.contains(parentId);
    }

    @Override
    public List<SysMenuVO> getMenuTreeOptions(String moduleCode) {
        List<SysMenu> menuList = listByModuleCode(moduleCode);
        // 添加一个虚拟的顶级菜单选项
        SysMenu rootMenu = new SysMenu();
        rootMenu.setId(0L);
        rootMenu.setMenuName("顶级菜单");
        rootMenu.setParentId(null);
        rootMenu.setModuleCode(moduleCode);
        rootMenu.setSort(0);
        menuList.add(0, rootMenu);
        return buildMenuTree(menuList, 0L);
    }

    /**
     * 递归构建菜单树（带循环检测）
     */
    private List<SysMenuVO> buildMenuTree(List<SysMenu> menuList, Long parentId) {
        return buildMenuTreeWithHistory(menuList, parentId, new HashSet<>());
    }

    /**
     * 递归构建菜单树（带历史记录防循环）
     */
    private List<SysMenuVO> buildMenuTreeWithHistory(List<SysMenu> menuList, Long parentId, Set<Long> visitedIds) {
        List<SysMenuVO> treeNodeList = new ArrayList<>();
        for (SysMenu menu : menuList) {
            if (parentId.equals(menu.getParentId())) {
                // 防循环：检查是否已访问过
                if (visitedIds.contains(menu.getId())) {
                    continue;
                }
                visitedIds.add(menu.getId());

                SysMenuVO menuVO = convertToVO(menu);
                // 递归查询当前菜单的所有子菜单（无限层级）
                List<SysMenuVO> children = buildMenuTreeWithHistory(menuList, menu.getId(), visitedIds);
                menuVO.setChildren(children);
                treeNodeList.add(menuVO);

                // 回溯时移除当前节点，允许从其他路径访问
                visitedIds.remove(menu.getId());
            }
        }
        // 按sort字段排序子菜单
        treeNodeList.sort(Comparator.comparingInt(SysMenuVO::getSort));
        return treeNodeList;
    }

    /**
     * 将实体转换为VO
     */
    private SysMenuVO convertToVO(SysMenu menu) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(menu.getId());
        vo.setMenuName(menu.getMenuName());
        vo.setParentId(menu.getParentId());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setModuleCode(menu.getModuleCode());
        vo.setSort(menu.getSort());
        vo.setStatus(menu.getStatus());
        vo.setCreateTime(menu.getCreateTime());
        vo.setUpdateTime(menu.getUpdateTime());
        return vo;
    }

    // ==================== 产品智能定制模块实现（全局结构+租户独立状态） ====================

    @Override
    public List<SysMenu> listProductCustomMenus(Long tenantId) {
        // 1. 查询全局菜单结构
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getModuleCode, PRODUCT_CUSTOM_MODULE_CODE)
                .eq(SysMenu::getDeleted, 0)
                .orderByAsc(SysMenu::getSort);
        List<SysMenu> globalMenus = sysMenuMapper.selectList(wrapper);

        if (tenantId == null) {
            // 如果没有指定租户，返回全局基础记录
            return globalMenus;
        }

        // 2. 查询该租户的菜单状态
        List<Long> menuIds = globalMenus.stream().map(SysMenu::getId).collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return globalMenus;
        }

        LambdaQueryWrapper<SysTenantMenuStatus> statusWrapper = new LambdaQueryWrapper<>();
        statusWrapper.eq(SysTenantMenuStatus::getTenantId, tenantId)
                .in(SysTenantMenuStatus::getMenuId, menuIds)
                .eq(SysTenantMenuStatus::getDeleted, 0);
        List<SysTenantMenuStatus> tenantStatusList = sysTenantMenuStatusMapper.selectList(statusWrapper);

        // 构建 menuId -> status 的映射
        Map<Long, Integer> tenantStatusMap = new HashMap<>();
        for (SysTenantMenuStatus status : tenantStatusList) {
            tenantStatusMap.put(status.getMenuId(), status.getStatus());
        }

        // 3. 合并：全局结构 + 租户状态（无状态则默认启用）
        List<SysMenu> resultMenus = new ArrayList<>();
        for (SysMenu globalMenu : globalMenus) {
            // 创建新的菜单对象，避免修改原始数据
            SysMenu mergedMenu = new SysMenu();
            BeanUtils.copyProperties(globalMenu, mergedMenu);

            // 覆盖为租户独立状态，无记录则用全局默认status
            if (tenantStatusMap.containsKey(globalMenu.getId())) {
                mergedMenu.setStatus(tenantStatusMap.get(globalMenu.getId()));
            }

            resultMenus.add(mergedMenu);
        }

        return resultMenus;
    }

    @Override
    public List<SysMenuVO> listProductCustomMenuTree(Long tenantId) {
        List<SysMenu> menuList = listProductCustomMenus(tenantId);
        return buildMenuTree(menuList, 0L);
    }

    @Override
    public List<SysMenuVO> getProductCustomMenuTreeOptions(Long tenantId) {
        // 产品智能定制模块：树选项不区分租户，使用全局结构
        List<SysMenu> menuList = listByModuleCode(PRODUCT_CUSTOM_MODULE_CODE);
        // 添加一个虚拟的顶级菜单选项
        SysMenu rootMenu = new SysMenu();
        rootMenu.setId(0L);
        rootMenu.setMenuName("顶级菜单");
        rootMenu.setParentId(0L);
        rootMenu.setModuleCode(PRODUCT_CUSTOM_MODULE_CODE);
        rootMenu.setSort(0);
        menuList.add(0, rootMenu);
        return buildMenuTree(menuList, 0L);
    }

    @Override
    @Transactional
    public boolean updateProductCustomMenu(SysMenu menu, Long operateTenantId) {
        // 1. 判断是否仅更新 status
        boolean isOnlyStatus = menu.getStatus() != null
                && menu.getMenuName() == null
                && menu.getPath() == null
                && menu.getComponent() == null
                && menu.getSort() == null;

        if (isOnlyStatus && operateTenantId != null) {
            // 场景1：仅更新 status - 更新租户菜单状态表
            return updateTenantMenuStatus(menu.getId(), operateTenantId, menu.getStatus());
        } else {
            // 场景2：更新结构字段 - 更新全局菜单表（全局生效）
            SysMenu globalMenu = sysMenuMapper.selectById(menu.getId());
            if (globalMenu == null) {
                throw new RuntimeException("菜单不存在");
            }
            if (!PRODUCT_CUSTOM_MODULE_CODE.equals(globalMenu.getModuleCode())) {
                throw new RuntimeException("仅允许更新产品智能定制模块的菜单结构");
            }

            // 仅更新结构字段
            if (menu.getMenuName() != null) {
                globalMenu.setMenuName(menu.getMenuName());
            }
            if (menu.getPath() != null) {
                globalMenu.setPath(menu.getPath());
            }
            if (menu.getComponent() != null) {
                globalMenu.setComponent(menu.getComponent());
            }
            if (menu.getSort() != null) {
                globalMenu.setSort(menu.getSort());
            }
            // 注意：不更新status，status由租户状态表管理

            return sysMenuMapper.updateById(globalMenu) > 0;
        }
    }

    /**
     * 更新租户菜单状态
     */
    private boolean updateTenantMenuStatus(Long menuId, Long tenantId, Integer status) {
        // 查询是否已有状态记录
        LambdaQueryWrapper<SysTenantMenuStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTenantMenuStatus::getTenantId, tenantId)
                .eq(SysTenantMenuStatus::getMenuId, menuId)
                .eq(SysTenantMenuStatus::getDeleted, 0);
        SysTenantMenuStatus existingStatus = sysTenantMenuStatusMapper.selectOne(wrapper);

        if (existingStatus != null) {
            // 更新已有记录
            existingStatus.setStatus(status);
            return sysTenantMenuStatusMapper.updateById(existingStatus) > 0;
        } else {
            // 新增记录
            SysTenantMenuStatus newStatus = new SysTenantMenuStatus();
            newStatus.setTenantId(tenantId);
            newStatus.setMenuId(menuId);
            newStatus.setStatus(status);
            return sysTenantMenuStatusMapper.insert(newStatus) > 0;
        }
    }

    @Override
    @Transactional
    public boolean deleteProductCustomMenu(Long menuId) {
        // 1. 获取该菜单的所有子菜单ID（包括自身）
        List<Long> menuIds = getAllChildIds(menuId);

        // 2. 删除所有租户的状态记录
        LambdaQueryWrapper<SysTenantMenuStatus> statusWrapper = new LambdaQueryWrapper<>();
        statusWrapper.in(SysTenantMenuStatus::getMenuId, menuIds);
        sysTenantMenuStatusMapper.delete(statusWrapper);

        // 3. 删除角色菜单关联
        for (Long id : menuIds) {
            LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
            roleMenuWrapper.eq(SysRoleMenu::getMenuId, id);
            sysRoleMenuMapper.delete(roleMenuWrapper);
        }

        // 4. 删除全局菜单记录
        return sysMenuMapper.deleteBatchIds(menuIds) > 0;
    }

    @Override
    @Transactional
    public boolean saveProductCustomMenu(SysMenu menu, List<Long> tenantIds) {
        // 1. 创建全局菜单记录
        // 保留前端传入的 moduleCode，不强制覆盖
        // 如果 parentId 是顶级菜单，设置为 0
        if (menu.getParentId() == null || menu.getParentId() == 0) {
            menu.setParentId(0L);
        }
        // status 使用默认值
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        sysMenuMapper.insert(menu);

        Long globalMenuId = menu.getId();

        // 2. 为所有租户生成状态记录
        if (tenantIds != null && !tenantIds.isEmpty()) {
            for (Long tenantId : tenantIds) {
                SysTenantMenuStatus tenantStatus = new SysTenantMenuStatus();
                tenantStatus.setTenantId(tenantId);
                tenantStatus.setMenuId(globalMenuId);
                tenantStatus.setStatus(menu.getStatus());
                sysTenantMenuStatusMapper.insert(tenantStatus);
            }
        }

        return true;
    }

    @Override
    @Transactional
    public boolean syncProductCustomMenuToAllTenants() {
        // 产品智能定制模块：结构字段在 sys_menu 表中，所有租户共用
        // 无需同步，因为所有租户共用同一套结构数据
        return true;
    }

    @Override
    @Transactional
    public boolean initTenantMenuStatus(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 查询所有产品智能定制菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getModuleCode, PRODUCT_CUSTOM_MODULE_CODE)
                .eq(SysMenu::getDeleted, 0);
        List<SysMenu> menus = sysMenuMapper.selectList(wrapper);

        if (menus.isEmpty()) {
            return true;
        }

        // 检查是否已有状态记录
        List<Long> menuIds = menus.stream().map(SysMenu::getId).collect(Collectors.toList());
        LambdaQueryWrapper<SysTenantMenuStatus> statusWrapper = new LambdaQueryWrapper<>();
        statusWrapper.eq(SysTenantMenuStatus::getTenantId, tenantId)
                .in(SysTenantMenuStatus::getMenuId, menuIds)
                .eq(SysTenantMenuStatus::getDeleted, 0);
        List<SysTenantMenuStatus> existingStatuses = sysTenantMenuStatusMapper.selectList(statusWrapper);

        // 已存在的菜单ID集合
        List<Long> existingMenuIds = existingStatuses.stream()
                .map(SysTenantMenuStatus::getMenuId)
                .collect(Collectors.toList());

        // 为没有状态记录的菜单创建默认状态
        for (SysMenu menu : menus) {
            if (!existingMenuIds.contains(menu.getId())) {
                SysTenantMenuStatus newStatus = new SysTenantMenuStatus();
                newStatus.setTenantId(tenantId);
                newStatus.setMenuId(menu.getId());
                newStatus.setStatus(menu.getStatus() != null ? menu.getStatus() : 1);
                sysTenantMenuStatusMapper.insert(newStatus);
            }
        }

        return true;
    }

    /**
     * 获取指定菜单的所有子菜单ID（包括自身）
     */
    private List<Long> getAllChildIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        result.add(parentId);

        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, parentId);
        List<SysMenu> children = sysMenuMapper.selectList(wrapper);

        for (SysMenu child : children) {
            result.addAll(getAllChildIds(child.getId()));
        }
        return result;
    }
}
