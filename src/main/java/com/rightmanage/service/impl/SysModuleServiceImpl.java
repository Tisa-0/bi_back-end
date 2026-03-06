package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rightmanage.entity.SysModule;
import com.rightmanage.mapper.SysModuleMapper;
import com.rightmanage.service.SysModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class SysModuleServiceImpl implements SysModuleService {
    @Autowired
    private SysModuleMapper sysModuleMapper;

    /**
     * 固定模块列表
     */
    private static final List<SysModule> FIXED_MODULES = Arrays.asList(
            createModule("A", "BI工作台"),
            createModule("B", "灵活查询中心"),
            createModule("C", "产品智能定制")
    );

    private static SysModule createModule(String code, String name) {
        SysModule module = new SysModule();
        module.setModuleCode(code);
        module.setModuleName(name);
        return module;
    }

    @Override
    public List<SysModule> list() {
        // 先查询数据库中已存在的模块
        List<SysModule> dbModules = sysModuleMapper.selectList(null);

        // 如果数据库中没有模块，初始化固定模块到数据库
        if (dbModules == null || dbModules.isEmpty()) {
            for (SysModule module : FIXED_MODULES) {
                sysModuleMapper.insert(module);
            }
            return FIXED_MODULES;
        }

        // 合并固定模块和数据库模块，去重（以固定模块为准）
        return FIXED_MODULES.stream()
                .map(fixed -> {
                    // 检查数据库中是否已存在相同编码的模块
                    return dbModules.stream()
                            .filter(db -> db.getModuleCode().equals(fixed.getModuleCode()))
                            .findFirst()
                            .orElse(fixed);
                })
                .collect(Collectors.toList());
    }
    @Override
    public SysModule getById(Long id) {
        return sysModuleMapper.selectById(id);
    }
    @Override
    public boolean save(SysModule module) {
        return sysModuleMapper.insert(module) > 0;
    }
    @Override
    public boolean updateById(SysModule module) {
        return sysModuleMapper.updateById(module) > 0;
    }
    @Override
    public boolean deleteById(Long id) {
        return sysModuleMapper.deleteById(id) > 0;
    }
}
