package com.rightmanage.enums;

/**
 * 流程实例状态枚举
 */
public enum FlowInstanceStatus {
    RUNNING(0, "运行中"),
    COMPLETED(1, "已完成"),
    REJECTED(2, "已驳回"),
    CANCELED(3, "已撤销"),
    TERMINATED(4, "已终止");

    private final Integer code;
    private final String name;

    FlowInstanceStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FlowInstanceStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlowInstanceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
