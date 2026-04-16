package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.service.flow.FlowInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 【新增】流程回调控制器
 * 
 * 该控制器提供外部业务模块回调的接口，用于以下场景：
 * 1. 外部业务模块执行完成后，调用回调接口通知流程引擎任务已完成
 * 2. 外部业务模块执行异常时，调用回调接口通知流程引擎任务失败
 * 3. 外部业务模块在执行过程中可以实时更新执行日志
 * 
 * 调用流程：
 * 1. 审批人点击审批 → 任务状态变为「业务执行中」(status=3)
 * 2. 流程引擎模拟调用外部业务模块，控制台打印回调接口信息
 * 3. 外部业务模块执行完成后，调用本控制器的 /complete 或 /success 接口
 * 4. 流程引擎根据回调结果将任务状态改为「已完成」(status=1)，并继续流转
 * 5. 外部业务模块可以随时调用 /log 接口更新执行日志
 */
@RestController
@RequestMapping("/flow/callback")
public class FlowCallbackController {

    @Autowired
    private FlowInstanceService flowInstanceService;

    /**
     * 【新增】外部模块回调完成接口
     * 
     * 外部业务模块执行完成后，调用此接口通知流程引擎。
     * 
     * @param callbackToken 回调令牌（审批通过后由流程引擎生成，传递给外部模块）
     * @param success 是否成功（true=业务执行成功，false=业务执行失败）
     * @param message 回调消息（可选，用于记录成功/失败原因）
     * @param extraData 额外数据（可选，JSON格式字符串，用于传递业务自定义数据）
     * @return 处理结果
     */
    @PostMapping("/complete")
    public Result<Map<String, Object>> moduleCallbackComplete(
            @RequestParam String callbackToken,
            @RequestParam Boolean success,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String extraData) {
        
        try {
            Map<String, Object> result = flowInstanceService.handleModuleCallback(
                    callbackToken, success, message, extraData);
            
            if ((Boolean) result.get("success")) {
                return Result.success(result);
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            return Result.error("回调处理异常: " + e.getMessage());
        }
    }

    /**
     * 【新增】外部模块回调成功（便捷方法）
     * 
     * 用于外部业务模块执行成功后的回调，success 固定为 true。
     */
    @PostMapping("/success")
    public Result<Map<String, Object>> moduleCallbackSuccess(
            @RequestParam String callbackToken,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String extraData) {
        
        return moduleCallbackComplete(callbackToken, true, message, extraData);
    }

    /**
     * 【新增】外部模块回调失败（便捷方法）
     * 
     * 用于外部业务模块执行失败后的回调，success 固定为 false。
     * 调用此方法后，任务状态将被重置为「逻辑处理失败」(status=4)，
     * 原审批人可以重新审批。
     */
    @PostMapping("/failed")
    public Result<Map<String, Object>> moduleCallbackFailed(
            @RequestParam String callbackToken,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String extraData) {
        
        return moduleCallbackComplete(callbackToken, false, message, extraData);
    }

    /**
     * 【新增】重置任务状态接口
     * 
     * 当任务处于「逻辑处理失败」状态时，可以调用此接口重置任务状态，
     * 使原审批人可以重新进行审批操作。
     * 
     * @param taskId 任务ID
     * @param userId 操作人ID（必须是原审批人）
     * @param reason 重置原因（可选）
     */
    @PostMapping("/reset")
    public Result<?> resetTask(
            @RequestParam String taskId,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {
        
        try {
            flowInstanceService.resetTaskStatus(taskId, userId, reason);
            return Result.success("任务已重置，可以重新审批");
        } catch (Exception e) {
            return Result.error("重置失败: " + e.getMessage());
        }
    }

    /**
     * 【新增】获取任务状态说明
     * 
     * @param status 状态码
     * @return 状态说明文本
     */
    @GetMapping("/status/text")
    public Result<String> getTaskStatusText(@RequestParam Integer status) {
        String statusText = flowInstanceService.getTaskStatusText(status);
        return Result.success(statusText);
    }

    /**
     * 【新增】模拟外部模块回调（用于测试）
     * 
     * 该接口模拟外部业务模块调用回调接口的过程，
     * 实际开发中，外部模块应自行调用回调接口。
     * 
     * 【重要说明】
     * 在生产环境中，请使用真实的回调令牌调用 /complete 或 /success/failed 接口。
     * 回调令牌在审批通过后生成，会在控制台日志中打印出来。
     */
    @PostMapping("/simulate")
    public Result<Map<String, Object>> simulateCallback(
            @RequestParam String callbackToken,
            @RequestParam Boolean success,
            @RequestParam(required = false) String message) {
        
        return moduleCallbackComplete(callbackToken, success, "[模拟] " + (message != null ? message : ""), null);
    }

    /**
     * 【新增】更新外部模块执行日志
     *
     * 外部业务模块在执行过程中，可以调用此接口实时更新执行日志，
     * 用户可以在「我的流转」「我的审批」「流程详情」页面查看执行进度。
     *
     * 示例调用：
     * POST /flow/callback/log?callbackToken=xxx&logContent=正在处理第1步...
     *
     * @param callbackToken 回调令牌
     * @param logContent 日志内容（将追加到现有日志后面）
     * @return 处理结果
     */
    @PostMapping("/log")
    public Result<Map<String, Object>> updateExecuteLog(
            @RequestParam String callbackToken,
            @RequestParam String logContent) {

        Map<String, Object> result = flowInstanceService.updateExecuteLog(callbackToken, logContent);
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }
}
