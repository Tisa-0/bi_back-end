package com.rightmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.common.Result;
import com.rightmanage.entity.SysUserAsset;
import com.rightmanage.service.SysUserAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/asset")
public class SysUserAssetController {

    @Autowired
    private SysUserAssetService sysUserAssetService;

    /**
     * 分页查询用户资产列表
     */
    @GetMapping("/list")
    public Result<IPage<SysUserAsset>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long userId,
            @RequestParam String moduleCode) {
        return Result.success(sysUserAssetService.page(pageNum, pageSize, userId, moduleCode));
    }

    /**
     * 查询用户所有资产（不分页）
     */
    @GetMapping("/all")
    public Result<List<SysUserAsset>> all(
            @RequestParam Long userId,
            @RequestParam(required = false) String moduleCode) {
        return Result.success(sysUserAssetService.list(userId, moduleCode));
    }

    /**
     * 查询用户已绑定的资产ID列表
     */
    @GetMapping("/boundIds")
    public Result<List<Long>> boundIds(
            @RequestParam Long userId,
            @RequestParam String moduleCode) {
        return Result.success(sysUserAssetService.getBoundAssetIds(userId, moduleCode));
    }

    /**
     * 绑定资产给用户
     */
    @PostMapping("/bind")
    public Result<?> bind(@RequestBody SysUserAsset userAsset) {
        boolean success = sysUserAssetService.bindAsset(
                userAsset.getUserId(), 
                userAsset.getAssetId(), 
                userAsset.getModuleCode()
        );
        if (!success) {
            return Result.error("该资产已绑定给该用户");
        }
        return Result.success();
    }

    /**
     * 批量绑定资产给用户
     */
    @PostMapping("/bindBatch")
    public Result<?> bindBatch(@RequestBody java.util.Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String moduleCode = params.get("moduleCode").toString();
        List<Long> assetIds = (List<Long>) params.get("assetIds");
        
        sysUserAssetService.bindAssets(userId, assetIds, moduleCode);
        return Result.success();
    }

    /**
     * 解除用户资产绑定
     */
    @DeleteMapping("/unbind/{id}")
    public Result<?> unbind(@PathVariable Long id) {
        sysUserAssetService.unbindAsset(id);
        return Result.success();
    }

    /**
     * 解除用户所有资产绑定
     */
    @DeleteMapping("/unbindAll")
    public Result<?> unbindAll(@RequestParam Long userId, @RequestParam String moduleCode) {
        sysUserAssetService.unbindAllAssets(userId, moduleCode);
        return Result.success();
    }
}
