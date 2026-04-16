package com.rightmanage.mapper.flow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.flow.FlowNodeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowNodeConfigMapper extends BaseMapper<FlowNodeConfig> {
    List<FlowNodeConfig> selectByFlowCode(@Param("flowCode") String flowCode);
}
