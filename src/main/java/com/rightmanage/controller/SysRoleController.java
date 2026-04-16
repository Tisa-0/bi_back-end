package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.entity.SysRole;
import com.rightmanage.entity.SysUser;
import com.rightmanage.service.SysRoleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @GetMapping("/page")
    public Result<IPage<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode) {
        return Result.success(sysRoleService.pageByModuleCode(pageNum, pageSize, moduleCode, tenantCode));
    }

    @GetMapping("/list")
    public Result<List<SysRole>> list(@RequestParam(required = false) String moduleCode) {
        return Result.success(sysRoleService.listByModuleCode(moduleCode));
    }

    @GetMapping("/{roleCode}")
    public Result<SysRole> getById(@PathVariable String roleCode) {
        return Result.success(sysRoleService.getById(roleCode));
    }

    @PostMapping
    public Result<?> save(@RequestBody SysRole role) {
        sysRoleService.save(role);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody SysRole role) {
        sysRoleService.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{roleCode}")
    public Result<?> delete(@PathVariable String roleCode) {
        sysRoleService.deleteById(roleCode);
        return Result.success();
    }

    // ============ 角色菜单绑定 ============

    /**
     * 获取角色的菜单ID列表
     */
    @GetMapping("/{roleId}/menus")
    public Result<List<String>> getRoleMenus(
            @PathVariable String roleId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode) {
        return Result.success(sysRoleService.getMenuIdsByRoleId(roleId, moduleCode, tenantCode));
    }

    /**
     * 绑定角色菜单
     */
    @PostMapping("/{roleId}/menus")
    public Result<?> bindMenus(
            @PathVariable String roleId,
            @RequestBody java.util.Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Object> menuIdObjects = (List<Object>) params.get("menuIds");
        List<String> menuIds = menuIdObjects != null ? menuIdObjects.stream()
            .map(String::valueOf).collect(java.util.stream.Collectors.toList()) : null;
        String moduleCode = (String) params.get("moduleCode");
        String tenantCode = (String) params.get("tenantCode");
        sysRoleService.bindMenus(roleId, menuIds, moduleCode, tenantCode);
        return Result.success();
    }

    // ============ 角色接口绑定 ============

    /**
     * 获取角色的接口ID列表
     */
    @GetMapping("/{roleId}/apis")
    public Result<List<Long>> getRoleApis(
            @PathVariable String roleId,
            @RequestParam(required = false) String moduleCode) {
        return Result.success(sysRoleService.getApiIdsByRoleId(roleId, moduleCode));
    }

    /**
     * 绑定角色接口
     */
    @PostMapping("/{roleId}/apis")
    public Result<?> bindApis(
            @PathVariable String roleId,
            @RequestBody java.util.Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Number> apiIdNumbers = (List<Number>) params.get("apiIds");
        List<Long> apiIds = apiIdNumbers != null ? apiIdNumbers.stream()
            .map(Number::longValue).collect(java.util.stream.Collectors.toList()) : null;
        String moduleCode = (String) params.get("moduleCode");
        sysRoleService.bindApis(roleId, apiIds, moduleCode);
        return Result.success();
    }

    // ============ 角色成员维护 ============

    /**
     * 获取角色的用户列表
     */
    @GetMapping("/user/list/{roleId}")
    public Result<List<SysUser>> getRoleUsers(
            @PathVariable String roleId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode) {
        return Result.success(sysRoleService.getRoleUsers(roleId, moduleCode, tenantCode));
    }

    /**
     * 获取可选用户列表（排除已绑定的）
     */
    @GetMapping("/user/optional/{roleId}")
    public Result<List<SysUser>> getOptionalUsers(
            @PathVariable String roleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tenantCode) {
        return Result.success(sysRoleService.getOptionalUsers(roleId, keyword, status, pageNum, pageSize, tenantCode));
    }

    /**
     * 批量绑定用户
     */
    @PostMapping("/user/bind")
    public Result<?> bindUsersBatch(@RequestBody java.util.Map<String, Object> params) {
        String roleId = (String) params.get("roleCode");
        @SuppressWarnings("unchecked")
        List<Number> userIdNumbers = (List<Number>) params.get("userIds");
        List<Long> userIds = userIdNumbers != null ? userIdNumbers.stream()
            .map(Number::longValue).collect(java.util.stream.Collectors.toList()) : null;
        String moduleCode = (String) params.get("moduleCode");
        String tenantCode = (String) params.get("tenantCode");
        return Result.success(sysRoleService.bindUsersBatch(roleId, userIds, moduleCode, tenantCode));
    }

    /**
     * 解除用户绑定
     */
    @PostMapping("/user/unbind")
    public Result<?> unbindUsers(@RequestBody java.util.Map<String, Object> params) {
        String roleId = (String) params.get("roleCode");
        @SuppressWarnings("unchecked")
        List<Number> userIdNumbers = (List<Number>) params.get("userIds");
        List<Long> userIds = userIdNumbers != null ? userIdNumbers.stream()
            .map(Number::longValue).collect(java.util.stream.Collectors.toList()) : null;
        String moduleCode = (String) params.get("moduleCode");
        String tenantCode = (String) params.get("tenantCode");
        return Result.success(sysRoleService.unbindUsers(roleId, userIds, moduleCode, tenantCode));
    }

    /**
     * 清除角色所有用户
     */
    @DeleteMapping("/{roleId}/users/clear")
    public Result<?> clearRoleUsers(
            @PathVariable String roleId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode) {
        sysRoleService.clearRoleUsers(roleId, moduleCode, tenantCode);
        return Result.success();
    }
}
