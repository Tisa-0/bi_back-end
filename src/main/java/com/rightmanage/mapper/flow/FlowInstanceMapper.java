package com.rightmanage.mapper.flow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.flow.FlowInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FlowInstanceMapper extends BaseMapper<FlowInstance> {
    FlowInstance selectByInstanceId(@Param("instanceId") String instanceId);
}
