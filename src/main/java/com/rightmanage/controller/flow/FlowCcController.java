package com.rightmanage.controller.flow;

import com.rightmanage.common.Result;
import com.rightmanage.entity.FlowCcRecord;
import com.rightmanage.mapper.flow.FlowCcRecordMapper;
import com.rightmanage.service.SysUserService;
import com.rightmanage.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程抄送控制器
 */
@RestController
@RequestMapping("/flow/cc")
public class FlowCcController {

    @Autowired
    private FlowCcRecordMapper flowCcRecordMapper;
    
    @Autowired
    private SysUserService sysUserService;

    /**
     * 获取抄送给我的记录
     */
    @GetMapping("/my")
    public Result<List<FlowCcRecord>> getMyCcRecords(@RequestParam Long userId) {
        return Result.success(flowCcRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FlowCcRecord>()
                .eq(FlowCcRecord::getCcUserId, userId)
                .orderByDesc(FlowCcRecord::getCreateTime)
        ));
    }

    /**
     * 标记为已读
     */
    @PutMapping("/read/{id}")
    public Result<?> markAsRead(@PathVariable Long id) {
        FlowCcRecord record = flowCcRecordMapper.selectById(id);
        if (record != null) {
            record.setIsRead(1);
            flowCcRecordMapper.updateById(record);
        }
        return Result.success();
    }
}
