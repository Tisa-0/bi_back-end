package com.rightmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.common.Result;
import com.rightmanage.entity.SysApi;
import com.rightmanage.service.SysApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api")
public class SysApiController {
    @Autowired
    private SysApiService sysApiService;
    @GetMapping("/list")
    public Result<List<SysApi>> list(@RequestParam(required = false) String moduleCode) {
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return Result.success(sysApiService.listByModuleCode(moduleCode));
        }
        return Result.success(sysApiService.list());
    }
    @GetMapping("/page")
    public Result<IPage<SysApi>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String moduleCode) {
        return Result.success(sysApiService.pageByModuleCode(pageNum, pageSize, moduleCode));
    }
    @GetMapping("/{id}")
    public Result<SysApi> getById(@PathVariable Long id) {
        return Result.success(sysApiService.getById(id));
    }
    @PostMapping
    public Result<?> save(@RequestBody SysApi api) {
        sysApiService.save(api);
        return Result.success();
    }
    @PutMapping
    public Result<?> update(@RequestBody SysApi api) {
        sysApiService.updateById(api);
        return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysApiService.deleteById(id);
        return Result.success();
    }
    @PutMapping("/status/{id}")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysApiService.updateStatus(id, status);
        return Result.success();
    }
}
