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
    @GetMapping("/path/{orgcod}")
    public Result<List<BankOrg>> getPath(@PathVariable String orgcod) {
        return Result.success(bankOrgService.getPath(orgcod));
    }

    /**
     * 获取父机构
     */
    @GetMapping("/parent/{orgcod}")
    public Result<BankOrg> getParent(@PathVariable String orgcod) {
        return Result.success(bankOrgService.getParent(orgcod));
    }

    /**
     * 获取直接子机构
     */
    @GetMapping("/children/{orgcod}")
    public Result<List<BankOrg>> getChildren(@PathVariable String orgcod) {
        return Result.success(bankOrgService.getChildren(orgcod));
    }

    /**
     * 获取所有后代机构
     */
    @GetMapping("/descendants/{orgcod}")
    public Result<List<BankOrg>> getDescendants(@PathVariable String orgcod) {
        return Result.success(bankOrgService.getDescendants(orgcod));
    }

    /**
     * 按层级懒加载：查询指定机构下直接子机构
     */
    @GetMapping("/getsuborglist/{orgcod}")
    public Result<List<BankOrg>> getSubOrgList(@PathVariable String orgcod) {
        return Result.success(bankOrgService.getSubOrgList(orgcod));
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
    @GetMapping("/overview/{orgcod}")
    public Result<Map<String, Object>> overview(@PathVariable String orgcod) {
        Map<String, Object> result = new HashMap<>();
        result.put("tree", bankOrgService.getTree());
        result.put("path", bankOrgService.getPath(orgcod));
        result.put("parent", bankOrgService.getParent(orgcod));
        result.put("children", bankOrgService.getChildren(orgcod));
        result.put("descendants", bankOrgService.getDescendants(orgcod));
        return Result.success(result);
    }
}
