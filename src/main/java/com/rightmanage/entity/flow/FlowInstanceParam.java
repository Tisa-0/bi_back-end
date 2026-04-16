package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 流程实例参数值实体
 */
@Data
@TableName("flow_instance_param")
public class FlowInstanceParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "instance_param_id", type = IdType.INPUT)
    private String instanceParamId;
    private String instanceId;
    private String definitionParamId;
    private String paramCode;
    private String paramValue;
    private String paramValueLabel; // 参数值的中文翻译
    private java.util.Date createTime;
}
