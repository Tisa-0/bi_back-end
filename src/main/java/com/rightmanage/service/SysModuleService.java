package com.rightmanage.service;

import com.rightmanage.entity.SysModule;
import java.util.List;
public interface SysModuleService {
    List<SysModule> list();
    List<SysModule> listEnabled();
    SysModule getById(Long id);
    boolean save(SysModule module);
    boolean updateById(SysModule module);
    boolean deleteById(Long id);

    /**
     * 根据模块编码获取模块信息
     */
    SysModule getByCode(String moduleCode);

    /**
     * 判断模块是否是多租户模块
     */
    boolean isMultiTenant(String moduleCode);
}
