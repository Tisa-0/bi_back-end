package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.SysApi;
import com.rightmanage.mapper.SysApiMapper;
import com.rightmanage.service.SysApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class SysApiServiceImpl implements SysApiService {
    @Autowired
    private SysApiMapper sysApiMapper;
    @Override
    public List<SysApi> list() {
        return sysApiMapper.selectList(null);
    }
    @Override
    public IPage<SysApi> pageByModuleCode(Integer pageNum, Integer pageSize, String moduleCode) {
        Page<SysApi> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysApi> wrapper = new LambdaQueryWrapper<>();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysApi::getModuleCode, moduleCode);
        }
        wrapper.orderByDesc(SysApi::getCreateTime);
        return sysApiMapper.selectPage(page, wrapper);
    }
    @Override
    public List<SysApi> listByModuleCode(String moduleCode) {
        LambdaQueryWrapper<SysApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysApi::getModuleCode, moduleCode)
               .orderByDesc(SysApi::getCreateTime);
        return sysApiMapper.selectList(wrapper);
    }
    @Override
    public SysApi getById(Long id) {
        return sysApiMapper.selectById(id);
    }
    @Override
    public boolean save(SysApi api) {
        return sysApiMapper.insert(api) > 0;
    }
    @Override
    public boolean updateById(SysApi api) {
        return sysApiMapper.updateById(api) > 0;
    }
    @Override
    public boolean deleteById(Long id) {
        return sysApiMapper.deleteById(id) > 0;
    }
    @Override
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SysApi> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysApi::getId, id)
               .set(SysApi::getStatus, status);
        return sysApiMapper.update(null, wrapper) > 0;
    }
}
