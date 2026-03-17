package com.rightmanage.service.flow.impl;

import com.rightmanage.entity.flow.FlowTemplateParam;
import com.rightmanage.mapper.flow.FlowTemplateParamMapper;
import com.rightmanage.service.flow.FlowTemplateParamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 流程模板参数服务实现
 */
@Service
public class FlowTemplateParamServiceImpl implements FlowTemplateParamService {

    @Autowired
    private FlowTemplateParamMapper flowTemplateParamMapper;

    @Override
    public List<FlowTemplateParam> getParamsByTemplateId(Long templateId) {
        return flowTemplateParamMapper.findByTemplateId(templateId);
    }

    @Override
    @Transactional
    public void saveParams(Long templateId, List<FlowTemplateParam> params) {
        // 先删除原有参数
        flowTemplateParamMapper.deleteByTemplateId(templateId);
        // 批量保存新参数
        if (params != null && !params.isEmpty()) {
            for (FlowTemplateParam param : params) {
                param.setTemplateId(templateId);
            }
            flowTemplateParamMapper.batchSave(params);
        }
    }

    @Override
    @Transactional
    public void deleteParam(Long id) {
        flowTemplateParamMapper.deleteById(id);
    }
}
