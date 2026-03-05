package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.FlowInstance;
import com.rightmanage.entity.FlowInstanceDTO;
import com.rightmanage.entity.FlowStartDTO;
import com.rightmanage.service.flow.FlowInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程实例控制器
 */
@RestController
@RequestMapping("/flow/instance")
public class FlowInstanceController {

    @Autowired
    private FlowInstanceService flowInstanceService;

    /**
     * 获取流程实例列表
     */
    @GetMapping("/list")
    public Result<List<FlowInstance>> list() {
        return Result.success(flowInstanceService.list());
    }

    /**
     * 获取我的流程实例
     */
    @GetMapping("/my")
    public Result<List<FlowInstance>> getMyInstances(@RequestParam Long userId) {
        return Result.success(flowInstanceService.getMyInstances(userId));
    }

    /**
     * 获取流程实例详情
     */
    @GetMapping("/{id}")
    public Result<FlowInstanceDTO> getById(@PathVariable Long id) {
        return Result.success(flowInstanceService.getDetail(id));
    }

    /**
     * 发起流程
     */
    @PostMapping("/start")
    public Result<Long> start(@RequestBody FlowStartDTO dto, @RequestParam Long userId) {
        Long instanceId = flowInstanceService.start(dto, userId);
        return Result.success(instanceId);
    }

    /**
     * 撤回流程
     */
    @PostMapping("/cancel/{id}")
    public Result<?> cancel(@PathVariable Long id, @RequestParam Long userId) {
        flowInstanceService.cancel(id, userId);
        return Result.success();
    }
}
