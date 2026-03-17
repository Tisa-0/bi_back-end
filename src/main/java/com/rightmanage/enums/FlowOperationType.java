package com.rightmanage.enums;

/**
 * 流程操作类型枚举
 */
public enum FlowOperationType {
    INIT("init", "流程发起"),
    APPROVE("approve", "审批通过"),
    REJECT("reject", "审批驳回"),
    NOTIFY("notify", "自动通知"),
    PASS("pass", "自动通过"),
    CANCEL("cancel", "流程撤销"),
    TERMINATE("terminate", "流程终止"),
    COMPLETE("complete", "流程完成");

    private final String code;
    private final String name;

    FlowOperationType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FlowOperationType fromCode(String code) {
        for (FlowOperationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
