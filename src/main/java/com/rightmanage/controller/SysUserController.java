package com.rightmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.common.Result;
import com.rightmanage.entity.SysTenant;
import com.rightmanage.entity.SysUser;
import com.rightmanage.service.SysTenantService;
import com.rightmanage.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/user")
public class SysUserController {
    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysTenantService sysTenantService;

    @GetMapping("/list")
    public Result<List<SysUser>> list() {
        return Result.success(sysUserService.list());
    }
    @GetMapping("/page")
    public Result<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String username) {
        return Result.success(sysUserService.page(pageNum, pageSize, username));
    }
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }
    @PostMapping
    public Result<?> save(@RequestBody SysUser user) {
        sysUserService.save(user);
        return Result.success();
    }
    @PutMapping
    public Result<?> update(@RequestBody SysUser user) {
        sysUserService.updateById(user);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysUserService.deleteById(id);
        return Result.success();
    }
    @PutMapping("/status/{id}")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysUserService.updateStatus(id, status);
        return Result.success();
    }
    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(
            @PathVariable Long id,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String tenantCode) {
        // 如果传了tenantCode，先查询tenantId
        if (tenantCode != null && !tenantCode.isEmpty()) {
            SysTenant tenant = sysTenantService.getByTenantCode(tenantCode);
            if (tenant != null) {
                tenantId = tenant.getId();
            }
        }
        if (moduleCode != null || tenantId != null) {
            return Result.success(sysUserService.getRoleIdsByUserId(id, moduleCode, tenantId));
        }
        return Result.success(sysUserService.getRoleIdsByUserId(id));
    }

    @PostMapping("/{id}/roles")
    public Result<?> bindRoles(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        List<?> roleIdsObj = (List<?>) params.get("roleIds");
        List<Long> roleIds = roleIdsObj.stream().map(item -> ((Number) item).longValue()).collect(Collectors.toList());
        String moduleCode = (String) params.get("moduleCode");
        Object tenantIdObj = params.get("tenantId");
        Long tenantId = tenantIdObj != null ? ((Number) tenantIdObj).longValue() : null;
        
        // 如果传了tenantCode，先查询tenantId
        String tenantCode = (String) params.get("tenantCode");
        if (tenantCode != null && !tenantCode.isEmpty() && tenantId == null) {
            SysTenant tenant = sysTenantService.getByTenantCode(tenantCode);
            if (tenant != null) {
                tenantId = tenant.getId();
            }
        }
        
        sysUserService.bindRoles(id, roleIds, moduleCode, tenantId);
        return Result.success();
    }
}
