package com.rightmanage.mapper.flow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rightmanage.entity.flow.FlowInstanceParam;
import org.apache.ibatis.annotations.*;

/**
 * 流程实例参数Mapper
 */
@Mapper
public interface FlowInstanceParamMapper extends BaseMapper<FlowInstanceParam> {

    /**
     * 根据实例ID查询参数列表
     */
    @Select("SELECT * FROM flow_instance_param WHERE instance_id = #{instanceId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "instanceId", column = "instance_id"),
        @Result(property = "templateParamId", column = "template_param_id"),
        @Result(property = "paramCode", column = "param_code"),
        @Result(property = "paramValue", column = "param_value"),
        @Result(property = "paramValueLabel", column = "param_value_label"),
        @Result(property = "createTime", column = "create_time")
    })
    FlowInstanceParam[] findByInstanceId(Long instanceId);

    /**
     * 根据实例ID和参数编码查询单条记录
     */
    @Select("SELECT * FROM flow_instance_param WHERE instance_id = #{instanceId} AND param_code = #{paramCode} LIMIT 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "instanceId", column = "instance_id"),
        @Result(property = "templateParamId", column = "template_param_id"),
        @Result(property = "paramCode", column = "param_code"),
        @Result(property = "paramValue", column = "param_value"),
        @Result(property = "paramValueLabel", column = "param_value_label"),
        @Result(property = "createTime", column = "create_time")
    })
    FlowInstanceParam findByInstanceIdAndParamCode(Long instanceId, String paramCode);

    /**
     * 批量保存实例参数
     */
    @Insert("<script>" +
            "INSERT INTO flow_instance_param (instance_id, template_param_id, param_code, param_value, param_value_label) " +
            "VALUES " +
            "<foreach collection='params' item='item' separator=','>" +
            "(#{item.instanceId}, #{item.templateParamId}, #{item.paramCode}, #{item.paramValue}, #{item.paramValueLabel})" +
            "</foreach>" +
            "</script>")
    int batchSave(@Param("params") FlowInstanceParam[] params);

    /**
     * 根据实例ID删除参数
     */
    @Delete("DELETE FROM flow_instance_param WHERE instance_id = #{instanceId}")
    int deleteByInstanceId(Long instanceId);
}
