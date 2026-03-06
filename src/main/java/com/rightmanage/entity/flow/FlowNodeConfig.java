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

    private Long flowId;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private String handlerType;

    private String handlerIds;

    private String notifyContent;

    private Integer sort;
}
