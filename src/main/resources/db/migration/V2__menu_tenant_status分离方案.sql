-- =====================================================
-- 菜单全局结构+租户独立状态管理（分离存储方案）
-- =====================================================

-- 1. 移除之前新增的 tenant_id 字段，恢复原有表结构
ALTER TABLE `sys_menu` DROP COLUMN IF EXISTS `tenant_id`;

-- 2. 创建租户菜单状态表
CREATE TABLE IF NOT EXISTS `sys_tenant_menu_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
  `menu_id` bigint(20) NOT NULL COMMENT '关联sys_menu的菜单ID',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '菜单状态（1启用0禁用，仅对该租户生效）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant_menu` (`tenant_id`, `menu_id`) USING BTREE,
  INDEX `idx_tenant_id` (`tenant_id`) USING BTREE,
  INDEX `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户菜单状态表（隔离各租户的菜单启用状态）';

-- =====================================================
-- 数据初始化（请根据实际租户ID调整）
-- =====================================================

-- 3. 导入产品智能定制全局菜单结构（所有租户共用）
-- 注意：请先清空相关数据后再执行
DELETE FROM sys_menu WHERE module_code = 'C';
DELETE FROM sys_tenant_menu_status WHERE 1=1;

INSERT INTO `sys_menu` (
  `menu_name`, `parent_id`, `path`, `component`, `module_code`, 
  `sort`, `status`, `deleted`, `create_time`, `update_time`
) VALUES 
('产品智能定制', 0, '/product-custom', 'layout', 'C', 1, 1, 0, NOW(), NOW());

-- 获取刚才插入的父菜单ID（假设为 1）
SET @parent_id = 1;

INSERT INTO `sys_menu` (
  `menu_name`, `parent_id`, `path`, `component`, `module_code`, 
  `sort`, `status`, `deleted`, `create_time`, `update_time`
) VALUES 
('首页', @parent_id, '/product-custom/home', 'product-custom/home', 'C', 2, 1, 0, NOW(), NOW()),
('设计分析', @parent_id, '/product-custom/design', 'product-custom/design', 'C', 3, 1, 0, NOW(), NOW()),
('产品管理', @parent_id, '/product-custom/product', 'product-custom/product', 'C', 4, 1, 0, NOW(), NOW());

-- 4. 初始化租户菜单状态（默认启用）
-- 请将下面的 1001, 1002 替换为实际的租户ID

-- 查看现有租户ID（如果需要）
-- SELECT id, tenant_name FROM sys_tenant;

-- 为租户初始化菜单状态（假设租户ID为 1001）
INSERT INTO `sys_tenant_menu_status` (`tenant_id`, `menu_id`, `status`, `create_time`, `update_time`)
SELECT 1001, `id`, 1, NOW(), NOW() 
FROM `sys_menu` 
WHERE `module_code` = 'C' AND `deleted` = 0;

-- 为租户初始化菜单状态（假设租户ID为 1002）
INSERT INTO `sys_tenant_menu_status` (`tenant_id`, `menu_id`, `status`, `create_time`, `update_time`)
SELECT 1002, `id`, 1, NOW(), NOW() 
FROM `sys_menu` 
WHERE `module_code` = 'C' AND `deleted` = 0;

-- 5. 验证数据
SELECT 'sys_menu' as table_name, COUNT(*) as count FROM sys_menu WHERE module_code = 'C'
UNION ALL
SELECT 'sys_tenant_menu_status', COUNT(*) FROM sys_tenant_menu_status;
