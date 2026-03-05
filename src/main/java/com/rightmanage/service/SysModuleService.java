package com.rightmanage.service;

import com.rightmanage.entity.SysModule;
import java.util.List;
public interface SysModuleService {
    List<SysModule> list();
    SysModule getById(Long id);
    boolean save(SysModule module);
    boolean updateById(SysModule module);
    boolean deleteById(Long id);
}
