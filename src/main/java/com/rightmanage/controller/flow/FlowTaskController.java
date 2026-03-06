package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.flow.FlowTask;
import com.rightmanage.entity.flow.FlowTaskVO;
import com.rightmanage.entity.flow.FlowApproveDTO;
import com.rightmanage.service.flow.FlowInstanceService;
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
    private FlowInstanceService flowInstanceService;

    /**
     * 获取我的审批任务（待处理）
     */
    @GetMapping("/myApproval")
    public Result<List<FlowTaskVO>> myApproval(@RequestParam Long userId, @RequestParam Integer taskStatus) {
        return Result.success(flowInstanceService.myApproval(userId, taskStatus));
    }

    /**
     * 审批任务
     */
    @PostMapping("/approve")
    public Result<?> approve(@RequestBody FlowApproveDTO dto) {
        flowInstanceService.approveFlow(dto.getTaskId(), dto.getAction(), dto.getComment(), dto.getUserId());
        return Result.success();
    }
}
