-- =====================================================
-- 流程管理增强表结构
-- 核心flow_definition/flow_node_config/flow表：_task/flow_operation_log
-- =====================================================

-- 1. 流程定义表（核心，存储节点配置）
CREATE TABLE IF NOT EXISTS `flow_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流程ID',
  `flow_name` varchar(100) NOT NULL COMMENT '流程名称（如：请假流程）',
  `flow_code` varchar(50) NOT NULL COMMENT '流程编码（唯一）',
  `flow_json` text COMMENT '流程设计JSON（节点/连线/样式）',
  `start_role_ids` varchar(200) DEFAULT '' COMMENT '可发起角色ID（sys_role.id，逗号分隔）',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态（1启用0禁用）',
  `creator_id` bigint(20) NOT NULL COMMENT '创建人ID（sys_user.id）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_code` (`flow_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- 2. 流程实例表（流程运行实例）
CREATE TABLE IF NOT EXISTS `flow_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '实例ID',
  `flow_id` bigint(20) NOT NULL COMMENT '关联流程定义ID',
  `instance_name` varchar(100) NOT NULL COMMENT '实例名称（如：张三-3天请假）',
  `applicant_id` bigint(20) NOT NULL COMMENT '申请人ID（sys_user.id）',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户ID（产品智能定制模块需要）',
  `current_node_key` varchar(50) DEFAULT '' COMMENT '当前节点标识',
  `current_node_name` varchar(50) DEFAULT '' COMMENT '当前节点名称',
  `status` tinyint(4) DEFAULT 0 COMMENT '状态（0运行中1已完成2已驳回3已撤销4已终止）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- 3. 流程任务表（节点执行记录，区分人工/自动）
CREATE TABLE IF NOT EXISTS `flow_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `instance_id` bigint(20) NOT NULL COMMENT '关联流程实例ID',
  `node_key` varchar(50) NOT NULL COMMENT '节点标识',
  `node_name` varchar(50) NOT NULL COMMENT '节点名称',
  `node_type` varchar(20) NOT NULL COMMENT '节点类型（start/approve/notify/end）',
  `handler_id` bigint(20) DEFAULT NULL COMMENT '处理人ID（sys_user.id，自动节点为NULL）',
  `handler_name` varchar(50) DEFAULT '' COMMENT '处理人名称（冗余存储，便于展示）',
  `action` varchar(20) DEFAULT '' COMMENT '操作类型（approve/reject/notify/auto）',
  `comment` text COMMENT '审批意见/通知内容',
  `execute_time` datetime DEFAULT NULL COMMENT '任务执行时间',
  `status` tinyint(4) DEFAULT 0 COMMENT '任务状态（0待处理1已完成2已驳回）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(4) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_handler_id` (`handler_id`),
  KEY `idx_node_type` (`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务表';

-- 4. 流程节点配置表（节点规则定义）
CREATE TABLE IF NOT EXISTS `flow_node_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `flow_id` bigint(20) NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(50) NOT NULL COMMENT '节点标识',
  `node_name` varchar(50) NOT NULL COMMENT '节点名称',
  `node_type` varchar(20) NOT NULL COMMENT '节点类型（start/approve/notify/end）',
  `handler_type` varchar(20) DEFAULT '' COMMENT '处理人类型（role/user/role_user，仅approve/notify节点）',
  `handler_ids` varchar(500) DEFAULT '' COMMENT '处理人ID（role.id或user.id，逗号分隔）',
  `module_code` varchar(20) DEFAULT '' COMMENT '模块编码（产品智能定制模块为C）',
  `notify_content` text DEFAULT '' COMMENT '通知内容（仅notify节点）',
  `sort` int(11) DEFAULT 0 COMMENT '节点排序（决定执行顺序）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_node` (`flow_id`,`node_key`),
  KEY `idx_flow_id` (`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点配置表';

-- 5. 流程操作日志表（全流程审计）
CREATE TABLE IF NOT EXISTS `flow_operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `instance_id` bigint(20) NOT NULL COMMENT '流程实例ID',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID（sys_user.id，自动操作为NULL）',
  `operator_name` varchar(50) DEFAULT '' COMMENT '操作人名称',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型（init/approve/reject/notify/cancel/terminate/complete）',
  `operation_content` text NOT NULL COMMENT '操作内容（如：自动通知角色1，内容：xxx）',
  `operation_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程操作日志表';

-- =====================================================
-- 更新现有表结构
-- =====================================================

-- 为 flow_node_config 表添加 module_code 字段
ALTER TABLE `flow_node_config` ADD COLUMN `module_code` varchar(20) DEFAULT '' COMMENT '模块编码（产品智能定制模块为C）' AFTER `handler_ids`;

-- 为 flow_instance 表添加 tenant_id 字段
ALTER TABLE `flow_instance` ADD COLUMN `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户ID（产品智能定制模块需要）' AFTER `applicant_id`;

-- 为 flow_node_config 表添加 uuid 字段（用于连线排序）
ALTER TABLE `flow_node_config` ADD COLUMN `uuid` varchar(50) DEFAULT '' COMMENT '前端节点唯一标识（用于排序）' AFTER `id`;

-- 为 flow_definition 表添加 can_initiate 字段（是否允许主动发起）
ALTER TABLE `flow_definition` ADD COLUMN `can_initiate` tinyint(1) DEFAULT 1 COMMENT '是否允许主动发起（1允许，0不允许）' AFTER `status`;

-- 为 flow_instance 表添加凭证和额外信息字段
ALTER TABLE `flow_instance` ADD COLUMN `attachment_url` varchar(500) DEFAULT '' COMMENT '凭证文件URL' AFTER `current_node_name`;
ALTER TABLE `flow_instance` ADD COLUMN `attachment_name` varchar(200) DEFAULT '' COMMENT '凭证文件名' AFTER `attachment_url`;
ALTER TABLE `flow_instance` ADD COLUMN `extra_info` varchar(1000) DEFAULT '' COMMENT '额外信息' AFTER `attachment_name`;

-- 为 flow_definition 表添加 module_code 字段（流程所属模块）
ALTER TABLE `flow_definition` ADD COLUMN `module_code` varchar(10) DEFAULT '' COMMENT '流程所属模块（A/B/C）' AFTER `need_attachment`;

-- 为 flow_node_config 表添加自定义字段配置
ALTER TABLE `flow_node_config` ADD COLUMN `custom_fields` text COMMENT '自定义字段配置（JSON格式）' AFTER `sort`;

-- 为 flow_task 表添加自定义字段值
ALTER TABLE `flow_task` ADD COLUMN `custom_field_values` text COMMENT '自定义字段值（JSON格式）' AFTER `deleted`;
