package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.flow.FlowDefinitionDetailDTO;
import com.rightmanage.entity.flow.FlowDefinition;
import com.rightmanage.service.flow.FlowDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程定义控制器（扩展）
 */
@RestController
@RequestMapping("/flow/definition")
public class FlowDefinitionController {

    @Autowired
    private FlowDefinitionService flowDefinitionService;

    /**
     * 获取流程定义列表
     */
    @GetMapping("/list")
    public Result<List<FlowDefinition>> list() {
        return Result.success(flowDefinitionService.listFlowDefinition());
    }

    /**
     * 获取流程定义详情（包含节点信息）
     */
    @GetMapping("/{id}")
    public Result<FlowDefinitionDetailDTO> getById(@PathVariable Long id) {
        FlowDefinitionDetailDTO detail = flowDefinitionService.getFlowDefinitionDetail(id);
        return Result.success(detail);
    }

    /**
     * 获取当前用户可发起的流程列表
     */
    @GetMapping("/startable")
    public Result<List<FlowDefinition>> getStartableFlows(@RequestParam Long userId) {
        return Result.success(flowDefinitionService.getStartableFlows(userId));
    }

    /**
     * 保存流程定义（新建或更新）
     */
    @PostMapping("/save")
    public Result<?> save(@RequestBody FlowDefinitionDetailDTO dto, @RequestParam Long userId) {
        flowDefinitionService.saveFlowDefinition(dto, userId);
        return Result.success();
    }

    /**
     * 更新流程定义
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody FlowDefinitionDetailDTO dto) {
        flowDefinitionService.updateFlowDefinition(dto);
        return Result.success();
    }

    /**
     * 删除流程定义
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        flowDefinitionService.deleteFlowDefinition(id);
        return Result.success();
    }
}
