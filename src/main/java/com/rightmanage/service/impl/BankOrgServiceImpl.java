package com.rightmanage.service.impl;

import com.rightmanage.dto.BankOrgTreeVO;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.mapper.BankOrgMapper;
import com.rightmanage.service.BankOrgService;
import com.rightmanage.util.BankOrgTreeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    public List<BankOrg> getPath(String orgcod) {
        List<BankOrg> all = getAll();
        Map<String, BankOrg> byId = BankOrgTreeUtils.toByIdMap(all);
        if (!byId.containsKey(orgcod)) {
            return Collections.emptyList();
        }
        return BankOrgTreeUtils.getPath(orgcod, byId);
    }

    @Override
    public BankOrg getParent(String orgcod) {
        List<BankOrg> all = getAll();
        Map<String, BankOrg> byId = BankOrgTreeUtils.toByIdMap(all);
        return BankOrgTreeUtils.getParent(orgcod, byId);
    }

    @Override
    public List<BankOrg> getChildren(String orgcod) {
        List<BankOrg> all = getAll();
        Map<String, List<BankOrg>> childrenMap = BankOrgTreeUtils.toChildrenMap(all);
        return BankOrgTreeUtils.getChildren(orgcod, childrenMap);
    }

    @Override
    public List<BankOrg> getDescendants(String orgcod) {
        List<BankOrg> all = getAll();
        Map<String, List<BankOrg>> childrenMap = BankOrgTreeUtils.toChildrenMap(all);
        return BankOrgTreeUtils.getDescendants(orgcod, childrenMap);
    }

    @Override
    public List<BankOrg> getSubOrgList(String orgcod) {
        return bankOrgMapper.selectSubOrgList(orgcod, today());
    }

    @Override
    public List<BankOrg> getAll() {
        return bankOrgMapper.selectActiveAll(today());
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}
