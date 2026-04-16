package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rightmanage.entity.flow.FlowOperationLog;
import com.rightmanage.mapper.flow.FlowOperationLogMapper;
import com.rightmanage.service.flow.FlowOperationLogService;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
public class FlowOperationLogServiceImpl extends ServiceImpl<FlowOperationLogMapper, FlowOperationLog> implements FlowOperationLogService {

    @Override
    public void saveLog(String instanceId, Long operatorId, String operatorName, String operationType, String operationContent) {
        FlowOperationLog log = new FlowOperationLog();
        log.setLogId(java.util.UUID.randomUUID().toString().replace("-", ""));
        log.setInstanceId(instanceId);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperationType(operationType);
        log.setOperationContent(operationContent);
        log.setOperationTime(new Date());
        baseMapper.insert(log);
    }

    @Override
    public List<FlowOperationLog> listByInstanceId(String instanceId) {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowOperationLog>()
                .eq(FlowOperationLog::getInstanceId, instanceId)
                .orderByAsc(FlowOperationLog::getOperationTime));
    }
}
