package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 流程节点配置实体
 */
@Data
@TableName("flow_node_config")
public class FlowNodeConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "node_id", type = IdType.INPUT)
    private String nodeId;

    private String flowCode;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private String handlerType;

    private String handlerIds;

    private String moduleCode; // 模块编码，产品智能定制模块为 "bi_wx_product"

    private String notifyContent;

    private Integer sort;

    // 自定义字段配置（JSON格式：[{fieldName: "englishName", fieldLabel: "中文名称"}]）
    private String customFields;

    private String executeModules; // 审批后需要调用的模块编码，多个用逗号分隔

    // 是否开启通知（0否，1是，仅approve节点生效）
    private String enableNotify;

    // 通知方式（畅聊/邮件/知会，仅enable_notify=1时生效）
    private String notifyType;
}
