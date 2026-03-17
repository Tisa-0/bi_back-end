-- 流程参数功能 SQL 脚本
-- 执行时间：2026-03-17

-- 1. 创建动态参数配置表（核心）flow_template_param
CREATE TABLE IF NOT EXISTS `flow_template_param` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL COMMENT '流程模板ID',
  `param_code` varchar(50) NOT NULL COMMENT '参数编码',
  `param_name` varchar(100) NOT NULL COMMENT '参数名称',
  `param_type` varchar(30) NOT NULL DEFAULT 'input' COMMENT '参数类型：input/select/number/date/file/textarea',
  `required` tinyint DEFAULT 0 COMMENT '是否必填：1-必填，0-非必填',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `option_json` text COMMENT '下拉选项JSON [{"label":"","value":""}]',
  `sort` int DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_param_code` (`param_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板参数配置表';

-- 2. 创建流程实例参数表（核心）flow_instance_param
CREATE TABLE IF NOT EXISTS `flow_instance_param` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `template_param_id` bigint NOT NULL COMMENT '对应模板参数ID',
  `param_code` varchar(50) NOT NULL COMMENT '参数编码',
  `param_value` text COMMENT '参数值',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_param_code` (`param_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例参数值表';

-- 3. 添加外键约束（可选，如果需要数据完整性）
-- ALTER TABLE `flow_instance_param`
-- ADD CONSTRAINT `fk_instance_param_template` FOREIGN KEY (`template_param_id`) REFERENCES `flow_template_param`(`id`);
-- ALTER TABLE `flow_instance_param`
-- ADD CONSTRAINT `fk_instance_param_instance` FOREIGN KEY (`instance_id`) REFERENCES `flow_instance`(`id`);
