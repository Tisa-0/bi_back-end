package com.rightmanage.enums;

/**
 * 流程节点类型枚举
 */
public enum FlowNodeType {
    START("start", "开始节点"),
    APPROVE("approve", "审批节点"),
    NOTIFY("notify", "通知节点"),
    END("end", "结束节点");

    private final String code;
    private final String name;

    FlowNodeType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FlowNodeType fromCode(String code) {
        for (FlowNodeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
