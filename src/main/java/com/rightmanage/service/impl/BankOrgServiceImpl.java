package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rightmanage.dto.BankOrgTreeVO;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.mapper.BankOrgMapper;
import com.rightmanage.service.BankOrgService;
import com.rightmanage.util.BankOrgTreeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class BankOrgServiceImpl implements BankOrgService {

    @Autowired
    private BankOrgMapper bankOrgMapper;

    @Override
    public List<BankOrgTreeVO> getTree() {
        List<BankOrg> all = getAll();
        return BankOrgTreeUtils.buildTree(all);
    }

    @Override
    public List<BankOrg> getPath(Long id) {
        List<BankOrg> all = getAll();
        Map<Long, BankOrg> byId = BankOrgTreeUtils.toByIdMap(all);
        if (!byId.containsKey(id)) {
            return Collections.emptyList();
        }
        return BankOrgTreeUtils.getPath(id, byId);
    }

    @Override
    public BankOrg getParent(Long id) {
        List<BankOrg> all = getAll();
        Map<Long, BankOrg> byId = BankOrgTreeUtils.toByIdMap(all);
        return BankOrgTreeUtils.getParent(id, byId);
    }

    @Override
    public List<BankOrg> getChildren(Long id) {
        List<BankOrg> all = getAll();
        Map<Long, List<BankOrg>> childrenMap = BankOrgTreeUtils.toChildrenMap(all);
        return BankOrgTreeUtils.getChildren(id, childrenMap);
    }

    @Override
    public List<BankOrg> getDescendants(Long id) {
        List<BankOrg> all = getAll();
        Map<Long, List<BankOrg>> childrenMap = BankOrgTreeUtils.toChildrenMap(all);
        return BankOrgTreeUtils.getDescendants(id, childrenMap);
    }

    @Override
    public List<BankOrg> getAll() {
        return bankOrgMapper.selectList(new LambdaQueryWrapper<BankOrg>()
                .eq(BankOrg::getStatus, 1)
                .orderByAsc(BankOrg::getLevel)
                .orderByAsc(BankOrg::getSort)
                .orderByAsc(BankOrg::getId));
    }
}
