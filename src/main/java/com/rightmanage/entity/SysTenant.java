package com.rightmanage.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bmip_tenant_management")
public class SysTenant implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "tenant_code")
    private String tenantCode;

    private String tenantName;
    private String orgCode;
    private String tenantAddress;
    private String commonTenantFlag;
    private Integer displayOrder;
    private String dataLakeUser;
    private String orgType;
    private String reportGrayReleaseSwitch;
    private String tenantIdxPrefix;
    private String tenantEnableSwitch;
    private String tenantSchema;
    private String tenantProject;
    private String tenantModelPrefix;
    private String tenantTablePrefix;
}
