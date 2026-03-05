package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.FlowDefinitionDetailDTO;
import com.rightmanage.entity.FlowDefinition;
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
        return Result.success(flowDefinitionService.list());
    }

    /**
     * 获取流程定义详情（包含节点信息）
     */
    @GetMapping("/{id}")
    public Result<FlowDefinitionDetailDTO> getById(@PathVariable Long id) {
        FlowDefinitionDetailDTO detail = flowDefinitionService.getDetailById(id);
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
    @PostMapping
    public Result<?> save(@RequestBody FlowDefinitionDetailDTO dto) {
        flowDefinitionService.saveDetail(dto);
        return Result.success();
    }

    /**
     * 更新流程定义
     */
    @PutMapping
    public Result<?> update(@RequestBody FlowDefinitionDetailDTO dto) {
        flowDefinitionService.updateDetail(dto);
        return Result.success();
    }

    /**
     * 删除流程定义
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        flowDefinitionService.delete(id);
        return Result.success();
    }

    /**
     * 发布流程定义
     */
    @PostMapping("/publish/{id}")
    public Result<?> publish(@PathVariable Long id) {
        flowDefinitionService.publish(id);
        return Result.success();
    }

    /**
     * 禁用流程定义
     */
    @PostMapping("/disable/{id}")
    public Result<?> disable(@PathVariable Long id) {
        flowDefinitionService.disable(id);
        return Result.success();
    }
}
