package com.rightmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.common.Result;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.entity.SysUser;
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
    public Result<List<String>> getUserRoles(
            @PathVariable Long id,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode) {
        if (moduleCode != null || tenantCode != null) {
            return Result.success(sysUserService.getRoleIdsByUserId(id, moduleCode, tenantCode));
        }
        return Result.success(sysUserService.getRoleIdsByUserId(id));
    }

    @PostMapping("/{id}/roles")
    public Result<?> bindRoles(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> roleCodes = (List<String>) params.get("roleCodes");
        String moduleCode = (String) params.get("moduleCode");
        String tenantCode = (String) params.get("tenantCode");
        sysUserService.bindRoles(id, roleCodes, moduleCode, tenantCode);
        return Result.success();
    }

    @GetMapping("/{id}/org-auth")
    public Result<BankOrg> getUserOrgAuth(
            @PathVariable Long id,
            @RequestParam String moduleCode,
            @RequestParam(required = false) String tenantCode) {
        return Result.success(sysUserService.getAuthorizedOrg(id, moduleCode, tenantCode));
    }

    @PostMapping("/{id}/org-auth")
    public Result<?> bindUserOrgAuth(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        String moduleCode = (String) params.get("moduleCode");
        String tenantCode = (String) params.get("tenantCode");
        Object orgIdObj = params.get("orgId");
        String orgId = orgIdObj == null ? null : String.valueOf(orgIdObj);

        if ((moduleCode == null || moduleCode.isEmpty()) || orgId == null || orgId.trim().isEmpty()) {
            return Result.error("moduleCode和orgId不能为空");
        }

        sysUserService.bindAuthorizedOrg(id, moduleCode, tenantCode, orgId);
        return Result.success();
    }
}
