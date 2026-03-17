package com.rightmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.common.Result;
import com.rightmanage.entity.SysTenant;
import com.rightmanage.service.SysTenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenant")
public class SysTenantController {

    @Autowired
    private SysTenantService sysTenantService;

    @GetMapping("/list")
    public Result<List<SysTenant>> list(@RequestParam(required = false) String moduleCode) {
        return Result.success(sysTenantService.listByModuleCode(moduleCode));
    }

    @GetMapping("/page")
    public Result<IPage<SysTenant>> page(
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(sysTenantService.page(moduleCode, tenantName, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<SysTenant> getById(@PathVariable Long id) {
        return Result.success(sysTenantService.getById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody SysTenant tenant) {
        return Result.success(sysTenantService.save(tenant));
    }

    @PutMapping
    public Result<?> update(@RequestBody SysTenant tenant) {
        return Result.success(sysTenantService.updateById(tenant));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return Result.success(sysTenantService.deleteById(id));
    }
}
