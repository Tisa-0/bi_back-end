package com.rightmanage.mapper.flow;

import com.rightmanage.entity.flow.FlowTemplateParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 流程模板参数Mapper
 */
@Mapper
public interface FlowTemplateParamMapper {

    /**
     * 根据模板ID查询参数列表
     */
    @Select("SELECT * FROM flow_template_param WHERE template_id = #{templateId} ORDER BY sort ASC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "templateId", column = "template_id"),
        @Result(property = "paramCode", column = "param_code"),
        @Result(property = "paramName", column = "param_name"),
        @Result(property = "paramType", column = "param_type"),
        @Result(property = "required", column = "required"),
        @Result(property = "defaultValue", column = "default_value"),
        @Result(property = "optionJson", column = "option_json"),
        @Result(property = "sort", column = "sort")
    })
    List<FlowTemplateParam> findByTemplateId(Long templateId);

    /**
     * 保存参数
     */
    @Insert("INSERT INTO flow_template_param (template_id, param_code, param_name, param_type, required, default_value, option_json, sort) " +
            "VALUES (#{templateId}, #{paramCode}, #{paramName}, #{paramType}, #{required}, #{defaultValue}, #{optionJson}, #{sort})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(FlowTemplateParam param);

    /**
     * 批量保存参数
     */
    @Insert("<script>" +
            "INSERT INTO flow_template_param (template_id, param_code, param_name, param_type, required, default_value, option_json, sort) " +
            "VALUES " +
            "<foreach collection='params' item='item' separator=','>" +
            "(#{item.templateId}, #{item.paramCode}, #{item.paramName}, #{item.paramType}, #{item.required}, #{item.defaultValue}, #{item.optionJson}, #{item.sort})" +
            "</foreach>" +
            "</script>")
    int batchSave(@Param("params") List<FlowTemplateParam> params);

    /**
     * 根据ID删除参数
     */
    @Delete("DELETE FROM flow_template_param WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据模板ID删除所有参数
     */
    @Delete("DELETE FROM flow_template_param WHERE template_id = #{templateId}")
    int deleteByTemplateId(Long templateId);
}
