package com.rightmanage.mapper.flow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.flow.FlowDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FlowDefinitionMapper extends BaseMapper<FlowDefinition> {
    FlowDefinition selectByFlowCode(@Param("flowCode") String flowCode);
}
