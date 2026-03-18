package com.rightmanage.entity.flow;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 流程发起DTO
 */
@Data
public class FlowStartDTO {
    private Long flowId;
    private String instanceName;
    private Long tenantId; // 租户ID（产品智能定制模块需要）
    private String attachmentUrl; // 凭证文件URL
    private String attachmentName; // 凭证文件名
    private Map<String, Object> params; // 流程参数
    private List<DynamicHandlerDTO> dynamicHandlers; // 动态用户处理人列表
}
