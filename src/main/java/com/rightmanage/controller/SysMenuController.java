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
     * 产品智能定制模块(C)支持tenantId过滤，其他模块忽略tenantId
     */
    @GetMapping("/tree")
    public Result<List<SysMenuVO>> tree(@RequestParam(required = false) String moduleCode,
                                          @RequestParam(required = false) Long tenantId) {
        if (moduleCode != null && !moduleCode.isEmpty()) {
            // 产品智能定制模块使用新的按租户过滤方法
            if ("C".equals(moduleCode) && tenantId != null) {
                return Result.success(sysMenuService.listProductCustomMenuTree(tenantId));
            }
            return Result.success(sysMenuService.listTreeByModuleCode(moduleCode));
        }
        return Result.success(sysMenuService.listTreeByModuleCode(null));
    }

    /**
     * 获取菜单树选项（用于新增/修改时的父菜单选择）
     * 产品智能定制模块(C)支持tenantId过滤，其他模块忽略tenantId
     */
    @GetMapping("/treeOptions")
    public Result<List<SysMenuVO>> treeOptions(@RequestParam String moduleCode,
                                                @RequestParam(required = false) Long tenantId) {
        // 产品智能定制模块使用新的按租户过滤方法
        if ("C".equals(moduleCode) && tenantId != null) {
            return Result.success(sysMenuService.getProductCustomMenuTreeOptions(tenantId));
        }
        return Result.success(sysMenuService.getMenuTreeOptions(moduleCode));
    }

    @GetMapping("/{id}")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(sysMenuService.getById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody SysMenu menu) {
        // 校验父菜单不能是自身或子菜单
        if (!sysMenuService.validateParentId(menu.getId(), menu.getParentId())) {
            return Result.error("父菜单不能选择自身或子菜单");
        }
        sysMenuService.save(menu);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody SysMenu menu) {
        // 校验父菜单不能是自身或子菜单
        if (!sysMenuService.validateParentId(menu.getId(), menu.getParentId())) {
            return Result.error("父菜单不能选择自身或子菜单");
        }
        sysMenuService.updateById(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysMenuService.deleteById(id);
        return Result.success();
    }

    /**
     * 级联删除菜单（删除菜单及其所有子菜单）
     */
    @DeleteMapping("/cascade/{id}")
    public Result<?> deleteCascade(@PathVariable Long id) {
        sysMenuService.deleteWithChildren(id);
        return Result.success();
    }

    @PutMapping("/status/{id}")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysMenuService.updateStatus(id, status);
        return Result.success();
    }

    // ==================== 产品智能定制模块接口（全局结构+租户独立状态） ====================

    /**
     * 获取产品智能定制菜单列表（合并全局结构+租户状态）
     */
    @GetMapping("/tenant-product-list")
    public Result<?> listProductCustomMenus(@RequestParam(required = false) Long tenantId) {
        return Result.success(sysMenuService.listProductCustomMenus(tenantId));
    }

    /**
     * 获取产品智能定制菜单树（合并全局结构+租户状态）
     */
    @GetMapping("/tenant-product-tree")
    public Result<?> listProductCustomMenuTree(@RequestParam(required = false) Long tenantId) {
        return Result.success(sysMenuService.listProductCustomMenuTree(tenantId));
    }

    /**
     * 获取产品智能定制菜单树选项（用于新增/修改时的父菜单选择）
     */
    @GetMapping("/tenant-product-treeOptions")
    public Result<?> getProductCustomMenuTreeOptions(@RequestParam(required = false) Long tenantId) {
        return Result.success(sysMenuService.getProductCustomMenuTreeOptions(tenantId));
    }

    /**
     * 更新产品智能定制菜单
     * 结构字段全局生效，status仅租户生效
     */
    @PostMapping("/update-product-custom")
    public Result<?> updateProductCustomMenu(@RequestBody SysMenu menu, @RequestParam Long operateTenantId) {
        try {
            sysMenuService.updateProductCustomMenu(menu, operateTenantId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除产品智能定制菜单（全局记录+所有租户状态记录）
     */
    @DeleteMapping("/delete-product-custom/{id}")
    public Result<?> deleteProductCustomMenu(@PathVariable Long id) {
        try {
            sysMenuService.deleteProductCustomMenu(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增产品智能定制菜单（全局记录+所有租户状态记录）
     */
    @PostMapping("/save-product-custom")
    public Result<?> saveProductCustomMenu(@RequestBody SysMenu menu, @RequestParam List<Long> tenantIds) {
        try {
            sysMenuService.saveProductCustomMenu(menu, tenantIds);
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
    public Result<?> initTenantMenuStatus(@RequestParam Long tenantId) {
        try {
            sysMenuService.initTenantMenuStatus(tenantId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
