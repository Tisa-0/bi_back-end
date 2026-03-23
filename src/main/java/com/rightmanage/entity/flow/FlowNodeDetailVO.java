package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.Date;

/**
 * 节点详情VO
 */
@Data
public class FlowNodeDetailVO {
    private String nodeKey;
    private String nodeName;
    private String nodeType;
    private String handlerNames;
    private String action;
    private String comment;
    private Date executeTime;
    private String status;

    // 自定义字段配置（用于显示中文名称）
    private String customFields;

    // 自定义字段值（JSON格式：{fieldName: "value", ...}）
    private String customFieldValues;

    // 外部模块执行日志
    private String executeLog;

    // 节点层级（用于前端显示并行关系）
    private Integer level;
}
