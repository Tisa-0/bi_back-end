package com.rightmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.SysModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysModuleMapper extends BaseMapper<SysModule> {
    SysModule selectByModuleCode(@Param("moduleCode") String moduleCode);
}
