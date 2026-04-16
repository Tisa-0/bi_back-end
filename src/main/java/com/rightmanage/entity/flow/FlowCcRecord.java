package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程抄送记录实体
 */
@Data
public class FlowCcRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long processInstanceId;

    private String nodeId;

    private String nodeName;

    // 被抄送人（关联主项目sys_user）
    private Long ccUserId;

    private String ccUserName;

    // 抄送人（关联主项目sys_user）
    private Long senderId;

    private String senderName;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Integer isRead;
}
