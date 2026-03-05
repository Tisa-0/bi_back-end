package com.rightmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.entity.SysApi;
import java.util.List;
public interface SysApiService {
    List<SysApi> list();
    IPage<SysApi> pageByModuleCode(Integer pageNum, Integer pageSize, String moduleCode);
    List<SysApi> listByModuleCode(String moduleCode);
    SysApi getById(Long id);
    boolean save(SysApi api);
    boolean updateById(SysApi api);
    boolean deleteById(Long id);
    boolean updateStatus(Long id, Integer status);
}
