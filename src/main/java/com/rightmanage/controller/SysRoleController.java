package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.entity.SysRole;
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
            @RequestParam(required = false) Long tenantId) {
        return Result.success(sysRoleService.pageByModuleCode(pageNum, pageSize, moduleCode, tenantId));
    }

    @GetMapping("/list")
    public Result<List<SysRole>> list(@RequestParam(required = false) String moduleCode) {
        return Result.success(sysRoleService.listByModuleCode(moduleCode));
    }

    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(sysRoleService.getById(id));
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

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysRoleService.deleteById(id);
        return Result.success();
    }
}
