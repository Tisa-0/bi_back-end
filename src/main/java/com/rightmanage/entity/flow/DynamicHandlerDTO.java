package com.rightmanage.entity.flow;

import lombok.Data;

/**
 * 动态用户处理人DTO
 */
@Data
public class DynamicHandlerDTO {
    private String nodeKey; // 节点key
    private Long handlerId; // 处理人ID
    private String handlerName; // 处理人名称
}
