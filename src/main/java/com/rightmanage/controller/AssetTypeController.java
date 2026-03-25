package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.dto.AssetTypeTreeVO;
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
     * 获取树形列表（用于表格展示）
     */
    @GetMapping("/tree")
    public Result<List<AssetTypeTreeVO>> tree(@RequestParam(required = false) String moduleCode) {
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return Result.success(assetTypeService.getTreeByModuleCode(moduleCode));
        }
        return Result.success(assetTypeService.getTree());
    }

    /**
     * 获取下拉树选项（用于新增/编辑时的父级选择）
     */
    @GetMapping("/treeOptions")
    public Result<List<AssetTypeTreeVO>> treeOptions(@RequestParam(required = false) String moduleCode) {
        return Result.success(assetTypeService.getTreeOptions(moduleCode));
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
     * 级联删除（含所有子节点）
     */
    @DeleteMapping("/cascade/{id}")
    public Result<?> deleteCascade(@PathVariable Long id) {
        assetTypeService.deleteCascade(id);
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
