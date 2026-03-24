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
     * 固定模块编码列表（不允许修改、删除）
     */
    private static final List<String> FIXED_MODULE_CODES = Arrays.asList(
            "bi_workstation",
            "bi_wx_product"
    );

    /**
     * 固定模块列表（初始数据）
     */
    private static final List<SysModule> FIXED_MODULES = Arrays.asList(
            createModule("bi_workstation", "BI工作台", 0),
            createModule("B", "灵活查询中心", 0),
            createModule("bi_wx_product", "产品智能定制", 1)
    );

    private static SysModule createModule(String code, String name, Integer multiTenant) {
        SysModule module = new SysModule();
        module.setModuleCode(code);
        module.setModuleName(name);
        module.setMultiTenant(multiTenant);
        module.setStatus(1);
        // 不设置ID，让数据库自动生成，避免删除后重新插入时ID冲突
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
            return sysModuleMapper.selectList(null);
        }

        // 获取数据库中已有的模块编码
        List<String> existingCodes = dbModules.stream()
                .map(SysModule::getModuleCode)
                .collect(Collectors.toList());

        // 只补充在 FIXED_MODULE_CODES 中的固定模块（不可删除的模块）
        for (String fixedCode : FIXED_MODULE_CODES) {
            if (!existingCodes.contains(fixedCode)) {
                // 固定模块在数据库中不存在，需要插入
                // 创建新对象，清除ID，确保数据库自动生成
                SysModule toInsert = new SysModule();
                toInsert.setModuleCode(fixedCode);
                // 根据编码设置默认名称
                toInsert.setModuleName(getDefaultModuleName(fixedCode));
                toInsert.setMultiTenant(fixedCode.equals("bi_wx_product") ? 1 : 0);
                toInsert.setStatus(1);
                sysModuleMapper.insert(toInsert);
            }
        }

        // 返回最新的数据库数据
        return sysModuleMapper.selectList(null);
    }

    /**
     * 根据模块编码获取默认名称
     */
    private String getDefaultModuleName(String code) {
        switch (code) {
            case "bi_workstation":
                return "BI工作台";
            case "bi_wx_product":
                return "产品智能定制";
            default:
                return code;
        }
    }

    @Override
    public List<SysModule> listEnabled() {
        LambdaQueryWrapper<SysModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysModule::getStatus, 1);
        queryWrapper.orderByAsc(SysModule::getId);
        List<SysModule> dbModules = sysModuleMapper.selectList(queryWrapper);

        // 如果数据库中没有模块，初始化固定模块
        if (dbModules == null || dbModules.isEmpty()) {
            for (String fixedCode : FIXED_MODULE_CODES) {
                SysModule toInsert = new SysModule();
                toInsert.setModuleCode(fixedCode);
                toInsert.setModuleName(getDefaultModuleName(fixedCode));
                toInsert.setMultiTenant(fixedCode.equals("bi_wx_product") ? 1 : 0);
                toInsert.setStatus(1);
                sysModuleMapper.insert(toInsert);
            }
            return sysModuleMapper.selectList(queryWrapper);
        }

        return dbModules;
    }

    @Override
    public SysModule getById(Long id) {
        return sysModuleMapper.selectById(id);
    }

    @Override
    public boolean save(SysModule module) {
        // 校验模块编码格式（只能包含英文字母、数字和下划线，且不能以数字开头）
        if (module.getModuleCode() == null || !module.getModuleCode().matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new RuntimeException("模块编码只能包含英文字母、数字和下划线，且不能以数字开头");
        }
        // 检查编码是否已存在
        SysModule existing = getByCode(module.getModuleCode());
        if (existing != null) {
            throw new RuntimeException("模块编码已存在");
        }
        // 设置默认值
        if (module.getMultiTenant() == null) {
            module.setMultiTenant(0);
        }
        if (module.getStatus() == null) {
            module.setStatus(1);
        }
        return sysModuleMapper.insert(module) > 0;
    }

    @Override
    public boolean isMultiTenant(String moduleCode) {
        SysModule module = getByCode(moduleCode);
        return module != null && module.getMultiTenant() != null && module.getMultiTenant() == 1;
    }

    @Override
    public SysModule getByCode(String moduleCode) {
        LambdaQueryWrapper<SysModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysModule::getModuleCode, moduleCode);
        return sysModuleMapper.selectOne(queryWrapper);
    }

    /**
     * 判断模块编码是否为固定模块（不允许修改、删除）
     */
    private boolean isFixedModule(String moduleCode) {
        return FIXED_MODULE_CODES.contains(moduleCode);
    }

    /**
     * 检查是否为固定模块，如果是则抛出异常
     */
    private void checkFixedModule(String moduleCode) {
        if (isFixedModule(moduleCode)) {
            throw new RuntimeException("固定模块（" + moduleCode + "）不允许修改或删除");
        }
    }

    @Override
    public boolean updateById(SysModule module) {
        // 校验模块编码格式（只能包含英文字母、数字和下划线，且不能以数字开头）
        if (module.getModuleCode() != null && !module.getModuleCode().matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new RuntimeException("模块编码只能包含英文字母、数字和下划线，且不能以数字开头");
        }
        if (module.getModuleCode() != null) {
            checkFixedModule(module.getModuleCode());
        }
        // 检查原模块是否为固定模块
        if (module.getId() != null) {
            SysModule existing = sysModuleMapper.selectById(module.getId());
            if (existing != null && isFixedModule(existing.getModuleCode())) {
                throw new RuntimeException("固定模块（" + existing.getModuleCode() + "）不允许修改");
            }
        }
        return sysModuleMapper.updateById(module) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        SysModule existing = sysModuleMapper.selectById(id);
        if (existing != null && isFixedModule(existing.getModuleCode())) {
            throw new RuntimeException("固定模块（" + existing.getModuleCode() + "）不允许删除");
        }
        return sysModuleMapper.deleteById(id) > 0;
    }
}
