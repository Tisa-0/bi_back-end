package com.rightmanage.entity.flow;

import lombok.Data;
import java.io.Serializable;

/**
 * 流程实例参数值实体
 */
@Data
public class FlowInstanceParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long instanceId;
    private Long templateParamId;
    private String paramCode;
    private String paramValue;
    private String paramValueLabel; // 参数值的中文翻译
}
