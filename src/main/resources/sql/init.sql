
-- ----------------------------
-- Table structure for flow_definition
-- ----------------------------
DROP TABLE IF EXISTS `flow_definition`;
CREATE TABLE `flow_definition`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `flow_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程名称（如：请假流程）',
  `flow_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程编码（唯一）',
  `flow_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '流程设计JSON（节点/连线/样式）',
  `start_role_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '可发起角色ID（sys_role.id，逗号分隔）',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态（1启用0禁用）',
  `can_initiate` tinyint(1) NULL DEFAULT 1 COMMENT '是否允许主动发起（1允许，0不允许）',
  `creator_id` bigint(20) NOT NULL COMMENT '创建人ID（sys_user.id）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删除',
  `need_attachment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `module_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '流程所属模块（A/B/C）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_flow_code`(`flow_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程定义表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of flow_definition
-- ----------------------------
INSERT INTO `flow_definition` VALUES (1, '流程1', '1', '[{\"id\":\"42d538e3-3c83-4263-9220-5281285e179f\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":70,\"y\":191,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"fdffa56e-b8d3-447a-818e-b02688bba9d5\",\"name\":\"审批节点\",\"type\":\"approve\",\"x\":250,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"5917f365-49f3-4bf8-b58f-4c3c5f1ae397\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":400,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"}]', '', 1, 1, 1, '2026-03-05 17:11:44', '2026-03-06 10:50:19', 1, NULL, '');
INSERT INTO `flow_definition` VALUES (2, '审批流程1', '1234', '{\"nodes\":[{\"id\":\"a024bf47-ed49-4069-8d45-d655e1a5f5ec\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":100,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"9a5ae316-d0f0-48cd-b893-868742863875\",\"name\":\"审批节点\",\"type\":\"approve\",\"x\":306,\"y\":199,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"46957f62-90c8-4870-8b8a-11aecba6d9c6\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":535,\"y\":198,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"}],\"lines\":[{\"fromNode\":\"a024bf47-ed49-4069-8d45-d655e1a5f5ec\",\"toNode\":\"9a5ae316-d0f0-48cd-b893-868742863875\"},{\"fromNode\":\"9a5ae316-d0f0-48cd-b893-868742863875\",\"toNode\":\"46957f62-90c8-4870-8b8a-11aecba6d9c6\"}]}', '', 1, 1, 1, '2026-03-06 10:13:21', '2026-03-06 10:50:17', 1, NULL, '');
INSERT INTO `flow_definition` VALUES (3, '审批流程1', 'FLOW_1772765233373', '{\"nodes\":[{\"id\":\"a024bf47-ed49-4069-8d45-d655e1a5f5ec\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":100,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"9a5ae316-d0f0-48cd-b893-868742863875\",\"name\":\"审批节点\",\"type\":\"approve\",\"x\":306,\"y\":199,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"46957f62-90c8-4870-8b8a-11aecba6d9c6\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":751,\"y\":198,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"32b7157b-4f5f-4667-9f17-fdd29d880fce\",\"name\":\"通知节点\",\"type\":\"notify\",\"x\":534,\"y\":199,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"}],\"lines\":[{\"fromNode\":\"a024bf47-ed49-4069-8d45-d655e1a5f5ec\",\"toNode\":\"9a5ae316-d0f0-48cd-b893-868742863875\"},{\"fromNode\":\"9a5ae316-d0f0-48cd-b893-868742863875\",\"toNode\":\"32b7157b-4f5f-4667-9f17-fdd29d880fce\"},{\"fromNode\":\"32b7157b-4f5f-4667-9f17-fdd29d880fce\",\"toNode\":\"46957f62-90c8-4870-8b8a-11aecba6d9c6\"}]}', '', 1, 1, 1, '2026-03-06 10:47:13', '2026-03-06 10:50:15', 1, NULL, '');
INSERT INTO `flow_definition` VALUES (4, '报表审批流程', 'FLOW_1772765239115', '{\"nodes\":[{\"id\":\"a024bf47-ed49-4069-8d45-d655e1a5f5ec\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":56,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\",\"moduleCode\":\"C\"},{\"id\":\"46957f62-90c8-4870-8b8a-11aecba6d9c6\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":750,\"y\":198,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\",\"moduleCode\":\"A\"},{\"id\":\"9a6c6174-9085-4911-85ca-0bce9d910869\",\"name\":\"租户外审批\",\"type\":\"approve\",\"x\":553,\"y\":199,\"handlerType\":\"role\",\"handlerIds\":[\"1\"],\"notifyContent\":\"\",\"moduleCode\":\"A\"},{\"id\":\"468209d6-fa2e-4278-a5c1-2b39c7b76658\",\"name\":\"通知节点\",\"type\":\"notify\",\"x\":393,\"y\":198,\"handlerType\":\"role\",\"handlerIds\":[\"1\"],\"notifyContent\":\"\",\"moduleCode\":\"A\"},{\"id\":\"2e78ed4f-781c-4a18-a74e-b2b947ef6ce0\",\"name\":\"租户内审批\",\"type\":\"approve\",\"x\":218,\"y\":203,\"handlerType\":\"role\",\"handlerIds\":[\"6\"],\"notifyContent\":\"\",\"moduleCode\":\"C\"}],\"lines\":[{\"fromNode\":\"2e78ed4f-781c-4a18-a74e-b2b947ef6ce0\",\"toNode\":\"468209d6-fa2e-4278-a5c1-2b39c7b76658\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"a024bf47-ed49-4069-8d45-d655e1a5f5ec\",\"toNode\":\"2e78ed4f-781c-4a18-a74e-b2b947ef6ce0\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"468209d6-fa2e-4278-a5c1-2b39c7b76658\",\"toNode\":\"9a6c6174-9085-4911-85ca-0bce9d910869\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"9a6c6174-9085-4911-85ca-0bce9d910869\",\"toNode\":\"46957f62-90c8-4870-8b8a-11aecba6d9c6\",\"fromSide\":\"right\",\"toSide\":\"left\"}]}', '', 1, 1, 1, '2026-03-06 10:47:19', '2026-03-09 16:26:33', 1, NULL, '');
INSERT INTO `flow_definition` VALUES (5, '报表灰度发布流程', 'FLOW_1773044848704', '{\"nodes\":[{\"id\":\"211f83f1-71c0-433c-a008-e4a62553fc97\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":100,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"4760a5af-a4b8-4297-93bf-06771e91aef9\",\"name\":\"租户内审批\",\"type\":\"approve\",\"x\":250,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"6\"],\"notifyContent\":\"\",\"moduleCode\":\"C\"},{\"id\":\"6dc540bb-8c9b-4783-a8e2-e218133311f7\",\"name\":\"工作台管理员审批\",\"type\":\"approve\",\"x\":400,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"1\"],\"notifyContent\":\"\",\"moduleCode\":\"A\"},{\"id\":\"25a31487-c17c-41ed-a49d-e56aa5b98166\",\"name\":\"通知节点\",\"type\":\"notify\",\"x\":550,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"1\"],\"notifyContent\":\"请审批\",\"moduleCode\":\"A\"},{\"id\":\"dad7e071-092c-4f08-83b2-0b477d95e5de\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":700,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\",\"moduleCode\":\"A\"}],\"lines\":[{\"fromNode\":\"211f83f1-71c0-433c-a008-e4a62553fc97\",\"toNode\":\"4760a5af-a4b8-4297-93bf-06771e91aef9\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"4760a5af-a4b8-4297-93bf-06771e91aef9\",\"toNode\":\"6dc540bb-8c9b-4783-a8e2-e218133311f7\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"6dc540bb-8c9b-4783-a8e2-e218133311f7\",\"toNode\":\"25a31487-c17c-41ed-a49d-e56aa5b98166\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"25a31487-c17c-41ed-a49d-e56aa5b98166\",\"toNode\":\"dad7e071-092c-4f08-83b2-0b477d95e5de\",\"fromSide\":\"right\",\"toSide\":\"left\"}]}', '', 1, 1, 1, '2026-03-09 16:27:29', '2026-03-10 09:40:05', 1, '0', 'C');
INSERT INTO `flow_definition` VALUES (6, '申请工作台角色流程', 'FLOW_1773045521360', '{\"nodes\":[{\"id\":\"d02f4398-ce74-4cc2-8e4b-fe5327e6e05e\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":100,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"e3ebce0f-7390-4c3f-bbca-d3d93e0c1b6c\",\"name\":\"通知节点\",\"type\":\"notify\",\"x\":250,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"87e72c45-8eff-45b7-85ee-9341b023134d\",\"name\":\"审批节点\",\"type\":\"approve\",\"x\":400,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"67d2f296-8212-4464-be92-11fa93b3fbb3\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":550,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"}],\"lines\":[{\"fromNode\":\"d02f4398-ce74-4cc2-8e4b-fe5327e6e05e\",\"toNode\":\"e3ebce0f-7390-4c3f-bbca-d3d93e0c1b6c\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"e3ebce0f-7390-4c3f-bbca-d3d93e0c1b6c\",\"toNode\":\"87e72c45-8eff-45b7-85ee-9341b023134d\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"87e72c45-8eff-45b7-85ee-9341b023134d\",\"toNode\":\"67d2f296-8212-4464-be92-11fa93b3fbb3\",\"fromSide\":\"right\",\"toSide\":\"left\"}]}', '', 1, 1, 1, '2026-03-09 16:38:41', '2026-03-09 16:44:40', 1, NULL, '');
INSERT INTO `flow_definition` VALUES (7, '申请租户角色权限流程', 'FLOW_1773047413234', '{\"nodes\":[{\"id\":\"3b8484a7-afbf-4dd8-9a9d-97a4a8861c4f\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":100,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"},{\"id\":\"089e5e00-af89-4d71-b0ec-b60d8b89a594\",\"name\":\"租户内审批\",\"type\":\"approve\",\"x\":249,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"6\"],\"notifyContent\":\"\",\"moduleCode\":\"C\",\"nodeKey\":\"TENANT_SHENPO\",\"customFields\":[]},{\"id\":\"dedbc889-1ea0-45f2-8729-c40dfa9041a4\",\"name\":\"通知节点\",\"type\":\"notify\",\"x\":400,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\",\"nodeKey\":\"notice\",\"moduleCode\":\"C\",\"customFields\":[]},{\"id\":\"38f261f0-f8d9-4edc-9cee-32b1055c11de\",\"name\":\"工作台管理员审批\",\"type\":\"approve\",\"x\":550,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"1\"],\"notifyContent\":\"\",\"moduleCode\":\"A\",\"nodeKey\":\"bi_shenpi\",\"customFields\":[]},{\"id\":\"7953b6b1-239b-4913-8fac-a0262a8300fa\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":700,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\"}],\"lines\":[{\"fromNode\":\"3b8484a7-afbf-4dd8-9a9d-97a4a8861c4f\",\"toNode\":\"089e5e00-af89-4d71-b0ec-b60d8b89a594\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"089e5e00-af89-4d71-b0ec-b60d8b89a594\",\"toNode\":\"dedbc889-1ea0-45f2-8729-c40dfa9041a4\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"dedbc889-1ea0-45f2-8729-c40dfa9041a4\",\"toNode\":\"38f261f0-f8d9-4edc-9cee-32b1055c11de\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"38f261f0-f8d9-4edc-9cee-32b1055c11de\",\"toNode\":\"7953b6b1-239b-4913-8fac-a0262a8300fa\",\"fromSide\":\"right\",\"toSide\":\"left\"}]}', '', 1, 1, 1, '2026-03-09 17:10:13', '2026-03-09 17:10:13', 0, '1', 'C');
INSERT INTO `flow_definition` VALUES (8, '报表灰度发布流程', 'REPORT_RELEASE', '{\"nodes\":[{\"id\":\"cbaa0c7a-a71a-4f65-afd0-3e88ee163d4d\",\"name\":\"开始节点\",\"type\":\"start\",\"x\":-7,\"y\":202,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\",\"customFields\":[],\"nodeKey\":\"\",\"moduleCode\":\"C\"},{\"id\":\"80bc6b97-9702-4a5f-b8de-540722bc2aaa\",\"name\":\"租户内审批\",\"type\":\"approve\",\"x\":266,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"6\"],\"notifyContent\":\"\",\"customFields\":[],\"moduleCode\":\"C\",\"nodeKey\":\"TENANT_PROCESS\"},{\"id\":\"2c45ee26-8a37-451d-ae14-98b3542a6686\",\"name\":\"通知节点\",\"type\":\"notify\",\"x\":400,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"1\"],\"notifyContent\":\"请审批\",\"customFields\":[],\"moduleCode\":\"A\",\"nodeKey\":\"NOTICE\"},{\"id\":\"bc106c12-5901-49a9-b30c-7122d14460ca\",\"name\":\"系统管理员审批\",\"type\":\"approve\",\"x\":550,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[\"1\"],\"notifyContent\":\"\",\"customFields\":[],\"moduleCode\":\"A\",\"nodeKey\":\"BI_PROCESS\"},{\"id\":\"a22bcf2a-d8ea-4248-b950-934f9352ea87\",\"name\":\"结束节点\",\"type\":\"end\",\"x\":700,\"y\":200,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\",\"customFields\":[],\"nodeKey\":\"\",\"moduleCode\":\"A\"},{\"id\":\"149e1c05-cca0-439d-a96c-46c75d8ab8a3\",\"name\":\"灰度发布内容\",\"type\":\"text\",\"x\":126,\"y\":199,\"handlerType\":\"role\",\"handlerIds\":[],\"notifyContent\":\"\",\"customFields\":[{\"fieldName\":\"REPORT_GREY_URL\",\"fieldLabel\":\"灰度验证url\"}],\"moduleCode\":\"C\",\"nodeKey\":\"CONTENT\"}],\"lines\":[{\"fromNode\":\"80bc6b97-9702-4a5f-b8de-540722bc2aaa\",\"toNode\":\"2c45ee26-8a37-451d-ae14-98b3542a6686\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"2c45ee26-8a37-451d-ae14-98b3542a6686\",\"toNode\":\"bc106c12-5901-49a9-b30c-7122d14460ca\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"bc106c12-5901-49a9-b30c-7122d14460ca\",\"toNode\":\"a22bcf2a-d8ea-4248-b950-934f9352ea87\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"cbaa0c7a-a71a-4f65-afd0-3e88ee163d4d\",\"toNode\":\"149e1c05-cca0-439d-a96c-46c75d8ab8a3\",\"fromSide\":\"right\",\"toSide\":\"left\"},{\"fromNode\":\"149e1c05-cca0-439d-a96c-46c75d8ab8a3\",\"toNode\":\"80bc6b97-9702-4a5f-b8de-540722bc2aaa\",\"fromSide\":\"right\",\"toSide\":\"left\"}]}', '', 1, 1, 1, '2026-03-10 09:45:41', '2026-03-10 09:45:41', 0, '0', 'C');

