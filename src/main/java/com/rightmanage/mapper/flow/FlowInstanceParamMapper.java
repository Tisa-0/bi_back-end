package com.rightmanage.mapper.flow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.flow.FlowInstanceParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程实例参数Mapper
 */
@Mapper
public interface FlowInstanceParamMapper extends BaseMapper<FlowInstanceParam> {

    /**
     * 根据实例ID查询参数列表
     */
    FlowInstanceParam[] findByInstanceId(@Param("instanceId") String instanceId);

    /**
     * 根据实例ID和参数编码查询单条记录
     */
    FlowInstanceParam findByInstanceIdAndParamCode(@Param("instanceId") String instanceId, @Param("paramCode") String paramCode);

    /**
     * 批量保存实例参数
     */
    int batchSave(@Param("params") FlowInstanceParam[] params);

    /**
     * 根据实例ID删除参数
     */
    int deleteByInstanceId(@Param("instanceId") String instanceId);
}
