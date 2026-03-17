package com.rightmanage.entity.flow;

import lombok.Data;
import java.io.Serializable;

/**
 * 流程模板参数配置实体
 */
@Data
public class FlowTemplateParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private String paramCode;
    private String paramName;
    private String paramType;
    private Integer required;
    private String defaultValue;
    private String optionJson;
    private Integer sort;
}
