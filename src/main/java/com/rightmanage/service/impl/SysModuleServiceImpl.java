package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rightmanage.entity.SysModule;
import com.rightmanage.mapper.SysModuleMapper;
import com.rightmanage.service.SysModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class SysModuleServiceImpl implements SysModuleService {
    @Autowired
    private SysModuleMapper sysModuleMapper;
    @Override
    public List<SysModule> list() {
        return sysModuleMapper.selectList(null);
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
