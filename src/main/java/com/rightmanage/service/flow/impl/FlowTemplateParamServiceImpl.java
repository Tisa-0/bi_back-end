package com.rightmanage.service.flow.impl;

import com.rightmanage.entity.flow.FlowTemplateParam;
import com.rightmanage.mapper.flow.FlowTemplateParamMapper;
import com.rightmanage.service.flow.FlowTemplateParamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

/**
 * 流程模板参数服务实现
 */
@Service
public class FlowTemplateParamServiceImpl implements FlowTemplateParamService {

    @Autowired
    private FlowTemplateParamMapper flowTemplateParamMapper;

    @Override
    public List<FlowTemplateParam> getParamsByTemplateId(String templateId) {
        return flowTemplateParamMapper.findByTemplateId(templateId);
    }

    @Override
    @Transactional
    public void saveParams(String templateId, List<FlowTemplateParam> params) {
        // 先删除原有参数
        flowTemplateParamMapper.deleteByTemplateId(templateId);
        // 批量保存新参数
        if (params != null && !params.isEmpty()) {
            for (FlowTemplateParam param : params) {
                param.setFlowCode(templateId);
                if (param.getDefinitionParamId() == null || param.getDefinitionParamId().trim().isEmpty()) {
                    param.setDefinitionParamId(UUID.randomUUID().toString().replace("-", ""));
                }
            }
            flowTemplateParamMapper.batchSave(params);
        }
    }

    @Override
    @Transactional
    public void deleteParam(String id) {
        flowTemplateParamMapper.deleteById(id);
    }
}
