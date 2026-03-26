package com.rightmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.common.Result;
import com.rightmanage.entity.SysTenant;
import com.rightmanage.entity.SysUserAsset;
import com.rightmanage.service.SysTenantService;
import com.rightmanage.service.SysUserAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/asset")
public class SysUserAssetController {

    @Autowired
    private SysUserAssetService sysUserAssetService;

    @Autowired
    private SysTenantService sysTenantService;

    /**
     * 分页查询用户资产列表（支持按资产类型+租户过滤）
     */
    @GetMapping("/list")
    public Result<IPage<SysUserAsset>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long userId,
            @RequestParam String moduleCode,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String tenantCode) {
        Long tenantId = resolveTenantId(tenantCode);
        return Result.success(sysUserAssetService.page(pageNum, pageSize, userId, moduleCode, typeId, tenantId));
    }

    /**
     * 查询用户所有资产（不分页，支持按资产类型+租户过滤）
     */
    @GetMapping("/all")
    public Result<List<SysUserAsset>> all(
            @RequestParam Long userId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String tenantCode) {
        Long tenantId = resolveTenantId(tenantCode);
        return Result.success(sysUserAssetService.list(userId, moduleCode, typeId, tenantId));
    }

    /**
     * 查询用户已绑定的资产ID列表
     */
    @GetMapping("/boundIds")
    public Result<List<Long>> boundIds(
            @RequestParam Long userId,
            @RequestParam String moduleCode,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String tenantCode) {
        Long tenantId = resolveTenantId(tenantCode);
        return Result.success(sysUserAssetService.getBoundAssetIds(userId, moduleCode, typeId, tenantId));
    }

    /**
     * 绑定单个资产给用户
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
     * 批量绑定资产给用户（支持指定 typeId 和 tenantCode）
     */
    @PostMapping("/bindBatch")
    public Result<?> bindBatch(@RequestBody java.util.Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String moduleCode = params.get("moduleCode").toString();
        @SuppressWarnings("unchecked")
        List<Long> assetIds = (List<Long>) params.get("assetIds");

        // 解析 typeId
        Long typeId = null;
        Object typeIdObj = params.get("typeId");
        if (typeIdObj != null) {
            typeId = typeIdObj instanceof Number ? ((Number) typeIdObj).longValue() : Long.valueOf(typeIdObj.toString());
        }

        // 解析 tenantCode -> tenantId
        String tenantCode = params.get("tenantCode") != null ? params.get("tenantCode").toString() : null;
        Long tenantId = resolveTenantId(tenantCode);

        sysUserAssetService.bindAssets(userId, assetIds, moduleCode, typeId, tenantId);
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
    public Result<?> unbindAll(
            @RequestParam Long userId,
            @RequestParam String moduleCode) {
        sysUserAssetService.unbindAllAssets(userId, moduleCode);
        return Result.success();
    }

    /**
     * 将 tenantCode 解析为 tenantId（Long）
     */
    private Long resolveTenantId(String tenantCode) {
        if (tenantCode == null || tenantCode.trim().isEmpty()) {
            return null;
        }
        SysTenant tenant = sysTenantService.getByTenantCode(tenantCode);
        return tenant != null ? tenant.getId() : null;
    }
}
