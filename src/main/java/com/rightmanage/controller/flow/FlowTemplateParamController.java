package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.flow.FlowTemplateParam;
import com.rightmanage.service.flow.FlowTemplateParamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程模板参数控制器
 */
@RestController
@RequestMapping("/flow/template")
public class FlowTemplateParamController {

    @Autowired
    private FlowTemplateParamService flowTemplateParamService;

    /**
     * 获取流程模板参数列表
     */
    @GetMapping("/params")
    public Result<List<FlowTemplateParam>> getParams(@RequestParam String templateId) {
        List<FlowTemplateParam> params = flowTemplateParamService.getParamsByTemplateId(templateId);
        return Result.success(params);
    }

    /**
     * 保存流程模板参数
     */
    @PostMapping("/params")
    public Result<?> saveParams(@RequestParam String templateId, @RequestBody List<FlowTemplateParam> params) {
        flowTemplateParamService.saveParams(templateId, params);
        return Result.success();
    }

    /**
     * 删除流程模板参数
     */
    @DeleteMapping("/param/{id}")
    public Result<?> deleteParam(@PathVariable String id) {
        flowTemplateParamService.deleteParam(id);
        return Result.success();
    }
}
