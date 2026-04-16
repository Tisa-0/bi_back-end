package com.rightmanage.mapper.flow;

import com.rightmanage.entity.flow.FlowTemplateParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程模板参数Mapper
 */
@Mapper
public interface FlowTemplateParamMapper {

    /**
     * 根据模板ID查询参数列表
     */
    List<FlowTemplateParam> findByTemplateId(@Param("flowCode") String flowCode);

    /**
     * 保存参数
     */
    int save(FlowTemplateParam param);

    /**
     * 批量保存参数
     */
    int batchSave(@Param("params") List<FlowTemplateParam> params);

    /**
     * 根据ID删除参数
     */
    int deleteById(@Param("definitionParamId") String definitionParamId);

    /**
     * 根据模板ID删除所有参数
     */
    int deleteByTemplateId(@Param("flowCode") String flowCode);
}
