package com.rightmanage.controller;

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
