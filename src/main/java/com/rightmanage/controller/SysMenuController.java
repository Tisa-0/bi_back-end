package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.dto.SysMenuVO;
import com.rightmanage.entity.SysMenu;
import com.rightmanage.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("/list")
    public Result<List<SysMenu>> list(@RequestParam(required = false) String moduleCode) {
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return Result.success(sysMenuService.listByModuleCode(moduleCode));
        }
        return Result.success(sysMenuService.list());
    }

    /**
     * 获取树形菜单列表（支持无限层级）
     * 产品智能定制模块(C)支持tenantCode过滤，其他模块忽略tenantCode
     */
    @GetMapping("/tree")
    public Result<List<SysMenuVO>> tree(@RequestParam(required = false) String moduleCode,
                                          @RequestParam(required = false) String tenantCode,
                                          @RequestParam(required = false) Long userId) {
        if (userId != null && moduleCode != null && !moduleCode.isEmpty()) {
            // 如果传入userId，则根据用户角色过滤菜单
            // 模块bi_wx_product需要根据租户ID过滤
            if ("bi_wx_product".equals(moduleCode) && tenantCode != null) {
                return Result.success(sysMenuService.listTreeByUserIdAndModuleCodeAndTenant(userId, moduleCode, tenantCode));
            }
            return Result.success(sysMenuService.listTreeByUserIdAndModuleCode(userId, moduleCode));
        }
        if (moduleCode != null && !moduleCode.isEmpty()) {
            // 产品智能定制模块使用新的按租户过滤方法
            if ("bi_wx_product".equals(moduleCode) && tenantCode != null) {
                return Result.success(sysMenuService.listProductCustomMenuTree(tenantCode));
            }
            return Result.success(sysMenuService.listTreeByModuleCode(moduleCode));
        }
        return Result.success(sysMenuService.listTreeByModuleCode(null));
    }

    /**
     * 获取菜单树选项（用于新增/修改时的父菜单选择）
     * 产品智能定制模块(bi_wx_product)支持tenantCode过滤，其他模块忽略tenantCode
     */
    @GetMapping("/treeOptions")
    public Result<List<SysMenuVO>> treeOptions(@RequestParam String moduleCode,
                                                @RequestParam(required = false) String tenantCode) {
        // 产品智能定制模块使用新的按租户过滤方法
        if ("bi_wx_product".equals(moduleCode) && tenantCode != null) {
            return Result.success(sysMenuService.getProductCustomMenuTreeOptions(tenantCode));
        }
        return Result.success(sysMenuService.getMenuTreeOptions(moduleCode));
    }

    @GetMapping("/{menuId}")
    public Result<SysMenu> getById(@PathVariable String menuId) {
        return Result.success(sysMenuService.getById(menuId));
    }

    @PostMapping
    public Result<?> save(@RequestBody SysMenu menu) {
        // 校验父菜单不能是自身或子菜单
        if (!sysMenuService.validateParentId(menu.getMenuId(), menu.getParentId())) {
            return Result.error("父菜单不能选择自身或子菜单");
        }
        sysMenuService.save(menu);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody SysMenu menu) {
        // 校验父菜单不能是自身或子菜单
        if (!sysMenuService.validateParentId(menu.getMenuId(), menu.getParentId())) {
            return Result.error("父菜单不能选择自身或子菜单");
        }
        sysMenuService.updateById(menu);
        return Result.success();
    }

    @DeleteMapping("/{menuId}")
    public Result<?> delete(@PathVariable String menuId) {
        sysMenuService.deleteById(menuId);
        return Result.success();
    }

    /**
     * 级联删除菜单（删除菜单及其所有子菜单）
     */
    @DeleteMapping("/cascade/{menuId}")
    public Result<?> deleteCascade(@PathVariable String menuId) {
        sysMenuService.deleteWithChildren(menuId);
        return Result.success();
    }

    @PutMapping("/status/{menuId}")
    public Result<?> updateStatus(@PathVariable String menuId, @RequestParam Integer status) {
        sysMenuService.updateStatus(menuId, status);
        return Result.success();
    }

    // ==================== 产品智能定制模块接口（全局结构+租户独立状态） ====================

    /**
     * 获取产品智能定制菜单列表（合并全局结构+租户状态）
     */
    @GetMapping("/tenant-product-list")
    public Result<?> listProductCustomMenus(@RequestParam(required = false) String tenantCode) {
        return Result.success(sysMenuService.listProductCustomMenus(tenantCode));
    }

    /**
     * 获取产品智能定制菜单树（合并全局结构+租户状态）
     */
    @GetMapping("/tenant-product-tree")
    public Result<?> listProductCustomMenuTree(@RequestParam(required = false) String tenantCode) {
        return Result.success(sysMenuService.listProductCustomMenuTree(tenantCode));
    }

    /**
     * 获取产品智能定制菜单树选项（用于新增/修改时的父菜单选择）
     */
    @GetMapping("/tenant-product-treeOptions")
    public Result<?> getProductCustomMenuTreeOptions(@RequestParam(required = false) String tenantCode) {
        return Result.success(sysMenuService.getProductCustomMenuTreeOptions(tenantCode));
    }

    /**
     * 更新产品智能定制菜单
     * 结构字段全局生效，status仅租户生效
     */
    @PostMapping("/update-product-custom")
    public Result<?> updateProductCustomMenu(@RequestBody SysMenu menu, @RequestParam String operateTenantCode) {
        try {
            sysMenuService.updateProductCustomMenu(menu, operateTenantCode);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除产品智能定制菜单（全局记录+所有租户状态记录）
     */
    @DeleteMapping("/delete-product-custom/{menuId}")
    public Result<?> deleteProductCustomMenu(@PathVariable String menuId) {
        try {
            sysMenuService.deleteProductCustomMenu(menuId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增产品智能定制菜单（全局记录+所有租户状态记录）
     */
    @PostMapping("/save-product-custom")
    public Result<?> saveProductCustomMenu(@RequestBody SysMenu menu, @RequestParam List<String> tenantCodes) {
        try {
            System.out.println("saveProductCustomMenu 接收到的 menu: " + menu);
            System.out.println("moduleCode: " + menu.getModuleCode());
            sysMenuService.saveProductCustomMenu(menu, tenantCodes);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 同步产品智能定制菜单结构到所有租户
     */
    @PostMapping("/sync-product-custom")
    public Result<?> syncProductCustomMenuToAllTenants() {
        try {
            sysMenuService.syncProductCustomMenuToAllTenants();
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 初始化租户菜单状态（为指定租户初始化所有产品智能定制菜单的默认状态）
     */
    @PostMapping("/init-tenant-status")
    public Result<?> initTenantMenuStatus(@RequestParam String tenantCode) {
        try {
            sysMenuService.initTenantMenuStatus(tenantCode);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
