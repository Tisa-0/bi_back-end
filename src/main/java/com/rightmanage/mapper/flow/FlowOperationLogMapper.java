package com.rightmanage.mapper.flow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.flow.FlowOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowOperationLogMapper extends BaseMapper<FlowOperationLog> {
    List<FlowOperationLog> selectByInstanceId(@Param("instanceId") String instanceId);
}
