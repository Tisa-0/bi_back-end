package com.rightmanage.enums;

/**
 * 流程节点类型枚举
 */
public enum FlowNodeType {
    START("start", "开始节点"),
    APPROVE("approve", "审批节点"),
    NOTIFY("notify", "通知节点"),
    END("end", "结束节点"),
    TEXT("text", "文本节点"),
    LOGIC_AND("logic_and", "逻辑与"),
    LOGIC_OR("logic_or", "逻辑或");

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

    /**
     * 判断是否为逻辑判断节点
     */
    public static boolean isLogicNode(String code) {
        return LOGIC_AND.code.equals(code) || LOGIC_OR.code.equals(code);
    }
}
