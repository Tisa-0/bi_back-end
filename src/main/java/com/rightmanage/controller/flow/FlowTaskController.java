package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.FlowTask;
import com.rightmanage.entity.FlowTaskDTO;
import com.rightmanage.entity.FlowApproveDTO;
import com.rightmanage.service.flow.FlowTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程任务控制器
 */
@RestController
@RequestMapping("/flow/task")
public class FlowTaskController {

    @Autowired
    private FlowTaskService flowTaskService;

    /**
     * 获取我的待办任务
     */
    @GetMapping("/todo")
    public Result<List<FlowTask>> getTodoTasks(@RequestParam Long userId) {
        return Result.success(flowTaskService.getTodoTasks(userId));
    }

    /**
     * 获取我的已办任务
     */
    @GetMapping("/done")
    public Result<List<FlowTask>> getDoneTasks(@RequestParam Long userId) {
        return Result.success(flowTaskService.getDoneTasks(userId));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public Result<FlowTaskDTO> getTaskDetail(@PathVariable Long id) {
        return Result.success(flowTaskService.getTaskDetail(id));
    }

    /**
     * 审批任务
     */
    @PostMapping("/approve")
    public Result<?> approve(@RequestBody FlowApproveDTO dto, @RequestParam Long userId) {
        flowTaskService.approve(dto, userId);
        return Result.success();
    }

    /**
     * 转办任务
     */
    @PostMapping("/delegate")
    public Result<?> delegate(@RequestParam Long taskId, @RequestParam Long delegateUserId, @RequestParam Long userId) {
        flowTaskService.delegate(taskId, delegateUserId, userId);
        return Result.success();
    }
}
