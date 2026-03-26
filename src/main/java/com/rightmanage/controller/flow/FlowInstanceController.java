package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.flow.FlowInstance;
import com.rightmanage.entity.flow.FlowStartDTO;
import com.rightmanage.entity.flow.FlowDetailVO;
import com.rightmanage.entity.flow.FlowInstanceVO;
import com.rightmanage.entity.flow.FlowQueryDTO;
import com.rightmanage.entity.flow.FlowQueryResultVO;
import com.rightmanage.service.flow.FlowInstanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) Long flowId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String currentNodeKey,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(flowInstanceService.myInitiated(userId, moduleCode, tenantCode, flowId, typeCode, currentNodeKey, pageNum, pageSize));
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

    /**
     * 主动触发流程节点通知
     * @param instanceId 流程实例ID
     * @param userId 操作人ID
     * @return 通知结果
     */
    @PostMapping("/notify")
    public Result<String> triggerNotify(@RequestParam Long instanceId, @RequestParam Long userId) {
        return Result.success(flowInstanceService.triggerNodeNotify(instanceId, userId));
    }

    /**
     * 获取角色+动态用户（role_dynamic_user）节点的候选用户列表
     * @param moduleCode 模块编码
     * @param roleIds 逗号分隔的角色ID
     * @param tenantId 租户ID（可为null）
     * @param sourceOrgId 发起机构ID（可为null）
     */
    @GetMapping("/roleDynamicUsers")
    public Result<List<Map<String, Object>>> getRoleDynamicUsers(
            @RequestParam String moduleCode,
            @RequestParam String roleIds,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long sourceOrgId) {
        return Result.success(
                flowInstanceService.getRoleDynamicUsers(moduleCode, roleIds, tenantId, sourceOrgId));
    }

    /**
     * 【新增】统一查询接口
     * 根据 queryType 查询对应的数据：
     * - pending: 待办任务（taskStatus=0 的我的审批）
     * - myApproval: 我的审批（可指定 taskStatus）
     * - myInitiated: 我的流转
     *
     * 支持筛选条件：moduleCode、tenantCode、flowId、typeCode、nodeKey
     */
    @PostMapping("/query")
    public Result<FlowQueryResultVO<?>> queryFlow(@RequestBody FlowQueryDTO dto) {
        return Result.success(flowInstanceService.queryFlow(dto));
    }
}
