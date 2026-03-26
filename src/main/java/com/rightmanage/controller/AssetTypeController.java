package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.entity.AssetType;
import com.rightmanage.service.AssetTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asset-type")
public class AssetTypeController {

    @Autowired
    private AssetTypeService assetTypeService;

    /**
     * 获取列表（用于表格展示）
     */
    @GetMapping("/list")
    public Result<List<AssetType>> list(@RequestParam(required = false) String moduleCode) {
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return Result.success(assetTypeService.listByModuleCode(moduleCode));
        }
        return Result.success(assetTypeService.listAll());
    }

    /**
     * 根据ID查询详情
     */
    @GetMapping("/{id}")
    public Result<AssetType> getById(@PathVariable Long id) {
        AssetType assetType = assetTypeService.getById(id);
        if (assetType == null) {
            return Result.error("记录不存在");
        }
        return Result.success(assetType);
    }

    /**
     * 新增
     */
    @PostMapping
    public Result<?> save(@RequestBody AssetType assetType) {
        // 编码唯一性校验
        if (!assetTypeService.checkCodeUnique(assetType.getTypeCode(), null)) {
            return Result.error("资产类型编码已存在，请勿重复");
        }
        assetTypeService.save(assetType);
        return Result.success();
    }

    /**
     * 更新
     */
    @PutMapping
    public Result<?> update(@RequestBody AssetType assetType) {
        // 编码唯一性校验（排除自身）
        if (!assetTypeService.checkCodeUnique(assetType.getTypeCode(), assetType.getId())) {
            return Result.error("资产类型编码已存在，请勿重复");
        }
        assetTypeService.update(assetType);
        return Result.success();
    }

    /**
     * 删除（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        assetTypeService.delete(id);
        return Result.success();
    }

    /**
     * 切换状态
     */
    @PutMapping("/status/{id}")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        assetTypeService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 检查编码唯一性
     */
    @GetMapping("/checkCode")
    public Result<Boolean> checkCode(@RequestParam String typeCode,
                                     @RequestParam(required = false) Long excludeId) {
        boolean unique = assetTypeService.checkCodeUnique(typeCode, excludeId);
        return Result.success(unique);
    }
}
