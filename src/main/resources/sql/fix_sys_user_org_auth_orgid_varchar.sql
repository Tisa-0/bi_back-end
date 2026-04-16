-- 修复 sys_user_org_auth 的 org_id 字段类型，适配机构号（如 99H999）
-- 执行日期：2026-04-16

ALTER TABLE `sys_user_org_auth`
  MODIFY COLUMN `tenant_code` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '租户编码（多租户模块生效；非多租户模块传空字符）',
  MODIFY COLUMN `org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权机构号（bmip_sys_ttlorginf.orgcod）';
