package com.rightmanage.entity.flow;

import lombok.Data;

/**
 * 流程实例参数VO（用于展示）
 */
@Data
public class FlowInstanceParamVO {
    private String paramName;  // 参数名称
    private String paramValue; // 参数值
    private String paramValueLabel; // 参数值的中文翻译
}
