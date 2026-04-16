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
    @GetMapping("/{flowCode}")
    public Result<FlowDefinitionDetailDTO> getById(@PathVariable String flowCode) {
        FlowDefinitionDetailDTO detail = flowDefinitionService.getFlowDefinitionDetail(flowCode);
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
    @DeleteMapping("/{flowCode}")
    public Result<?> delete(@PathVariable String flowCode) {
        flowDefinitionService.deleteFlowDefinition(flowCode);
        return Result.success();
    }

    /**
     * 检查流程编码是否已存在（用于前端实时校验或保存前校验）
     * @param flowCode 流程编码
     * @param excludeFlowCode 排除的流程编码（编辑时传入，保存时可不传或传null）
     */
    @GetMapping("/checkCode")
    public Result<Boolean> checkFlowCode(@RequestParam String flowCode,
                                          @RequestParam(required = false) String excludeFlowCode) {
        boolean exists = flowDefinitionService.checkFlowCodeExists(flowCode, excludeFlowCode);
        return Result.success(exists);
    }
}
