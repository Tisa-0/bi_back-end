package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.flow.FlowTask;
import com.rightmanage.entity.flow.FlowTaskVO;
import com.rightmanage.entity.flow.FlowApproveDTO;
import com.rightmanage.service.flow.FlowInstanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 获取待办任务列表（分页）
     */
    @GetMapping("/pending")
    public Result<IPage<FlowTaskVO>> pending(
            @RequestParam Long userId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) Long flowId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String nodeKey,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // taskStatus = 0 表示待处理
        return Result.success(flowInstanceService.myApproval(userId, 0, moduleCode, tenantCode, flowId, typeCode, nodeKey, pageNum, pageSize));
    }

    /**
     * 获取我的审批任务（待处理/已处理，分页）
     */
    @GetMapping("/myApproval")
    public Result<IPage<FlowTaskVO>> myApproval(
            @RequestParam Long userId,
            @RequestParam Integer taskStatus,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) Long flowId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String nodeKey,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(flowInstanceService.myApproval(userId, taskStatus, moduleCode, tenantCode, flowId, typeCode, nodeKey, pageNum, pageSize));
    }

    /**
     * 审批任务
     */
    @PostMapping("/approve")
    public Result<?> approve(@RequestBody FlowApproveDTO dto) {
        flowInstanceService.approveFlow(dto.getTaskId(), dto.getAction(), dto.getComment(), dto.getUserId());
        return Result.success();
    }

    /**
     * 【管理员】查询所有在途流程任务（不分用户权限限制）
     * 用于流程管理后台页面，管理员可查看/审批所有流程任务
     */
    @GetMapping("/admin/all")
    public Result<IPage<FlowTaskVO>> adminAllTasks(
            @RequestParam(required = false) Integer taskStatus,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) Long flowId,
            @RequestParam(required = false) String flowCode,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String nodeKey,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(flowInstanceService.adminAllTasks(
                taskStatus, moduleCode, tenantCode, flowId, flowCode, typeCode, nodeKey, pageNum, pageSize));
    }
}
