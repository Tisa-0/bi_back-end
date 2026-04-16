package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 流程模板参数配置实体
 */
@Data
@TableName("flow_definition_param")
public class FlowTemplateParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "definition_param_id", type = IdType.INPUT)
    private String definitionParamId;
    private String flowCode;
    private String paramCode;
    private String paramName;
    private String paramType;
    private Integer required;
    private String defaultValue;
    private String optionJson;
    private Integer sort;
}
