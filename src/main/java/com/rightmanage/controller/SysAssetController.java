package com.rightmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.common.Result;
import com.rightmanage.entity.SysAsset;
import com.rightmanage.service.SysAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asset")
public class SysAssetController {

    @Autowired
    private SysAssetService sysAssetService;

    /**
     * 分页查询资产列表
     */
    @GetMapping("/page")
    public Result<IPage<SysAsset>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String keyword) {
        return Result.success(sysAssetService.page(pageNum, pageSize, moduleCode, keyword));
    }

    /**
     * 查询可用资产列表（可绑定给用户的资产，支持按 typeId / tenantCode 过滤）
     */
    @GetMapping("/available")
    public Result<List<SysAsset>> available(
            @RequestParam String moduleCode,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) Long userId) {
        return Result.success(sysAssetService.listAvailableAssets(moduleCode, typeId, tenantCode, userId));
    }

    /**
     * 根据ID查询资产
     */
    @GetMapping("/{id}")
    public Result<SysAsset> getById(@PathVariable Long id) {
        return Result.success(sysAssetService.getById(id));
    }

    /**
     * 新增资产（通用接口，/asset 走此方法）
     */
    @PostMapping
    public Result<?> add(@RequestBody SysAsset asset) {
        if (!sysAssetService.isAssetCodeUnique(asset.getModuleCode(), asset.getAssetCode(), null)) {
            return Result.error("资产编码在该模块下已存在");
        }
        sysAssetService.save(asset);
        return Result.success();
    }

    /**
     * 修改资产
     */
    @PutMapping
    public Result<?> edit(@RequestBody SysAsset asset) {
        if (!sysAssetService.isAssetCodeUnique(asset.getModuleCode(), asset.getAssetCode(), asset.getId())) {
            return Result.error("资产编码在该模块下已存在");
        }
        sysAssetService.updateById(asset);
        return Result.success();
    }

    /**
     * 删除资产
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysAssetService.deleteById(id);
        return Result.success();
    }

    /**
     * 分页查询指定模块+资产类型下已分配的资产
     * GET /asset/allocated/page?moduleCode=&typeId=&tenantCode=&pageNum=1&pageSize=10
     */
    @GetMapping("/allocated/page")
    public Result<IPage<SysAsset>> pageAllocatedAssets(
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(sysAssetService.pageAllocatedAssets(
                pageNum, pageSize, moduleCode, typeId, tenantCode));
    }

    /**
     * 新增分配资产
     * POST /asset/allocate
     */
    @PostMapping("/allocate")
    public Result<?> allocate(@RequestBody SysAsset asset) {
        if (!sysAssetService.isAssetCodeUnique(asset.getModuleCode(), asset.getAssetCode(), null)) {
            return Result.error("资产编码在该模块下已存在");
        }
        sysAssetService.saveAllocatedAsset(asset);
        return Result.success();
    }

    /**
     * 删除已分配的资产
     * DELETE /asset/allocated/{id}
     */
    @DeleteMapping("/allocated/{id}")
    public Result<?> deleteAllocated(@PathVariable Long id) {
        sysAssetService.deleteAllocatedAsset(id);
        return Result.success();
    }
}
