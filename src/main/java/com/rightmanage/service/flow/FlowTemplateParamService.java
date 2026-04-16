package com.rightmanage.service.flow;

import com.rightmanage.entity.flow.FlowTemplateParam;
import java.util.List;

/**
 * 流程模板参数服务接口
 */
public interface FlowTemplateParamService {

    /**
     * 根据模板ID获取参数列表
     */
    List<FlowTemplateParam> getParamsByTemplateId(String templateId);

    /**
     * 保存流程模板参数
     */
    void saveParams(String templateId, List<FlowTemplateParam> params);

    /**
     * 删除参数
     */
    void deleteParam(String id);
}
