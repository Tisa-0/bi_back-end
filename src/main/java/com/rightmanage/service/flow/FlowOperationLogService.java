package com.rightmanage.service.flow;

import com.rightmanage.entity.flow.FlowOperationLog;
import java.util.List;

/**
 * 流程操作日志服务接口
 */
public interface FlowOperationLogService {
    /**
     * 保存操作日志
     */
    void saveLog(String instanceId, Long operatorId, String operatorName, String operationType, String operationContent);

    /**
     * 获取流程实例的操作日志列表
     */
    List<FlowOperationLog> listByInstanceId(String instanceId);
}