-- ----------------------------
-- Table structure for flow_instance
-- ----------------------------
DROP TABLE IF EXISTS `flow_instance`;
CREATE TABLE `flow_instance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `flow_id` bigint(20) NOT NULL COMMENT '关联流程定义ID',
  `instance_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '实例名称（如：张三-3天请假）',
  `applicant_id` bigint(20) NOT NULL COMMENT '申请人ID（sys_user.id）',
  `tenant_id` bigint(20) NULL DEFAULT NULL COMMENT '租户ID（产品智能定制模块需要）',
  `current_node_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '当前节点标识',
  `current_node_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '当前节点名称',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态（0运行中1已完成2已驳回3已撤销4已终止）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NULL DEFAULT 0,
  `attachment_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `attachment_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `extra_info` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '额外信息',
  `dynamic_handlers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '动态处理人信息（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_applicant_id`(`applicant_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程实例表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of flow_instance
-- ----------------------------
INSERT INTO `flow_instance` VALUES (1, 8, '公共租户报表发布', 1, 1, 'approve_2', '租户内审批', 4, '2026-03-10 09:47:54', '2026-03-10 09:47:54', 0, '', '', '');
INSERT INTO `flow_instance` VALUES (2, 8, '模拟报表灰度发布', 1, 1, 'approve_2', '租户内审批', 0, '2026-03-10 10:21:14', '2026-03-10 10:21:14', 0, '', '', '');
INSERT INTO `flow_instance` VALUES (3, 8, '模拟报表灰度', 1, 1, 'end_5', '结束节点', 1, '2026-03-10 10:34:39', '2026-03-10 10:34:39', 0, '', '', '');
INSERT INTO `flow_instance` VALUES (4, 8, '公共租户模拟报表灰度发布', 1, 1, 'end_5', '结束节点', 1, '2026-03-16 16:08:11', '2026-03-16 16:08:11', 0, '', '', '');
INSERT INTO `flow_instance` VALUES (5, 8, '模拟灰度发布流程', 1, 1, 'approve_2', '租户内审批', 0, '2026-03-17 14:53:51', '2026-03-17 14:53:51', 0, '', '', '');
INSERT INTO `flow_instance` VALUES (6, 8, '报表发布', 1, 1, 'TENANT_PROCESS', '租户内审批', 0, '2026-03-17 15:19:59', '2026-03-17 15:19:59', 0, '', '', '');
INSERT INTO `flow_instance` VALUES (7, 7, '申请角色', 1, 1, 'TENANT_SHENPO', '租户内审批', 0, '2026-03-17 16:39:18', '2026-03-17 16:39:18', 0, '/api/files/20260317/941fbe9fdfbb442fb78f1409041e9706.pdf', '2.pdf', '');
INSERT INTO `flow_instance` VALUES (8, 7, '申请租户角色', 1, 1, 'end_5', '结束节点', 1, '2026-03-17 16:46:04', '2026-03-17 16:46:04', 0, '/api/files/20260317/261e44e85e8f4594996c74ccfa27006e.pdf', '2.pdf', '');


-- ----------------------------
-- Table structure for flow_instance_param
-- ----------------------------
DROP TABLE IF EXISTS `flow_instance_param`;
CREATE TABLE `flow_instance_param`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `instance_id` bigint(20) NOT NULL COMMENT '流程实例ID',
  `template_param_id` bigint(20) NOT NULL COMMENT '对应模板参数ID',
  `param_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数编码',
  `param_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '参数值',
  `param_value_label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数值的中文翻译',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_instance_id`(`instance_id`) USING BTREE,
  INDEX `idx_param_code`(`param_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程实例参数值表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of flow_instance_param
-- ----------------------------
INSERT INTO `flow_instance_param` VALUES (1, 5, 7, 'REPORT_CODE', 'ZH_00001234', NULL);
INSERT INTO `flow_instance_param` VALUES (2, 6, 7, 'REPORT_CODE', 'ZH_000000000', NULL);
INSERT INTO `flow_instance_param` VALUES (3, 7, 9, 'ROLE_TYPE', '6', NULL);
INSERT INTO `flow_instance_param` VALUES (4, 8, 9, 'ROLE_TYPE', '19', '数据集管理员');

-- ----------------------------
-- Table structure for flow_node_config
-- ----------------------------
DROP TABLE IF EXISTS `flow_node_config`;
CREATE TABLE `flow_node_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '前端节点唯一标识（用于排序）',
  `flow_id` bigint(20) NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点标识',
  `node_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点名称',
  `node_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点类型（start/approve/notify/end）',
  `handler_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '处理人类型（role/user/role_user，仅approve/notify节点）',
  `handler_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '处理人ID（role.id或user.id，逗号分隔）',
  `module_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '模块编码（产品智能定制模块为C）',
  `notify_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '通知内容（仅notify节点）',
  `sort` int(11) NULL DEFAULT 0 COMMENT '节点排序（决定执行顺序）',
  `custom_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '自定义字段配置（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_flow_node`(`flow_id`, `node_key`) USING BTREE,
  INDEX `idx_flow_id`(`flow_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 182 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程节点配置表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of flow_node_config
-- ----------------------------
INSERT INTO `flow_node_config` VALUES (171, 'cbaa0c7a-a71a-4f65-afd0-3e88ee163d4d', 8, 'start_1', '开始节点', 'start', 'role', '', 'C', '', 1, '[]');
INSERT INTO `flow_node_config` VALUES (172, '149e1c05-cca0-439d-a96c-46c75d8ab8a3', 8, 'CONTENT', '灰度发布内容', 'text', 'role', '', 'C', '', 2, '[{\"fieldName\":\"REPORT_GREY_URL\",\"fieldLabel\":\"灰度验证url\"}]');
INSERT INTO `flow_node_config` VALUES (173, '80bc6b97-9702-4a5f-b8de-540722bc2aaa', 8, 'TENANT_PROCESS', '租户内审批', 'approve', 'role', '6', 'C', '', 3, '[]');
INSERT INTO `flow_node_config` VALUES (174, '2c45ee26-8a37-451d-ae14-98b3542a6686', 8, 'NOTICE', '通知节点', 'notify', 'role', '1', 'A', '请审批', 4, '[]');
INSERT INTO `flow_node_config` VALUES (175, 'bc106c12-5901-49a9-b30c-7122d14460ca', 8, 'BI_PROCESS', '系统管理员审批', 'approve', 'role', '1', 'A', '', 5, '[]');
INSERT INTO `flow_node_config` VALUES (176, 'a22bcf2a-d8ea-4248-b950-934f9352ea87', 8, 'end_5', '结束节点', 'end', 'role', '', 'A', '', 6, '[]');
INSERT INTO `flow_node_config` VALUES (177, '3b8484a7-afbf-4dd8-9a9d-97a4a8861c4f', 7, 'start_1', '开始节点', 'start', 'role', '', '', '', 1, '');
INSERT INTO `flow_node_config` VALUES (178, '089e5e00-af89-4d71-b0ec-b60d8b89a594', 7, 'TENANT_SHENPO', '租户内审批', 'approve', 'role', '6', 'C', '', 2, '[]');
INSERT INTO `flow_node_config` VALUES (179, 'dedbc889-1ea0-45f2-8729-c40dfa9041a4', 7, 'notice', '通知节点', 'notify', 'role', '', 'C', '', 3, '[]');
INSERT INTO `flow_node_config` VALUES (180, '38f261f0-f8d9-4edc-9cee-32b1055c11de', 7, 'bi_shenpi', '工作台管理员审批', 'approve', 'role', '1', 'A', '', 4, '[]');
INSERT INTO `flow_node_config` VALUES (181, '7953b6b1-239b-4913-8fac-a0262a8300fa', 7, 'end_5', '结束节点', 'end', 'role', '', '', '', 5, '');

-- ----------------------------
-- Table structure for flow_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `flow_operation_log`;
CREATE TABLE `flow_operation_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `instance_id` bigint(20) NOT NULL COMMENT '流程实例ID',
  `operator_id` bigint(20) NULL DEFAULT NULL COMMENT '操作人ID（sys_user.id，自动操作为NULL）',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作人名称',
  `operation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型（init/approve/reject/notify/cancel/terminate/complete）',
  `operation_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作内容（如：自动通知角色1，内容：xxx）',
  `operation_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_instance_id`(`instance_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 76 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程操作日志表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of flow_operation_log
-- ----------------------------
INSERT INTO `flow_operation_log` VALUES (1, 1, 1, 'admin', 'init', '用户admin发起流程：模拟公共租户发起流程', '2026-03-09 16:04:57');
INSERT INTO `flow_operation_log` VALUES (2, 1, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-09 16:04:57');
INSERT INTO `flow_operation_log` VALUES (3, 1, NULL, '', 'complete', '流程到达结束节点，状态改为已完成', '2026-03-09 16:04:57');
INSERT INTO `flow_operation_log` VALUES (4, 2, 1, 'admin', 'init', '用户admin发起流程：公共报表审批流程', '2026-03-09 16:26:23');
INSERT INTO `flow_operation_log` VALUES (5, 2, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-09 16:26:23');
INSERT INTO `flow_operation_log` VALUES (6, 2, NULL, '', 'complete', '流程到达结束节点，状态改为已完成', '2026-03-09 16:26:23');
INSERT INTO `flow_operation_log` VALUES (7, 3, 1, 'admin', 'init', '用户admin发起流程：公共租户发布报表13', '2026-03-09 16:27:54');
INSERT INTO `flow_operation_log` VALUES (8, 3, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-09 16:27:54');
INSERT INTO `flow_operation_log` VALUES (9, 3, NULL, '', 'init', '审批节点[审批节点]已分配处理人：角色：租户管理员', '2026-03-09 16:27:54');
INSERT INTO `flow_operation_log` VALUES (10, 3, 1, 'admin', 'approve', '用户admin对节点[审批节点]审批通过，意见：通过', '2026-03-09 16:29:07');
INSERT INTO `flow_operation_log` VALUES (11, 3, NULL, '', 'init', '审批节点[审批节点]已分配处理人：角色：BI工作台超管', '2026-03-09 16:29:07');
INSERT INTO `flow_operation_log` VALUES (12, 3, 1, 'admin', 'approve', '用户admin对节点[审批节点]审批通过，意见：通过', '2026-03-09 16:29:33');
INSERT INTO `flow_operation_log` VALUES (13, 3, NULL, '', 'notify', '【流程通知】节点：通知节点，通知对象：角色：BI工作台超管，内容：请审批', '2026-03-09 16:29:33');
INSERT INTO `flow_operation_log` VALUES (14, 3, NULL, '', 'complete', '流程到达结束节点，状态改为已完成', '2026-03-09 16:29:33');
INSERT INTO `flow_operation_log` VALUES (15, 4, 1, 'admin', 'init', '用户admin发起流程：cmm发布报表', '2026-03-09 16:31:26');
INSERT INTO `flow_operation_log` VALUES (16, 4, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-09 16:31:26');
INSERT INTO `flow_operation_log` VALUES (17, 4, NULL, '', 'init', '审批节点[审批节点]已分配处理人：无', '2026-03-09 16:31:26');
INSERT INTO `flow_operation_log` VALUES (18, 4, 1, 'admin', 'cancel', '用户撤销流程', '2026-03-09 16:31:51');
INSERT INTO `flow_operation_log` VALUES (19, 5, 1, 'admin', 'init', '用户admin发起流程：cmm发布报表', '2026-03-09 16:32:28');
INSERT INTO `flow_operation_log` VALUES (20, 5, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-09 16:32:28');
INSERT INTO `flow_operation_log` VALUES (21, 5, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：无', '2026-03-09 16:32:28');
INSERT INTO `flow_operation_log` VALUES (22, 6, 1, 'admin', 'init', '用户admin发起流程：申请租户角色权限', '2026-03-09 18:20:16');
INSERT INTO `flow_operation_log` VALUES (23, 6, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-09 18:20:16');
INSERT INTO `flow_operation_log` VALUES (24, 6, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-09 18:20:16');
INSERT INTO `flow_operation_log` VALUES (25, 6, 1, 'admin', 'approve', '用户admin对节点[租户内审批]审批通过，意见：通过', '2026-03-09 18:23:09');
INSERT INTO `flow_operation_log` VALUES (26, 6, NULL, '', 'notify', '【流程通知】节点：通知节点，通知对象：无，内容：', '2026-03-09 18:23:09');
INSERT INTO `flow_operation_log` VALUES (27, 6, NULL, '', 'init', '审批节点[工作台管理员审批]已分配处理人：角色：BI工作台超管', '2026-03-09 18:23:09');
INSERT INTO `flow_operation_log` VALUES (28, 7, 1, 'admin', 'init', '用户admin发起流程：灰度发布报表', '2026-03-09 18:30:23');
INSERT INTO `flow_operation_log` VALUES (29, 7, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-09 18:30:23');
INSERT INTO `flow_operation_log` VALUES (30, 7, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-09 18:30:23');
INSERT INTO `flow_operation_log` VALUES (31, 1, 1, 'admin', 'init', '用户admin发起流程：公共租户报表发布', '2026-03-10 09:47:54');
INSERT INTO `flow_operation_log` VALUES (32, 1, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-10 09:47:54');
INSERT INTO `flow_operation_log` VALUES (33, 1, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-10 09:47:54');
INSERT INTO `flow_operation_log` VALUES (34, 1, 1, 'admin', 'terminate', '管理员终止流程', '2026-03-10 10:13:15');
INSERT INTO `flow_operation_log` VALUES (35, 2, 1, 'admin', 'init', '用户admin发起流程：模拟报表灰度发布', '2026-03-10 10:21:14');
INSERT INTO `flow_operation_log` VALUES (36, 2, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-10 10:21:14');
INSERT INTO `flow_operation_log` VALUES (37, 2, NULL, '', 'pass', '文本节点[文本节点]自动通过', '2026-03-10 10:21:14');
INSERT INTO `flow_operation_log` VALUES (38, 2, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-10 10:21:14');
INSERT INTO `flow_operation_log` VALUES (39, 3, 1, 'admin', 'init', '用户admin发起流程：模拟报表灰度', '2026-03-10 10:34:39');
INSERT INTO `flow_operation_log` VALUES (40, 3, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-10 10:34:39');
INSERT INTO `flow_operation_log` VALUES (41, 3, NULL, '', 'pass', '文本节点[文本节点]自动通过', '2026-03-10 10:34:39');
INSERT INTO `flow_operation_log` VALUES (42, 3, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-10 10:34:39');
INSERT INTO `flow_operation_log` VALUES (43, 3, 1, 'admin', 'approve', '用户admin对节点[租户内审批]审批通过，意见：通过', '2026-03-10 15:38:37');
INSERT INTO `flow_operation_log` VALUES (44, 3, NULL, '', 'notify', '【流程通知】节点：通知节点，通知对象：角色：BI工作台超管，内容：请审批', '2026-03-10 15:38:37');
INSERT INTO `flow_operation_log` VALUES (45, 3, NULL, '', 'init', '审批节点[系统管理员审批]已分配处理人：角色：BI工作台超管', '2026-03-10 15:38:37');
INSERT INTO `flow_operation_log` VALUES (46, 3, 1, 'admin', 'approve', '用户admin对节点[系统管理员审批]审批通过，意见：通过', '2026-03-16 16:06:22');
INSERT INTO `flow_operation_log` VALUES (47, 3, NULL, '', 'complete', '流程到达结束节点，状态改为已完成', '2026-03-16 16:06:22');
INSERT INTO `flow_operation_log` VALUES (48, 4, 1, 'admin', 'init', '用户admin发起流程：公共租户模拟报表灰度发布', '2026-03-16 16:08:11');
INSERT INTO `flow_operation_log` VALUES (49, 4, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-16 16:08:11');
INSERT INTO `flow_operation_log` VALUES (50, 4, NULL, '', 'pass', '文本节点[灰度发布内容]自动通过', '2026-03-16 16:08:11');
INSERT INTO `flow_operation_log` VALUES (51, 4, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-16 16:08:11');
INSERT INTO `flow_operation_log` VALUES (52, 4, 1, 'admin', 'approve', '用户admin对节点[租户内审批]审批通过，意见：通过', '2026-03-16 16:08:47');
INSERT INTO `flow_operation_log` VALUES (53, 4, NULL, '', 'notify', '【流程通知】节点：通知节点，通知对象：角色：BI工作台超管，内容：请审批', '2026-03-16 16:08:47');
INSERT INTO `flow_operation_log` VALUES (54, 4, NULL, '', 'init', '审批节点[系统管理员审批]已分配处理人：角色：BI工作台超管', '2026-03-16 16:08:47');
INSERT INTO `flow_operation_log` VALUES (55, 4, 1, 'admin', 'approve', '用户admin对节点[系统管理员审批]审批通过，意见：通过', '2026-03-16 16:08:54');
INSERT INTO `flow_operation_log` VALUES (56, 4, NULL, '', 'complete', '流程到达结束节点，状态改为已完成', '2026-03-16 16:08:54');
INSERT INTO `flow_operation_log` VALUES (57, 5, 1, 'admin', 'init', '用户admin发起流程：模拟灰度发布流程', '2026-03-17 14:53:51');
INSERT INTO `flow_operation_log` VALUES (58, 5, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-17 14:53:51');
INSERT INTO `flow_operation_log` VALUES (59, 5, NULL, '', 'pass', '文本节点[灰度发布内容]自动通过', '2026-03-17 14:53:51');
INSERT INTO `flow_operation_log` VALUES (60, 5, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-17 14:53:51');
INSERT INTO `flow_operation_log` VALUES (61, 6, 1, 'admin', 'init', '用户admin发起流程：报表发布', '2026-03-17 15:19:59');
INSERT INTO `flow_operation_log` VALUES (62, 6, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-17 15:19:59');
INSERT INTO `flow_operation_log` VALUES (63, 6, NULL, '', 'pass', '文本节点[灰度发布内容]自动通过', '2026-03-17 15:19:59');
INSERT INTO `flow_operation_log` VALUES (64, 6, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-17 15:19:59');
INSERT INTO `flow_operation_log` VALUES (65, 7, 1, 'admin', 'init', '用户admin发起流程：申请角色', '2026-03-17 16:39:18');
INSERT INTO `flow_operation_log` VALUES (66, 7, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-17 16:39:18');
INSERT INTO `flow_operation_log` VALUES (67, 7, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-17 16:39:18');
INSERT INTO `flow_operation_log` VALUES (68, 8, 1, 'admin', 'init', '用户admin发起流程：申请租户角色', '2026-03-17 16:46:04');
INSERT INTO `flow_operation_log` VALUES (69, 8, NULL, '', 'notify', '开始节点自动触发，流转至下一个节点', '2026-03-17 16:46:04');
INSERT INTO `flow_operation_log` VALUES (70, 8, NULL, '', 'init', '审批节点[租户内审批]已分配处理人：角色：租户管理员', '2026-03-17 16:46:04');
INSERT INTO `flow_operation_log` VALUES (71, 8, 1, 'admin', 'approve', '用户admin对节点[租户内审批]审批通过，意见：', '2026-03-17 16:50:27');
INSERT INTO `flow_operation_log` VALUES (72, 8, NULL, '', 'notify', '【流程通知】节点：通知节点，通知对象：无，内容：', '2026-03-17 16:50:27');
INSERT INTO `flow_operation_log` VALUES (73, 8, NULL, '', 'init', '审批节点[工作台管理员审批]已分配处理人：角色：BI工作台超管', '2026-03-17 16:50:27');
INSERT INTO `flow_operation_log` VALUES (74, 8, 1, 'admin', 'approve', '用户admin对节点[工作台管理员审批]审批通过，意见：', '2026-03-17 16:50:48');
INSERT INTO `flow_operation_log` VALUES (75, 8, NULL, '', 'complete', '流程到达结束节点，状态改为已完成', '2026-03-17 16:50:48');

-- ----------------------------
-- Table structure for flow_task
-- ----------------------------
DROP TABLE IF EXISTS `flow_task`;
CREATE TABLE `flow_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `instance_id` bigint(20) NOT NULL COMMENT '关联流程实例ID',
  `node_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点标识',
  `node_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点名称',
  `node_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点类型（start/approve/notify/end）',
  `handler_id` bigint(20) NULL DEFAULT NULL COMMENT '处理人ID（sys_user.id，自动节点为NULL）',
  `handler_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '处理人名称（冗余存储，便于展示）',
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作类型（approve/reject/notify/auto）',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '审批意见/通知内容',
  `execute_time` datetime NULL DEFAULT NULL COMMENT '任务执行时间',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '任务状态（0待处理1已完成2已驳回）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NULL DEFAULT 0,
  `custom_field_values` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '自定义字段值（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_instance_id`(`instance_id`) USING BTREE,
  INDEX `idx_handler_id`(`handler_id`) USING BTREE,
  INDEX `idx_node_type`(`node_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程任务表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of flow_task
-- ----------------------------
INSERT INTO `flow_task` VALUES (26, 1, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-10 09:47:54', 1, '2026-03-10 09:47:54', 0, NULL);
INSERT INTO `flow_task` VALUES (27, 1, 'approve_2', '租户内审批', 'approve', 1, 'admin', '', NULL, NULL, 0, '2026-03-10 09:47:54', 0, NULL);
INSERT INTO `flow_task` VALUES (28, 2, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-10 10:21:14', 1, '2026-03-10 10:21:14', 0, NULL);
INSERT INTO `flow_task` VALUES (29, 2, 'approve_2', '租户内审批', 'approve', 1, 'admin', '', NULL, NULL, 0, '2026-03-10 10:21:14', 0, NULL);
INSERT INTO `flow_task` VALUES (30, 3, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-10 10:34:39', 1, '2026-03-10 10:34:39', 0, NULL);
INSERT INTO `flow_task` VALUES (31, 3, 'text_6', '文本节点', 'text', NULL, '系统自动', 'auto', '文本节点自动通过', '2026-03-10 10:34:39', 1, '2026-03-10 10:34:39', 0, '{\"REPORT_GREY_URL\":\"https://gray.example.com/report/fb1683b2\"}');
INSERT INTO `flow_task` VALUES (32, 3, 'approve_2', '租户内审批', 'approve', 1, 'admin', 'approve', '通过', '2026-03-10 15:38:37', 1, '2026-03-10 10:34:39', 0, NULL);
INSERT INTO `flow_task` VALUES (33, 3, 'notify_3', '通知节点', 'notify', NULL, '系统自动', 'notify', '请审批', '2026-03-10 15:38:37', 1, '2026-03-10 15:38:37', 0, NULL);
INSERT INTO `flow_task` VALUES (34, 3, 'approve_4', '系统管理员审批', 'approve', 1, 'admin', 'approve', '通过', '2026-03-16 16:06:21', 1, '2026-03-10 15:38:37', 0, NULL);
INSERT INTO `flow_task` VALUES (35, 3, 'approve_4', '系统管理员审批', 'approve', 2, 'userA', '', NULL, NULL, 0, '2026-03-10 15:38:37', 0, NULL);
INSERT INTO `flow_task` VALUES (36, 3, 'approve_4', '系统管理员审批', 'approve', 3, 'userB', '', NULL, NULL, 0, '2026-03-10 15:38:37', 0, NULL);
INSERT INTO `flow_task` VALUES (37, 3, 'approve_4', '系统管理员审批', 'approve', 4, 'userC', '', NULL, NULL, 0, '2026-03-10 15:38:37', 0, NULL);
INSERT INTO `flow_task` VALUES (38, 3, 'approve_4', '系统管理员审批', 'approve', 5, 'gggg', '', NULL, NULL, 0, '2026-03-10 15:38:37', 0, NULL);
INSERT INTO `flow_task` VALUES (39, 3, 'end_5', '结束节点', 'end', NULL, '系统自动', 'auto', '流程完成', '2026-03-16 16:06:22', 1, '2026-03-16 16:06:22', 0, NULL);
INSERT INTO `flow_task` VALUES (40, 4, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-16 16:08:11', 1, '2026-03-16 16:08:11', 0, NULL);
INSERT INTO `flow_task` VALUES (41, 4, 'text_6', '灰度发布内容', 'text', NULL, '系统自动', 'auto', '文本节点自动通过', '2026-03-16 16:08:11', 1, '2026-03-16 16:08:11', 0, '{\"REPORT_GREY_URL\":\"https://gray.example.com/report/a2c781f4\"}');
INSERT INTO `flow_task` VALUES (42, 4, 'approve_2', '租户内审批', 'approve', 1, 'admin', 'approve', '通过', '2026-03-16 16:08:47', 1, '2026-03-16 16:08:11', 0, NULL);
INSERT INTO `flow_task` VALUES (43, 4, 'notify_3', '通知节点', 'notify', NULL, '系统自动', 'notify', '请审批', '2026-03-16 16:08:47', 1, '2026-03-16 16:08:47', 0, NULL);
INSERT INTO `flow_task` VALUES (44, 4, 'approve_4', '系统管理员审批', 'approve', 1, 'admin', 'approve', '通过', '2026-03-16 16:08:54', 1, '2026-03-16 16:08:47', 0, NULL);
INSERT INTO `flow_task` VALUES (45, 4, 'approve_4', '系统管理员审批', 'approve', 2, 'userA', '', NULL, NULL, 0, '2026-03-16 16:08:47', 0, NULL);
INSERT INTO `flow_task` VALUES (46, 4, 'approve_4', '系统管理员审批', 'approve', 3, 'userB', '', NULL, NULL, 0, '2026-03-16 16:08:47', 0, NULL);
INSERT INTO `flow_task` VALUES (47, 4, 'approve_4', '系统管理员审批', 'approve', 4, 'userC', '', NULL, NULL, 0, '2026-03-16 16:08:47', 0, NULL);
INSERT INTO `flow_task` VALUES (48, 4, 'approve_4', '系统管理员审批', 'approve', 5, 'gggg', '', NULL, NULL, 0, '2026-03-16 16:08:47', 0, NULL);
INSERT INTO `flow_task` VALUES (49, 4, 'end_5', '结束节点', 'end', NULL, '系统自动', 'auto', '流程完成', '2026-03-16 16:08:54', 1, '2026-03-16 16:08:54', 0, NULL);
INSERT INTO `flow_task` VALUES (50, 5, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-17 14:53:51', 1, '2026-03-17 14:53:51', 0, NULL);
INSERT INTO `flow_task` VALUES (51, 5, 'text_6', '灰度发布内容', 'text', NULL, '系统自动', 'auto', '文本节点自动通过', '2026-03-17 14:53:51', 1, '2026-03-17 14:53:51', 0, '{\"REPORT_GREY_URL\":\"https://gray.example.com/report/a18bae74\"}');
INSERT INTO `flow_task` VALUES (52, 5, 'approve_2', '租户内审批', 'approve', 1, 'admin', '', NULL, NULL, 0, '2026-03-17 14:53:51', 0, NULL);
INSERT INTO `flow_task` VALUES (53, 6, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-17 15:19:59', 1, '2026-03-17 15:19:59', 0, NULL);
INSERT INTO `flow_task` VALUES (54, 6, 'CONTENT', '灰度发布内容', 'text', NULL, '系统自动', 'auto', '文本节点自动通过', '2026-03-17 15:19:59', 1, '2026-03-17 15:19:59', 0, '{\"REPORT_GREY_URL\":\"https://gray.example.com/report/9a03e7ec\"}');
INSERT INTO `flow_task` VALUES (55, 6, 'TENANT_PROCESS', '租户内审批', 'approve', 1, 'admin', '', NULL, NULL, 0, '2026-03-17 15:19:59', 0, NULL);
INSERT INTO `flow_task` VALUES (56, 7, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-17 16:39:18', 1, '2026-03-17 16:39:18', 0, NULL);
INSERT INTO `flow_task` VALUES (57, 7, 'TENANT_SHENPO', '租户内审批', 'approve', 1, 'admin', '', NULL, NULL, 0, '2026-03-17 16:39:18', 0, NULL);
INSERT INTO `flow_task` VALUES (58, 8, 'start_1', '开始节点', 'start', NULL, '系统自动', 'auto', '开始节点自动执行', '2026-03-17 16:46:04', 1, '2026-03-17 16:46:04', 0, NULL);
INSERT INTO `flow_task` VALUES (59, 8, 'TENANT_SHENPO', '租户内审批', 'approve', 1, 'admin', 'approve', '', '2026-03-17 16:50:27', 1, '2026-03-17 16:46:04', 0, NULL);
INSERT INTO `flow_task` VALUES (60, 8, 'notice', '通知节点', 'notify', NULL, '系统自动', 'notify', '', '2026-03-17 16:50:27', 1, '2026-03-17 16:50:27', 0, NULL);
INSERT INTO `flow_task` VALUES (61, 8, 'bi_shenpi', '工作台管理员审批', 'approve', 1, 'admin', 'approve', '', '2026-03-17 16:50:48', 1, '2026-03-17 16:50:27', 0, NULL);
INSERT INTO `flow_task` VALUES (62, 8, 'bi_shenpi', '工作台管理员审批', 'approve', 2, 'userA', '', NULL, NULL, 0, '2026-03-17 16:50:27', 0, NULL);
INSERT INTO `flow_task` VALUES (63, 8, 'bi_shenpi', '工作台管理员审批', 'approve', 3, 'userB', '', NULL, NULL, 0, '2026-03-17 16:50:27', 0, NULL);
INSERT INTO `flow_task` VALUES (64, 8, 'bi_shenpi', '工作台管理员审批', 'approve', 4, 'userC', '', NULL, NULL, 0, '2026-03-17 16:50:27', 0, NULL);
INSERT INTO `flow_task` VALUES (65, 8, 'bi_shenpi', '工作台管理员审批', 'approve', 5, 'gggg', '', NULL, NULL, 0, '2026-03-17 16:50:27', 0, NULL);
INSERT INTO `flow_task` VALUES (66, 8, 'end_5', '结束节点', 'end', NULL, '系统自动', 'auto', '流程完成', '2026-03-17 16:50:48', 1, '2026-03-17 16:50:48', 0, NULL);

-- ----------------------------
-- Table structure for flow_template_param
-- ----------------------------
DROP TABLE IF EXISTS `flow_template_param`;
CREATE TABLE `flow_template_param`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `template_id` bigint(20) NOT NULL COMMENT '流程模板ID',
  `param_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数编码',
  `param_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名称',
  `param_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'input' COMMENT '参数类型：input/select/number/date/file/textarea',
  `required` tinyint(4) NULL DEFAULT 0 COMMENT '是否必填：1-必填，0-非必填',
  `default_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '默认值',
  `option_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '下拉选项JSON [{\"label\":\"\",\"value\":\"\"}]',
  `sort` int(11) NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_template_id`(`template_id`) USING BTREE,
  INDEX `idx_param_code`(`param_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程模板参数配置表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of flow_template_param
-- ----------------------------
INSERT INTO `flow_template_param` VALUES (7, 8, 'REPORT_CODE', '报表编码', 'input', 1, '', '', NULL);
INSERT INTO `flow_template_param` VALUES (9, 7, 'ROLE_TYPE', '角色类型', 'select', 1, '', '[{\"label\":\"分行审批员\",\"value\":\"20\"},\n{\"label\":\"数据集管理员\",\"value\":\"19\"},\n{\"label\":\"租户管理员\",\"value\":\"6\"},\n{\"label\":\"产品设计员\",\"value\":\"8\"}]', NULL);

-- ----------------------------
-- Table structure for sys_api
-- ----------------------------
DROP TABLE IF EXISTS `sys_api`;
CREATE TABLE `sys_api`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `api_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接口路径',
  `api_desc` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '接口描述',
  `module_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属模块(ABC)',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'GET' COMMENT '请求方法',
  `status` int(11) NULL DEFAULT 1 COMMENT '状态(1启用0禁用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) NULL DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_module_code`(`module_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '接口表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_api
-- ----------------------------
INSERT INTO `sys_api` VALUES (1, '/api/module-a/menu/list', '获取模块A菜单列表', 'A', 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (2, '/api/module-a/menu/add', '新增模块A菜单', 'A', 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (3, '/api/module-a/menu/update', '修改模块A菜单', 'A', 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (4, '/api/module-a/menu/delete', '删除模块A菜单', 'A', 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (5, '/api/module-a/api/list', '获取模块A接口列表', 'A', 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (6, '/api/module-a/api/add', '新增模块A接口', 'A', 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (7, '/api/module-a/api/update', '修改模块A接口', 'A', 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (8, '/api/module-a/api/delete', '删除模块A接口', 'A', 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (9, '/api/module-b/menu/list', '获取模块B菜单列表', 'B', 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (10, '/api/module-b/menu/add', '新增模块B菜单', 'B', 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (11, '/api/module-b/menu/update', '修改模块B菜单', 'B', 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (12, '/api/module-b/menu/delete', '删除模块B菜单', 'B', 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (13, '/api/module-b/api/list', '获取模块B接口列表', 'B', 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (14, '/api/module-b/api/add', '新增模块B接口', 'B', 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (15, '/api/module-b/api/update', '修改模块B接口', 'B', 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (16, '/api/module-b/api/delete', '删除模块B接口', 'B', 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (17, '/api/module-c/menu/list', '获取模块C菜单列表', 'C', 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (18, '/api/module-c/menu/add', '新增模块C菜单', 'C', 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (19, '/api/module-c/menu/update', '修改模块C菜单', 'C', 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (20, '/api/module-c/menu/delete', '删除模块C菜单', 'C', 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (21, '/api/module-c/api/list', '获取模块C接口列表', 'C', 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (22, '/api/module-c/api/add', '新增模块C接口', 'C', 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (23, '/api/module-c/api/update', '修改模块C接口', 'C', 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (24, '/api/module-c/api/delete', '删除模块C接口', 'C', 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (25, '/api/user/list', '获取用户列表', NULL, 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (26, '/api/user/add', '新增用户', NULL, 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (27, '/api/user/update', '修改用户', NULL, 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (28, '/api/user/delete', '删除用户', NULL, 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (29, '/api/role/list', '获取角色列表', NULL, 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (30, '/api/role/add', '新增角色', NULL, 'POST', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (31, '/api/role/update', '修改角色', NULL, 'PUT', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (32, '/api/role/delete', '删除角色', NULL, 'DELETE', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);
INSERT INTO `sys_api` VALUES (33, '/api/module/list', '获取模块列表', NULL, 'GET', 1, '2026-02-24 19:01:07', '2026-02-24 19:01:07', 0);

-- ----------------------------
-- Table structure for sys_asset
-- ----------------------------
DROP TABLE IF EXISTS `sys_asset`;
CREATE TABLE `sys_asset`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '资产ID',
  `module_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属模块（A/B/C）',
  `asset_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资产名称',
  `asset_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '资产编码',
  `asset_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '资产类型（如设备、权限、资源）',
  `asset_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '资产描述',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态（1启用 0禁用）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) NULL DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_asset_code`(`module_code`, `asset_code`) USING BTREE COMMENT '模块+资产编码唯一索引',
  INDEX `idx_module_code`(`module_code`) USING BTREE COMMENT '模块索引'
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '资产库表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_asset
-- ----------------------------
INSERT INTO `sys_asset` VALUES (1, 'A', '模块A-设备001', 'ASSET-A-001', '设备', '模块A的通用设备', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (2, 'A', '模块A-设备002', 'ASSET-A-002', '设备', '模块A的通用设备', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (3, 'A', '模块A-权限001', 'ASSET-A-003', '权限', '模块A的管理权限', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (4, 'A', '模块A-权限002', 'ASSET-A-004', '权限', '模块A的操作权限', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (5, 'A', '模块A-资源001', 'ASSET-A-005', '资源', '模块A的业务资源', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (6, 'A', '模块A-资源002', 'ASSET-A-006', '资源', '模块A的数据资源', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (7, 'B', '模块B-设备001', 'ASSET-B-001', '设备', '模块B的通用设备', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (8, 'B', '模块B-设备002', 'ASSET-B-002', '设备', '模块B的通用设备', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (9, 'B', '模块B-权限001', 'ASSET-B-003', '权限', '模块B的管理权限', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (10, 'B', '模块B-权限002', 'ASSET-B-004', '权限', '模块B的操作权限', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (11, 'B', '模块B-资源001', 'ASSET-B-005', '资源', '模块B的业务资源', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (12, 'C', '模块C-设备001', 'ASSET-C-001', '设备', '模块C的通用设备', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (13, 'C', '模块C-设备002', 'ASSET-C-002', '设备', '模块C的通用设备', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (14, 'C', '模块C-权限001', 'ASSET-C-003', '权限', '模块C的只读权限', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (15, 'C', '模块C-权限002', 'ASSET-C-004', '权限', '模块C的操作权限', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);
INSERT INTO `sys_asset` VALUES (16, 'C', '模块C-资源001', 'ASSET-C-005', '资源', '模块C的业务资源', 1, '2026-02-28 10:15:25', '2026-02-28 10:15:25', 0);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `menu_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父菜单ID',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由路径',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `module_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属模块(ABC)',
  `sort` int(11) NULL DEFAULT 0 COMMENT '排序',
  `status` int(11) NULL DEFAULT 1 COMMENT '状态(1启用0禁用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) NULL DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_module_code`(`module_code`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE,
  INDEX `idx_tenant_module`(`module_code`) USING BTREE,
  INDEX `idx_parent_tenant`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 177 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (73, '首页', 0, '/dashboard', 'Dashboard', 'A', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (74, '系统管理', 0, '/system', 'system/Index', 'A', 3, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (75, '用户管理', 3, '/system/user', 'system/User', 'A', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (76, '用户列表', 4, '/system/user', 'system/UserList', 'A', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (77, '用户详情', 4, '/system/user/detail', 'system/UserDetail', 'A', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (78, '角色管理', 3, '/system/role', 'system/Role', 'A', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (79, '角色列表', 6, '/system/role', 'system/RoleList', 'A', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (80, '角色权限', 6, '/system/role/permission', 'system/RolePermission', 'A', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (81, '系统管理', 0, '/module', 'module/Index', 'A', 3, 1, '2026-03-10 14:35:31', '2026-03-10 14:46:02', 1);
INSERT INTO `sys_menu` VALUES (82, '菜单管理', 9, '/module/menu', 'module/Menu', 'A', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (83, '菜单列表', 10, '/module/menu', 'module/MenuList', 'A', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (84, '菜单详情', 10, '/module/menu/detail', 'module/MenuDetail', 'A', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (85, '接口管理', 9, '/module/api', 'module/Api', 'A', 4, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (86, '接口列表', 13, '/module/api', 'module/ApiList', 'A', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (87, '流程设计', 0, '/flow/definition/list', 'flow/definition/FlowDefinitionList', 'A', 4, 1, '2026-03-10 14:35:31', '2026-03-10 14:46:05', 1);
INSERT INTO `sys_menu` VALUES (88, '待办任务', 0, '/my/pending', 'flow/my/MyPending', 'A', 5, 1, '2026-03-10 14:35:31', '2026-03-10 14:46:08', 1);
INSERT INTO `sys_menu` VALUES (89, '我的流转', 0, '/my/initiated', 'flow/my/MyInitiated', 'A', 6, 1, '2026-03-10 14:35:31', '2026-03-10 14:46:10', 1);
INSERT INTO `sys_menu` VALUES (90, '我的审批', 0, '/my/approval', 'flow/my/MyApproval', 'A', 7, 1, '2026-03-10 14:35:31', '2026-03-10 14:46:13', 1);
INSERT INTO `sys_menu` VALUES (91, '首页', 0, '/dashboard', 'Dashboard', 'B', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (92, '权限管理', 0, '/system', 'system/Index', 'B', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (93, '用户管理', 20, '/system/user', 'system/User', 'B', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (94, '用户列表', 21, '/system/user', 'system/UserList', 'B', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (95, '用户详情', 21, '/system/user/detail', 'system/UserDetail', 'B', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (96, '角色管理', 20, '/system/role', 'system/Role', 'B', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (97, '角色列表', 24, '/system/role', 'system/RoleList', 'B', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (98, '角色权限', 24, '/system/role/permission', 'system/RolePermission', 'B', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (99, '系统管理', 0, '/module', 'module/Index', 'B', 3, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (100, '菜单管理', 27, '/module/menu', 'module/Menu', 'B', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (101, '菜单列表', 28, '/module/menu', 'module/MenuList', 'B', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (102, '菜单详情', 28, '/module/menu/detail', 'module/MenuDetail', 'B', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (103, '接口管理', 27, '/module/api', 'module/Api', 'B', 4, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (104, '接口列表', 31, '/module/api', 'module/ApiList', 'B', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (105, '流程设计', 0, '/flow/definition/list', 'flow/definition/FlowDefinitionList', 'B', 4, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (106, '待办任务', 0, '/my/pending', 'flow/my/MyPending', 'B', 5, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (107, '我的流转', 0, '/my/initiated', 'flow/my/MyInitiated', 'B', 6, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (108, '我的审批', 0, '/my/approval', 'flow/my/MyApproval', 'B', 7, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (109, '首页', 0, '/dashboard', 'Dashboard', 'C', 1, 1, '2026-03-10 14:35:31', '2026-03-10 15:09:40', 1);
INSERT INTO `sys_menu` VALUES (110, '权限管理', 0, '/system', 'system/Index', 'C', 2, 1, '2026-03-10 14:35:31', '2026-03-10 15:09:41', 1);
INSERT INTO `sys_menu` VALUES (111, '用户管理', 38, '/system/user', 'system/User', 'C', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (112, '用户列表', 39, '/system/user', 'system/UserList', 'C', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (113, '用户详情', 39, '/system/user/detail', 'system/UserDetail', 'C', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (114, '角色管理', 38, '/system/role', 'system/Role', 'C', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (115, '角色列表', 42, '/system/role', 'system/RoleList', 'C', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (116, '角色权限', 42, '/system/role/permission', 'system/RolePermission', 'C', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (117, '系统管理', 0, '/module', 'module/Index', 'C', 3, 1, '2026-03-10 14:35:31', '2026-03-10 15:09:43', 1);
INSERT INTO `sys_menu` VALUES (118, '菜单管理', 45, '/module/menu', 'module/Menu', 'C', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (119, '菜单列表', 46, '/module/menu', 'module/MenuList', 'C', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (120, '菜单详情', 46, '/module/menu/detail', 'module/MenuDetail', 'C', 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (121, '接口管理', 45, '/module/api', 'module/Api', 'C', 4, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (122, '接口列表', 49, '/module/api', 'module/ApiList', 'C', 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (123, '流程设计', 0, '/flow/definition/list', 'flow/definition/FlowDefinitionList', 'C', 4, 1, '2026-03-10 14:35:31', '2026-03-10 15:09:44', 1);
INSERT INTO `sys_menu` VALUES (124, '待办任务', 0, '/my/pending', 'flow/my/MyPending', 'C', 5, 1, '2026-03-10 14:35:31', '2026-03-10 15:09:46', 1);
INSERT INTO `sys_menu` VALUES (125, '我的流转', 0, '/my/initiated', 'flow/my/MyInitiated', 'C', 6, 1, '2026-03-10 14:35:31', '2026-03-10 15:09:47', 1);
INSERT INTO `sys_menu` VALUES (126, '我的审批', 0, '/my/approval', 'flow/my/MyApproval', 'C', 7, 1, '2026-03-10 14:35:31', '2026-03-10 15:09:49', 1);
INSERT INTO `sys_menu` VALUES (127, '系统管理', 0, '/system', 'system/Index', NULL, 100, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (128, '用户管理', 54, '/system/user', 'system/User', NULL, 1, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (129, '角色管理', 54, '/system/role', 'system/Role', NULL, 2, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (130, '权限管理', 0, '/permission', 'permission/Index', NULL, 101, 1, '2026-03-10 14:35:31', '2026-03-10 14:35:31', 0);
INSERT INTO `sys_menu` VALUES (131, '菜单管理', 74, '/module/menu', '', 'A', 0, 1, '2026-03-10 14:40:39', '2026-03-10 14:40:39', 0);
INSERT INTO `sys_menu` VALUES (132, '接口管理', 74, '/module/api', '', 'A', 0, 1, '2026-03-10 14:40:51', '2026-03-10 14:40:51', 0);
INSERT INTO `sys_menu` VALUES (133, '流程设计', 74, '/flow/definition/list', '', 'A', 0, 1, '2026-03-10 14:41:02', '2026-03-10 14:41:02', 0);
INSERT INTO `sys_menu` VALUES (134, '我的待办', 0, '/my', '', 'A', 4, 1, '2026-03-10 14:46:32', '2026-03-10 14:46:32', 0);
INSERT INTO `sys_menu` VALUES (135, '待办任务', 134, '/my/pending', '', 'A', 0, 1, '2026-03-10 14:46:38', '2026-03-10 14:46:38', 0);
INSERT INTO `sys_menu` VALUES (136, '我的流转', 134, '/my/initiated', '', 'A', 0, 1, '2026-03-10 14:46:51', '2026-03-10 14:46:51', 0);
INSERT INTO `sys_menu` VALUES (137, '我的审批', 134, '/my/approval', '', 'A', 0, 1, '2026-03-10 14:47:03', '2026-03-10 14:47:03', 0);
INSERT INTO `sys_menu` VALUES (138, '权限管理', 0, '/system', '', 'A', 2, 1, '2026-03-10 14:49:58', '2026-03-10 14:49:58', 0);
INSERT INTO `sys_menu` VALUES (139, '角色管理', 138, '/system/role', '', 'A', 0, 1, '2026-03-10 14:50:12', '2026-03-10 14:50:12', 0);
INSERT INTO `sys_menu` VALUES (140, '用户管理', 138, '/system/user', '', 'A', 0, 1, '2026-03-10 14:50:26', '2026-03-10 14:50:26', 0);
INSERT INTO `sys_menu` VALUES (141, '首页', 0, '/tenant-design/home', '', 'C', 0, 1, '2026-03-10 15:10:00', '2026-03-10 15:10:00', 0);
INSERT INTO `sys_menu` VALUES (142, '设计分析', 0, '/', '', 'C', 0, 1, '2026-03-10 15:10:16', '2026-03-10 15:10:16', 0);
INSERT INTO `sys_menu` VALUES (143, '数据集编排', 142, '/tenant-design/dataset/zero-code', '', 'C', 0, 1, '2026-03-10 15:10:29', '2026-03-10 15:10:29', 0);
INSERT INTO `sys_menu` VALUES (144, 'SQL编排', 142, '/tenant-design/dataset/sql', '', 'C', 0, 1, '2026-03-10 15:10:40', '2026-03-10 15:10:40', 0);
INSERT INTO `sys_menu` VALUES (145, '报表设计', 142, '/tenant-design/report', '', 'C', 0, 1, '2026-03-10 15:10:53', '2026-03-10 15:10:53', 0);
INSERT INTO `sys_menu` VALUES (146, '大屏设计', 142, '/tenant-design/report', '', 'C', 0, 1, '2026-03-10 15:11:00', '2026-03-10 15:11:00', 0);
INSERT INTO `sys_menu` VALUES (147, '产品管理', 0, '/', '', 'C', 0, 1, '2026-03-10 15:11:21', '2026-03-10 15:11:21', 0);
INSERT INTO `sys_menu` VALUES (148, '指标管理', 147, '/', '', 'C', 0, 1, '2026-03-10 15:11:35', '2026-03-10 15:11:35', 0);
INSERT INTO `sys_menu` VALUES (149, '指标定义', 148, '/tenant-design/product/indicator', '', 'C', 0, 1, '2026-03-10 15:11:57', '2026-03-10 15:11:57', 0);
INSERT INTO `sys_menu` VALUES (150, '指标优先级管理', 148, '/tenant-design/product/indicator/priority', '', 'C', 0, 1, '2026-03-10 15:12:10', '2026-03-10 15:12:10', 0);
INSERT INTO `sys_menu` VALUES (151, '指标历史进度查看', 148, '/tenant-design/product/indicator/history', '', 'C', 0, 1, '2026-03-10 15:12:23', '2026-03-10 15:12:23', 0);
INSERT INTO `sys_menu` VALUES (152, '指标加工管理', 148, '/tenant-design/product/indicator/process', '', 'C', 0, 1, '2026-03-10 15:12:37', '2026-03-10 15:12:37', 0);
INSERT INTO `sys_menu` VALUES (153, '报表管理', 147, '/tenant-design/product/report', '', 'C', 0, 1, '2026-03-10 15:12:48', '2026-03-10 15:12:48', 0);
INSERT INTO `sys_menu` VALUES (154, '大屏管理', 147, '/tenant-design/product/screen', '', 'C', 0, 1, '2026-03-10 15:12:58', '2026-03-10 15:12:58', 0);
INSERT INTO `sys_menu` VALUES (155, '标签管理', 147, '/', '', 'C', 0, 1, '2026-03-10 15:13:10', '2026-03-10 15:13:10', 0);
INSERT INTO `sys_menu` VALUES (156, '标签定义', 155, '/tenant-design/product/tag', '', 'C', 0, 1, '2026-03-10 15:13:16', '2026-03-10 15:13:16', 0);
INSERT INTO `sys_menu` VALUES (157, '标签优先级管理', 155, '/tenant-design/product/tag/priority', '', 'C', 0, 1, '2026-03-10 15:13:32', '2026-03-10 15:13:32', 0);
INSERT INTO `sys_menu` VALUES (158, '标签历史进度查看', 155, '/tenant-design/product/tag/history', '', 'C', 0, 1, '2026-03-10 15:13:43', '2026-03-10 15:13:43', 0);
INSERT INTO `sys_menu` VALUES (159, '标签加工管理', 155, '/tenant-design/product/tag/process', '', 'C', 0, 1, '2026-03-10 15:13:55', '2026-03-10 15:13:55', 0);
INSERT INTO `sys_menu` VALUES (160, '数据集管理', 147, '/tenant-design/product/dataset', '', 'C', 0, 1, '2026-03-10 15:14:28', '2026-03-10 15:14:28', 0);
INSERT INTO `sys_menu` VALUES (161, '个人中心', 0, '/', '', 'C', 0, 1, '2026-03-10 15:14:45', '2026-03-10 15:14:45', 0);
INSERT INTO `sys_menu` VALUES (162, '数据集申请', 161, '/tenant-design/profile/dataset-apply', '', 'C', 0, 1, '2026-03-10 15:15:06', '2026-03-10 15:15:06', 0);
INSERT INTO `sys_menu` VALUES (163, '用户角色申请', 161, '/tenant-design/profile/user-role-apply', '', 'C', 0, 1, '2026-03-10 15:15:17', '2026-03-10 15:15:17', 0);
INSERT INTO `sys_menu` VALUES (164, '全部审批-我发起的', 161, '/tenant-design/profile/approval-mine', '', 'C', 0, 1, '2026-03-10 15:15:32', '2026-03-10 15:15:32', 0);
INSERT INTO `sys_menu` VALUES (165, '全部审批-流经我的', 161, '/tenant-design/profile/approval-through-me', '', 'C', 0, 1, '2026-03-10 15:15:45', '2026-03-10 15:15:45', 0);
INSERT INTO `sys_menu` VALUES (166, '系统管理', 0, '/', '', 'C', 0, 1, '2026-03-10 15:16:01', '2026-03-10 15:16:01', 0);
INSERT INTO `sys_menu` VALUES (167, '角色及权限管理', 166, '/tenant-design/system/role-permission', '', 'C', 0, 1, '2026-03-10 15:16:25', '2026-03-10 15:16:25', 0);
INSERT INTO `sys_menu` VALUES (168, '数据目录管理', 166, '/tenant-design/system/data-catalog', '', 'C', 0, 1, '2026-03-10 15:16:36', '2026-03-10 15:16:36', 0);
INSERT INTO `sys_menu` VALUES (169, '本地数据集发布', 166, '/tenant-design/system/local-dataset-publish', '', 'C', 0, 1, '2026-03-10 15:16:46', '2026-03-10 15:16:46', 0);
INSERT INTO `sys_menu` VALUES (170, '公共数据集申请', 166, '/tenant-design/system/public-dataset-apply', '', 'C', 0, 1, '2026-03-10 15:16:56', '2026-03-10 15:16:56', 0);
INSERT INTO `sys_menu` VALUES (171, '有权审批人配置', 166, '/tenant-design/system/approver-config', '', 'C', 0, 1, '2026-03-10 15:17:08', '2026-03-10 15:17:08', 0);
INSERT INTO `sys_menu` VALUES (172, '数据源管理', 166, '/tenant-design/system/datasource', '', 'C', 0, 1, '2026-03-10 15:17:17', '2026-03-10 15:17:17', 0);
INSERT INTO `sys_menu` VALUES (173, '日志管理', 166, '/tenant-design/system/log', '', 'C', 0, 1, '2026-03-10 15:17:25', '2026-03-10 15:17:25', 0);
INSERT INTO `sys_menu` VALUES (174, '字典管理', 166, '/tenant-design/system/dict', '', 'C', 0, 1, '2026-03-10 15:17:36', '2026-03-10 15:17:36', 0);
INSERT INTO `sys_menu` VALUES (175, '数据集白名单管理', 166, '/tenant-design/system/dataset-whitelist', '', 'C', 0, 1, '2026-03-10 15:17:48', '2026-03-10 15:17:48', 0);
INSERT INTO `sys_menu` VALUES (176, '数据资产管理', 0, '/tenant-design/asset', '', 'C', 0, 1, '2026-03-10 15:18:05', '2026-03-10 15:18:05', 0);

-- ----------------------------
-- Table structure for sys_module
-- ----------------------------
DROP TABLE IF EXISTS `sys_module`;
CREATE TABLE `sys_module`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `module_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模块代码(ABC)',
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模块名称',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_module_code`(`module_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模块表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_module
-- ----------------------------
INSERT INTO `sys_module` VALUES (1, 'A', 'BI工作台', '2026-02-24 19:01:07', '2026-02-24 21:43:16');
INSERT INTO `sys_module` VALUES (2, 'B', '灵活查询中心', '2026-02-24 19:01:07', '2026-02-24 21:43:24');
INSERT INTO `sys_module` VALUES (3, 'C', '产品智能定制', '2026-02-24 19:01:07', '2026-02-24 21:43:38');


-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) NULL DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  `module_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属模块编码',
  `tenant_id` bigint(20) NULL DEFAULT NULL COMMENT '关联租户ID（仅模块C角色有值）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code_module`(`role_code`, `module_code`) USING BTREE,
  INDEX `idx_module_tenant`(`module_code`, `tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'BI工作台超管', 'SUPER_ADMIN', 'BI工作台超管', '2026-02-24 19:01:07', '2026-02-28 10:28:43', 0, 'A', NULL);
INSERT INTO `sys_role` VALUES (2, '灵活查询管理员', 'MODULE_A_ADMIN', '灵活查询管理员', '2026-02-24 19:01:07', '2026-02-28 10:29:00', 0, 'B', NULL);
INSERT INTO `sys_role` VALUES (4, 'BI工作台普通用户', 'MODULE_C_READ', 'BI工作台普通用户', '2026-02-24 19:01:07', '2026-02-28 10:28:34', 0, 'A', NULL);
INSERT INTO `sys_role` VALUES (5, '灵活查询普通用户', 'USER', '灵活查询普通用户', '2026-02-24 19:01:07', '2026-02-28 10:29:06', 0, 'B', NULL);
INSERT INTO `sys_role` VALUES (6, '租户管理员', 'TENANT_ADMIN', '租户超级管理员', '2026-02-28 11:00:54', '2026-03-05 18:24:15', 0, 'C', 1);
INSERT INTO `sys_role` VALUES (7, '产品设计员', 'PRODUCT_DESIGNER', '租户产品设计人员', '2026-02-28 11:00:54', '2026-03-10 15:23:28', 1, 'A', 1);
INSERT INTO `sys_role` VALUES (8, '产品设计员', 'TENANT_VIEWER', '产品设计员', '2026-02-28 11:00:54', '2026-03-05 18:24:19', 0, 'C', 1);
INSERT INTO `sys_role` VALUES (15, '流程管理员', 'FLOW_ADMIN', '流程管理超级管理员', '2026-03-05 09:20:18', '2026-03-05 09:20:18', 0, NULL, NULL);
INSERT INTO `sys_role` VALUES (16, '流程发起人', 'FLOW_STARTER', '可发起流程', '2026-03-05 09:20:18', '2026-03-05 09:20:18', 0, NULL, NULL);
INSERT INTO `sys_role` VALUES (17, '流程审批人', 'FLOW_APPROVER', '可审批流程', '2026-03-05 09:20:18', '2026-03-05 09:20:18', 0, NULL, NULL);
INSERT INTO `sys_role` VALUES (18, '123', 'T', '', '2026-03-10 15:19:45', '2026-03-10 15:19:50', 1, 'A', NULL);
INSERT INTO `sys_role` VALUES (19, '数据集管理员', 'DATA', '', '2026-03-10 15:26:56', '2026-03-10 15:26:56', 0, 'C', NULL);
INSERT INTO `sys_role` VALUES (20, '分行审批员', 'SHENPI', '', '2026-03-10 15:27:17', '2026-03-10 15:27:17', 0, 'C', NULL);


-- ----------------------------
-- Table structure for sys_role_api
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_api`;
CREATE TABLE `sys_role_api`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `api_id` bigint(20) NOT NULL COMMENT '接口ID',
  `module_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模块代码(ABC)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE,
  INDEX `idx_api_id`(`api_id`) USING BTREE,
  INDEX `idx_module_code`(`module_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色接口关联表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_role_api
-- ----------------------------
INSERT INTO `sys_role_api` VALUES (1, 1, 25, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (2, 1, 26, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (3, 1, 27, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (4, 1, 28, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (5, 1, 29, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (6, 1, 30, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (7, 1, 31, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (8, 1, 32, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (9, 1, 33, NULL, '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (10, 1, 1, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (11, 1, 2, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (12, 1, 3, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (13, 1, 4, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (14, 1, 5, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (15, 1, 6, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (16, 1, 7, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (17, 1, 8, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (18, 1, 9, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (19, 1, 10, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (20, 1, 11, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (21, 1, 12, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (22, 1, 13, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (23, 1, 14, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (24, 1, 15, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (25, 1, 16, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (26, 1, 17, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (27, 1, 18, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (28, 1, 19, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (29, 1, 20, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (30, 1, 21, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (31, 1, 22, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (32, 1, 23, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (33, 1, 24, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (64, 2, 1, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (65, 2, 2, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (66, 2, 3, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (67, 2, 4, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (68, 2, 5, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (69, 2, 6, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (70, 2, 7, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (71, 2, 8, 'A', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (79, 3, 9, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (80, 3, 10, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (81, 3, 11, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (82, 3, 12, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (83, 3, 13, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (84, 3, 14, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (85, 3, 15, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (86, 3, 16, 'B', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (94, 4, 17, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (95, 4, 18, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (96, 4, 19, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (97, 4, 20, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (98, 4, 21, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (99, 4, 22, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (100, 4, 23, 'C', '2026-02-24 19:01:07');
INSERT INTO `sys_role_api` VALUES (101, 4, 24, 'C', '2026-02-24 19:01:07');


-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  `module_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模块代码(ABC)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE,
  INDEX `idx_menu_id`(`menu_id`) USING BTREE,
  INDEX `idx_module_code`(`module_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 335 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关联表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (98, 1, 19, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (99, 1, 20, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (100, 1, 21, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (101, 1, 22, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (102, 1, 23, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (103, 1, 24, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (104, 1, 25, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (105, 1, 26, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (106, 1, 27, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (107, 1, 28, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (108, 1, 29, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (109, 1, 30, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (110, 1, 31, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (111, 1, 32, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (112, 1, 33, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (113, 1, 34, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (114, 1, 35, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (115, 1, 36, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (116, 1, 37, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (117, 1, 38, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (118, 1, 39, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (119, 1, 40, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (120, 1, 41, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (121, 1, 42, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (122, 1, 43, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (123, 1, 44, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (124, 1, 45, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (125, 1, 46, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (126, 1, 47, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (127, 1, 48, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (128, 1, 49, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (129, 1, 50, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (130, 1, 51, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (131, 1, 52, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (132, 1, 53, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (133, 1, 54, NULL, '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (134, 1, 55, NULL, '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (135, 1, 56, NULL, '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (136, 1, 57, NULL, '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (137, 1, 58, NULL, '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (138, 2, 1, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (139, 2, 2, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (140, 2, 3, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (141, 2, 4, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (142, 2, 5, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (143, 2, 6, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (144, 2, 7, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (145, 2, 8, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (146, 2, 9, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (147, 2, 10, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (148, 2, 11, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (149, 2, 12, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (150, 2, 13, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (151, 2, 14, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (152, 2, 15, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (153, 2, 16, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (154, 2, 17, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (155, 2, 18, 'A', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (156, 3, 19, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (157, 3, 20, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (158, 3, 21, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (159, 3, 22, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (160, 3, 23, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (161, 3, 24, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (162, 3, 25, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (163, 3, 26, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (164, 3, 27, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (165, 3, 28, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (166, 3, 29, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (167, 3, 30, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (168, 3, 31, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (169, 3, 32, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (170, 3, 33, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (171, 3, 34, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (172, 3, 35, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (173, 3, 36, 'B', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (174, 4, 37, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (175, 4, 38, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (176, 4, 39, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (177, 4, 40, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (178, 4, 41, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (179, 4, 42, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (180, 4, 43, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (181, 4, 44, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (182, 4, 45, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (183, 4, 46, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (184, 4, 47, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (185, 4, 48, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (186, 4, 49, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (187, 4, 50, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (188, 4, 51, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (189, 4, 52, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (190, 4, 53, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (191, 4, 54, 'C', '2026-03-10 14:35:31', NULL);
INSERT INTO `sys_role_menu` VALUES (225, 1, 73, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (226, 1, 138, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (227, 1, 139, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (228, 1, 140, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (229, 1, 74, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (230, 1, 131, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (231, 1, 132, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (232, 1, 133, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (233, 1, 134, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (234, 1, 135, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (235, 1, 136, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (236, 1, 137, 'A', '2026-03-10 15:07:21', NULL);
INSERT INTO `sys_role_menu` VALUES (299, 6, 141, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (300, 6, 142, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (301, 6, 143, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (302, 6, 144, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (303, 6, 145, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (304, 6, 146, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (305, 6, 147, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (306, 6, 148, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (307, 6, 149, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (308, 6, 150, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (309, 6, 151, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (310, 6, 152, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (311, 6, 153, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (312, 6, 154, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (313, 6, 155, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (314, 6, 156, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (315, 6, 157, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (316, 6, 158, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (317, 6, 159, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (318, 6, 160, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (319, 6, 161, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (320, 6, 162, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (321, 6, 163, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (322, 6, 164, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (323, 6, 165, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (324, 6, 166, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (325, 6, 167, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (326, 6, 168, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (327, 6, 169, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (328, 6, 170, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (329, 6, 171, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (330, 6, 172, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (331, 6, 173, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (332, 6, 174, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (333, 6, 175, 'C', '2026-03-10 16:01:35', NULL);
INSERT INTO `sys_role_menu` VALUES (334, 6, 176, 'C', '2026-03-10 16:01:35', NULL);


-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户名称',
  `tenant_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编码（唯一）',
  `module_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'C' COMMENT '所属模块（固定为C）',
  `status` int(11) NULL DEFAULT 1 COMMENT '状态（1启用 0禁用）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_code`(`tenant_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模块C租户表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, '公共租户', 'TENANT_001', 'C', 1, '2026-02-28 11:00:54', '2026-02-28 14:15:27');
INSERT INTO `sys_tenant` VALUES (2, 'CMM租户', 'TENANT_002', 'C', 1, '2026-02-28 11:00:54', '2026-02-28 14:15:32');
INSERT INTO `sys_tenant` VALUES (3, 'ALM租户', 'TENANT_003', 'C', 1, '2026-02-28 11:00:54', '2026-02-28 14:15:36');


-- ----------------------------
-- Table structure for sys_tenant_menu_status
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_menu_status`;
CREATE TABLE `sys_tenant_menu_status`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
  `menu_id` bigint(20) NOT NULL COMMENT '关联sys_menu的菜单ID',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '菜单状态（1启用0禁用，仅对该租户生效）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) NULL DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_menu`(`tenant_id`, `menu_id`) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id`) USING BTREE,
  INDEX `idx_menu_id`(`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户菜单状态表（隔离各租户的菜单启用状态）' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_tenant_menu_status
-- ----------------------------
INSERT INTO `sys_tenant_menu_status` VALUES (1, 1, 20, 0, '2026-03-05 17:25:44', '2026-03-05 17:25:44', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (2, 1, 23, 1, '2026-03-05 17:25:44', '2026-03-05 17:25:44', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (3, 1, 27, 1, '2026-03-05 17:25:44', '2026-03-05 17:25:44', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (4, 2, 20, 1, '2026-03-05 17:36:31', '2026-03-05 17:36:31', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (5, 2, 23, 1, '2026-03-05 17:36:31', '2026-03-05 17:36:31', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (6, 2, 27, 1, '2026-03-05 17:36:31', '2026-03-05 17:36:31', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (7, 3, 20, 1, '2026-03-05 17:36:38', '2026-03-05 17:36:38', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (8, 3, 23, 1, '2026-03-05 17:36:38', '2026-03-05 17:36:38', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (9, 3, 27, 1, '2026-03-05 17:36:38', '2026-03-05 17:36:38', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (10, 1, 46, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (11, 1, 47, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (12, 1, 48, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (13, 1, 49, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (14, 1, 50, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (15, 1, 51, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (16, 1, 52, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (17, 1, 53, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (18, 1, 54, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (19, 1, 55, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);
INSERT INTO `sys_tenant_menu_status` VALUES (20, 1, 56, 1, '2026-03-06 10:03:25', '2026-03-06 10:03:25', 0);


-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `status` int(11) NULL DEFAULT 1 COMMENT '状态(1启用0禁用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) NULL DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '123456', 1, '2026-02-24 19:01:07', '2026-02-24 21:18:07', 0, NULL);
INSERT INTO `sys_user` VALUES (2, 'userA', '123456', 1, '2026-02-24 19:01:07', '2026-02-24 21:18:10', 0, NULL);
INSERT INTO `sys_user` VALUES (3, 'userB', '123456', 1, '2026-02-24 19:01:07', '2026-02-24 21:18:13', 0, NULL);
INSERT INTO `sys_user` VALUES (4, 'userC', '123456', 1, '2026-02-24 19:01:07', '2026-02-24 21:18:17', 0, NULL);
INSERT INTO `sys_user` VALUES (5, 'gggg', '$2a$10$Tkmjt5XXo96CrZ8eLCHi2.QF/YP3wF0Hw.7eRg.HyZT3q/rUUomiW', 1, '2026-03-06 15:37:21', '2026-03-06 15:37:21', 0, NULL);

-- ----------------------------
-- Table structure for sys_user_asset
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_asset`;
CREATE TABLE `sys_user_asset`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint(20) NOT NULL COMMENT '关联用户ID',
  `asset_id` bigint(20) NOT NULL COMMENT '关联资产ID',
  `module_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属模块（A/B/C）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_asset`(`user_id`, `asset_id`) USING BTREE COMMENT '用户+资产唯一索引',
  INDEX `idx_user_module`(`user_id`, `module_code`) USING BTREE COMMENT '用户+模块索引'
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户资产关联表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user_asset
-- ----------------------------
INSERT INTO `sys_user_asset` VALUES (1, 1, 1, 'A', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (2, 1, 3, 'A', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (3, 1, 5, 'A', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (4, 1, 7, 'B', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (5, 1, 11, 'B', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (6, 1, 12, 'C', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (7, 2, 2, 'A', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (8, 2, 4, 'A', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (9, 2, 6, 'A', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (10, 3, 8, 'B', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (11, 3, 10, 'B', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (12, 3, 11, 'B', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (13, 4, 13, 'C', '2026-02-28 10:15:25');
INSERT INTO `sys_user_asset` VALUES (14, 4, 14, 'C', '2026-02-28 10:15:25');


-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `module_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属模块编码',
  `tenant_id` bigint(20) NULL DEFAULT NULL COMMENT '关联租户ID（仅模块C）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE,
  INDEX `idx_module_tenant`(`module_code`, `tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (2, 2, 2, '2026-02-24 19:01:07', 'B', NULL);
INSERT INTO `sys_user_role` VALUES (3, 3, 3, '2026-02-24 19:01:07', 'C', NULL);
INSERT INTO `sys_user_role` VALUES (4, 4, 4, '2026-02-24 19:01:07', 'A', NULL);
INSERT INTO `sys_user_role` VALUES (6, 3, 1, '2026-02-27 15:43:27', 'A', NULL);
INSERT INTO `sys_user_role` VALUES (7, 4, 1, '2026-02-27 15:43:27', 'A', NULL);
INSERT INTO `sys_user_role` VALUES (11, 1, 1, '2026-02-28 10:41:25', 'A', NULL);
INSERT INTO `sys_user_role` VALUES (12, 1, 2, '2026-02-28 10:41:25', 'B', NULL);
INSERT INTO `sys_user_role` VALUES (13, 1, 4, '2026-02-28 10:41:25', 'A', NULL);
INSERT INTO `sys_user_role` VALUES (14, 1, 3, '2026-02-28 10:41:25', 'C', NULL);
INSERT INTO `sys_user_role` VALUES (15, 1, 5, '2026-02-28 10:41:25', 'B', NULL);
INSERT INTO `sys_user_role` VALUES (19, 1, 9, '2026-02-28 14:14:17', 'C', 2);
INSERT INTO `sys_user_role` VALUES (20, 1, 10, '2026-02-28 14:14:17', 'C', 2);
INSERT INTO `sys_user_role` VALUES (21, 5, 1, '2026-03-09 15:38:11', 'A', NULL);
INSERT INTO `sys_user_role` VALUES (22, 1, 6, '2026-03-09 15:38:23', 'C', 1);
INSERT INTO `sys_user_role` VALUES (23, 1, 7, '2026-03-09 15:38:23', 'C', 1);
INSERT INTO `sys_user_role` VALUES (24, 2, 1, '2026-03-09 15:43:11', 'A', NULL);
INSERT INTO `sys_user_role` VALUES (25, 2, 4, '2026-03-09 15:43:11', 'A', NULL);