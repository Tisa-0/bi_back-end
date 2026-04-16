package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程操作日志实体
 */
@Data
@TableName("flow_operation_log")
public class FlowOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.INPUT)
    private String logId;

    private String instanceId;

    private Long operatorId;

    private String operatorName;

    private String operationType;

    private String operationContent;

    private Date operationTime;
}
