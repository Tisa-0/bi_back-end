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

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String uuid; // 前端生成的唯一标识（用于连线排序）

    private Long flowId;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private String handlerType;

    private String handlerIds;

    private String moduleCode; // 模块编码，产品智能定制模块为 "C"

    private String notifyContent;

    private Integer sort;

    // 自定义字段配置（JSON格式：[{fieldName: "englishName", fieldLabel: "中文名称"}]）
    private String customFields;
}
