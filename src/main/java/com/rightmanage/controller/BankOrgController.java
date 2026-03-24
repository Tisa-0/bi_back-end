package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.dto.BankOrgTreeVO;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.service.BankOrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bank-org")
public class BankOrgController {

    @Autowired
    private BankOrgService bankOrgService;

    /**
     * 获取完整机构树
     */
    @GetMapping("/tree")
    public Result<List<BankOrgTreeVO>> getTree() {
        return Result.success(bankOrgService.getTree());
    }

    /**
     * 获取机构完整路径（总行 -> ... -> 当前机构）
     */
    @GetMapping("/path/{id}")
    public Result<List<BankOrg>> getPath(@PathVariable Long id) {
        return Result.success(bankOrgService.getPath(id));
    }

    /**
     * 获取父机构
     */
    @GetMapping("/parent/{id}")
    public Result<BankOrg> getParent(@PathVariable Long id) {
        return Result.success(bankOrgService.getParent(id));
    }

    /**
     * 获取直接子机构
     */
    @GetMapping("/children/{id}")
    public Result<List<BankOrg>> getChildren(@PathVariable Long id) {
        return Result.success(bankOrgService.getChildren(id));
    }

    /**
     * 获取所有后代机构
     */
    @GetMapping("/descendants/{id}")
    public Result<List<BankOrg>> getDescendants(@PathVariable Long id) {
        return Result.success(bankOrgService.getDescendants(id));
    }

    /**
     * 获取平铺结构
     */
    @GetMapping("/list")
    public Result<List<BankOrg>> list() {
        return Result.success(bankOrgService.getAll());
    }

    /**
     * 聚合查询（一次返回树/路径/父/子）
     */
    @GetMapping("/overview/{id}")
    public Result<Map<String, Object>> overview(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("tree", bankOrgService.getTree());
        result.put("path", bankOrgService.getPath(id));
        result.put("parent", bankOrgService.getParent(id));
        result.put("children", bankOrgService.getChildren(id));
        result.put("descendants", bankOrgService.getDescendants(id));
        return Result.success(result);
    }
}
