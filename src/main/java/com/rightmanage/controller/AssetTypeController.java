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
     * 根据编码查询详情
     */
    @GetMapping("/{typeCode}")
    public Result<AssetType> getById(@PathVariable String typeCode) {
        AssetType assetType = assetTypeService.getByTypeCode(typeCode);
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
        if (!assetTypeService.checkCodeUnique(assetType.getTypeCode(), assetType.getTypeCode())) {
            return Result.error("资产类型编码已存在，请勿重复");
        }
        assetTypeService.update(assetType);
        return Result.success();
    }

    /**
     * 删除（逻辑删除）
     */
    @DeleteMapping("/{typeCode}")
    public Result<?> delete(@PathVariable String typeCode) {
        assetTypeService.delete(typeCode);
        return Result.success();
    }

    /**
     * 切换状态
     */
    @PutMapping("/status/{typeCode}")
    public Result<?> updateStatus(@PathVariable String typeCode, @RequestParam Integer status) {
        assetTypeService.updateStatus(typeCode, status);
        return Result.success();
    }

    /**
     * 检查编码唯一性
     */
    @GetMapping("/checkCode")
    public Result<Boolean> checkCode(@RequestParam String typeCode,
                                     @RequestParam(required = false) String excludeTypeCode) {
        boolean unique = assetTypeService.checkCodeUnique(typeCode, excludeTypeCode);
        return Result.success(unique);
    }
}
