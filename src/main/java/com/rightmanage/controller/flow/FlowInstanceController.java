package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.flow.FlowInstance;
import com.rightmanage.entity.flow.FlowStartDTO;
import com.rightmanage.entity.flow.FlowDetailVO;
import com.rightmanage.entity.flow.FlowInstanceVO;
import com.rightmanage.service.flow.FlowInstanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 获取我的流程实例（分页）
     */
    @GetMapping("/myInitiated")
    public Result<IPage<FlowInstanceVO>> getMyInstances(
            @RequestParam Long userId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long flowId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(flowInstanceService.myInitiated(userId, moduleCode, tenantId, flowId, pageNum, pageSize));
    }

    /**
     * 获取流程实例详情
     */
    @GetMapping("/detail")
    public Result<FlowDetailVO> getById(@RequestParam Long instanceId) {
        return Result.success(flowInstanceService.getFlowDetail(instanceId));
    }

    /**
     * 发起流程
     */
    @PostMapping("/start")
    public Result<Long> start(@RequestBody FlowStartDTO dto, @RequestParam Long userId) {
        Long instanceId = flowInstanceService.startFlow(dto, userId);
        return Result.success(instanceId);
    }

    /**
     * 撤回流程
     */
    @PostMapping("/cancel")
    public Result<?> cancel(@RequestParam Long instanceId, @RequestParam Long userId) {
        flowInstanceService.cancelFlow(instanceId, userId);
        return Result.success();
    }

    /**
     * 终止流程
     */
    @PostMapping("/terminate")
    public Result<?> terminate(@RequestParam Long instanceId, @RequestParam Long userId) {
        flowInstanceService.terminateFlow(instanceId, userId);
        return Result.success();
    }
}
