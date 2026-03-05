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
     * 查询可用资产列表（可绑定给用户的资产）
     */
    @GetMapping("/available")
    public Result<List<SysAsset>> available(
            @RequestParam String moduleCode,
            @RequestParam(required = false) Long userId) {
        return Result.success(sysAssetService.listAvailableAssets(moduleCode, userId));
    }

    /**
     * 根据ID查询资产
     */
    @GetMapping("/{id}")
    public Result<SysAsset> getById(@PathVariable Long id) {
        return Result.success(sysAssetService.getById(id));
    }

    /**
     * 新增资产
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
}
