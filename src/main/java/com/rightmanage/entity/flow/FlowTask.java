package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务实体（增强版）
 */
@Data
@TableName("flow_task")
public class FlowTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private Long handlerId;

    private String handlerName;

    private String action;

    private String comment;

    private Date executeTime;

    private Integer status;
    // 任务状态说明：
    // 0 = 待处理（初始状态，审批人待审批）
    // 1 = 已通过（审批通过）
    // 2 = 已驳回（审批驳回）
    // 3 = 业务执行中（审批通过后，正在等待外部业务模块回调）
    // 4 = 逻辑处理失败（外部模块执行异常，需要重置后重新审批）

    // 回调令牌（审批通过后异步调用外部模块时生成，用于回调验证）
    private String callbackToken;

    // 外部模块执行日志（JSON格式，支持追加）
    private String executeLog;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableLogic
    private Integer deleted;

    // 自定义字段值（JSON格式：{fieldName: "value", ...}）
    private String customFieldValues;
}
