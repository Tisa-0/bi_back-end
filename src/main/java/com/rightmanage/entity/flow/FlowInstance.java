package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例实体（增强版）
 */
@Data
@TableName("flow_instance")
public class FlowInstance implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long flowId;

    private String instanceName;

    private Long applicantId;

    private Long tenantId; // 租户ID（产品智能定制模块需要）

    private String currentNodeKey;

    private String currentNodeName;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;

    // 凭证信息
    private String attachmentUrl; // 凭证文件URL
    private String attachmentName; // 凭证文件名

    // 额外信息
    private String extraInfo; // 额外信息（如灰度发布链接等）

    // 动态处理人信息（JSON格式：[{nodeKey: "xxx", handlerId: 1, handlerName: "xxx"}]）
    private String dynamicHandlers;
}
