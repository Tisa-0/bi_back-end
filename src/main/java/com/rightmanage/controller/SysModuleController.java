package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.entity.SysModule;
import com.rightmanage.service.SysModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/module")
public class SysModuleController {
    @Autowired
    private SysModuleService sysModuleService;
    @GetMapping("/list")
    public Result<List<SysModule>> list() {
        return Result.success(sysModuleService.list());
    }
    @GetMapping("/{id}")
    public Result<SysModule> getById(@PathVariable Long id) {
        return Result.success(sysModuleService.getById(id));
    }
    @PostMapping
    public Result<?> save(@RequestBody SysModule module) {
        sysModuleService.save(module);
        return Result.success();
    }
    @PutMapping
    public Result<?> update(@RequestBody SysModule module) {
        sysModuleService.updateById(module);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysModuleService.deleteById(id);
        return Result.success();
    }
}
