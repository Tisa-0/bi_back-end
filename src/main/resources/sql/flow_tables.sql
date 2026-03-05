-- =============================================
-- 流程管理核心表（集成到主项目）
-- 复用主项目sys_user、sys_role、sys_menu表
-- =============================================

USE right_manage;

-- =============================================
-- 1. 流程定义表
-- 存储流程模板信息，关联主项目角色控制发起权限
-- =============================================
CREATE TABLE IF NOT EXISTS flow_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流程定义ID',
    flow_key VARCHAR(100) NOT NULL COMMENT '流程标识（唯一）',
    flow_name VARCHAR(200) NOT NULL COMMENT '流程名称',
    flow_category VARCHAR(100) DEFAULT '' COMMENT '流程分类',
    description TEXT COMMENT '流程描述',
    version INT DEFAULT 1 COMMENT '版本号',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态（DRAFT草稿/PUBLISHED已发布/DISABLED已禁用）',
    
    -- 流程设计JSON（节点和连线）
    nodes_json TEXT COMMENT '节点JSON（前端流程设计器使用）',
    edges_json TEXT COMMENT '连线JSON（前端流程设计器使用）',
    
    -- 发起权限控制（关联主项目sys_role.id，多个用逗号分隔）
    starter_role_ids VARCHAR(500) DEFAULT '' COMMENT '可发起流程的角色ID（关联sys_role.id）',
    
    -- 表单配置（可选）
    form_type VARCHAR(50) DEFAULT 'NONE' COMMENT '表单类型（NONE无表单/BUILTIN内置表单/EXTERNAL外部表单）',
    form_config TEXT COMMENT '表单配置JSON',
    
    -- 关联主项目用户（创建者）
    creator_id BIGINT COMMENT '创建人ID（关联sys_user.id）',
    
    -- 租户（可选，兼容多租户）
    tenant_id BIGINT DEFAULT NULL COMMENT '租户ID（关联sys_tenant.id）',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
    
    UNIQUE KEY uk_flow_key (flow_key),
    INDEX idx_status (status),
    INDEX idx_creator_id (creator_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- =============================================
-- 2. 流程节点权限表
-- 存储每个节点的审批角色配置
-- =============================================
CREATE TABLE IF NOT EXISTS flow_node_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    flow_definition_id BIGINT NOT NULL COMMENT '流程定义ID（关联flow_definition.id）',
    node_id VARCHAR(100) NOT NULL COMMENT '节点ID（对应nodes_json中的节点ID）',
    node_name VARCHAR(200) COMMENT '节点名称',
    node_type VARCHAR(50) COMMENT '节点类型（START开始/APPROVAL审批/END结束/CC抄送/GATEWAY网关）',
    
    -- 审批人类型（关联主项目角色/用户）
    assignee_type VARCHAR(50) DEFAULT 'ROLE' COMMENT '处理人类型（ROLE角色/SPECIFIC_USER指定用户/INITIATOR发起人）',
    assignee_ids VARCHAR(500) DEFAULT '' COMMENT '处理人ID（角色时存sys_role.id，用户时存sys_user.id，多个逗号分隔）',
    
    -- 审批配置
    allow_delegate INT DEFAULT 1 COMMENT '允许转办（1是0否）',
    allow_reject INT DEFAULT 1 COMMENT '允许驳回（1是0否）',
    due_hours INT DEFAULT 0 COMMENT '办理时限（小时，0表示不限）',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_flow_node (flow_definition_id, node_id),
    INDEX idx_flow_id (flow_definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点权限表';

-- =============================================
-- 3. 流程实例表
-- 存储流程运行实例
-- =============================================
CREATE TABLE IF NOT EXISTS flow_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流程实例ID',
    instance_key VARCHAR(100) NOT NULL COMMENT '流程标识',
    instance_name VARCHAR(200) NOT NULL COMMENT '流程实例名称（标题）',
    flow_definition_id BIGINT NOT NULL COMMENT '流程定义ID（关联flow_definition.id）',
    flow_name VARCHAR(200) COMMENT '流程名称',
    
    -- 流程状态
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态（RUNNING运行中/COMPLETED已完成/REJECTED已驳回/CANCELLED已撤回）',
    current_node_ids VARCHAR(500) DEFAULT '' COMMENT '当前节点ID列表（多个逗号分隔）',
    current_node_names VARCHAR(500) DEFAULT '' COMMENT '当前节点名称列表',
    
    -- 申请人（关联主项目sys_user）
    applicant_id BIGINT NOT NULL COMMENT '申请人ID（关联sys_user.id）',
    applicant_name VARCHAR(100) COMMENT '申请人姓名',
    
    -- 当前处理人（关联主项目sys_user，多个用逗号分隔）
    current_handler_ids VARCHAR(500) DEFAULT '' COMMENT '当前处理人ID列表（关联sys_user.id）',
    current_handler_names VARCHAR(500) DEFAULT '' COMMENT '当前处理人姓名列表',
    
    -- 流程变量（JSON格式，存储表单数据）
    variables TEXT COMMENT '流程变量JSON',
    
    -- 租户
    tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    end_time DATETIME COMMENT '结束时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
    
    INDEX idx_flow_def_id (flow_definition_id),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- =============================================
-- 4. 流程任务表
-- 存储待办/已办任务
-- =============================================
CREATE TABLE IF NOT EXISTS flow_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_key VARCHAR(100) NOT NULL COMMENT '任务标识',
    process_instance_id BIGINT NOT NULL COMMENT '流程实例ID（关联flow_instance.id）',
    process_definition_id BIGINT NOT NULL COMMENT '流程定义ID',
    process_title VARCHAR(200) COMMENT '流程标题',
    
    -- 节点信息
    node_id VARCHAR(100) NOT NULL COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    node_type VARCHAR(50) COMMENT '节点类型',
    
    -- 处理人（关联主项目sys_user）
    assignee_id BIGINT COMMENT '处理人ID（关联sys_user.id）',
    assignee_name VARCHAR(100) COMMENT '处理人姓名',
    
    -- 申请人
    initiator_id BIGINT COMMENT '申请人ID',
    initiator_name VARCHAR(100) COMMENT '申请人姓名',
    
    -- 任务状态
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态（PENDING待处理/APPROVED已通过/REJECTED已驳回/DELEGATED已转办/CANCELLED已取消）',
    
    -- 审批意见
    comment TEXT COMMENT '审批意见',
    
    -- 租户
    tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    complete_time DATETIME COMMENT '完成时间',
    due_time DATETIME COMMENT '应完成时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
    
    INDEX idx_process_instance_id (process_instance_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务表';

-- =============================================
-- 5. 流程日志表
-- 记录流程操作历史
-- =============================================
CREATE TABLE IF NOT EXISTS flow_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    process_instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    node_id VARCHAR(100) COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    action VARCHAR(50) NOT NULL COMMENT '操作类型（START发起/APPROVED通过/REJECTED驳回/DELEGATED转办/CANCELLED撤回/AUTO_PASS自动通过）',
    
    -- 操作人（关联主项目sys_user）
    operator_id BIGINT COMMENT '操作人ID（关联sys_user.id）',
    operator_name VARCHAR(100) COMMENT '操作人姓名',
    
    comment TEXT COMMENT '审批意见',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    
    INDEX idx_process_instance_id (process_instance_id),
    INDEX idx_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程日志表';

-- =============================================
-- 6. 流程抄送记录表
-- =============================================
CREATE TABLE IF NOT EXISTS flow_cc_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    process_instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    node_id VARCHAR(100) COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    
    -- 抄送人（关联主项目sys_user）
    cc_user_id BIGINT NOT NULL COMMENT '被抄送人ID',
    cc_user_name VARCHAR(100) COMMENT '被抄送人姓名',
    
    -- 抄送人（关联主项目sys_user）
    sender_id BIGINT COMMENT '抄送人ID',
    sender_name VARCHAR(100) COMMENT '抄送人姓名',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_read INT DEFAULT 0 COMMENT '是否已读（0未读1已读）',
    
    INDEX idx_process_instance_id (process_instance_id),
    INDEX idx_cc_user_id (cc_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程抄送记录表';

-- =============================================
-- 初始化流程管理菜单数据
-- =============================================
INSERT INTO sys_menu (menu_name, parent_id, path, component, module_code, sort, status) VALUES
-- 流程管理一级菜单
('流程管理', 0, '/flow', 'flow/Index', NULL, 50, 1),

-- 流程定义子菜单
('流程定义', 0, '/flow/definition', 'flow/definition/List', NULL, 1, 1),
('流程设计', 0, '/flow/definition/design/:id', 'flow/definition/Design', NULL, 2, 1),

-- 流程实例子菜单
('我的发起', 0, '/flow/instance/my', 'flow/instance/My', NULL, 3, 1),
('全部实例', 0, '/flow/instance/all', 'flow/instance/All', NULL, 4, 1),

-- 任务待办子菜单
('我的待办', 0, '/flow/task/todo', 'flow/task/Todo', NULL, 5, 1),
('我的已办', 0, '/flow/task/done', 'flow/task/Done', NULL, 6, 1),
('抄送我的', 0, '/flow/task/cc', 'flow/task/CC', NULL, 7, 1);

-- =============================================
-- 初始化流程管理相关角色
-- =============================================
INSERT INTO sys_role (role_name, role_code, description, module_code) VALUES
('流程管理员', 'FLOW_ADMIN', '流程管理超级管理员', NULL),
('流程发起人', 'FLOW_STARTER', '可发起流程', NULL),
('流程审批人', 'FLOW_APPROVER', '可审批流程', NULL);

-- =============================================
-- 初始化流程定义测试数据
-- =============================================
INSERT INTO flow_definition (flow_key, flow_name, flow_category, description, version, status, nodes_json, edges_json, starter_role_ids, creator_id) VALUES
('test1', 'test1', '审批流程', '测试流程1', 1, 'PUBLISHED', 
'[{"id":"node_1","name":"开始","type":"START","assigneeType":"SPECIFIC_USER","positionX":45,"positionY":120},{"id":"node_2","name":"审批","type":"APPROVAL","assigneeType":"ROLE","assigneeId":"role_manager","assigneeName":"部门经理","positionX":60,"positionY":225},{"id":"node_3","name":"结束","type":"END","assigneeType":"SPECIFIC_USER","positionX":105,"positionY":345}]',
'[{"id":"edge_1","sourceNodeId":"node_1","targetNodeId":"node_2"},{"id":"edge_2","sourceNodeId":"node_2","targetNodeId":"node_3"}]',
'', 1);

-- 初始化流程节点权限
INSERT INTO flow_node_permission (flow_definition_id, node_id, node_name, node_type, assignee_type, assignee_ids) VALUES
(1, 'node_1', '开始', 'START', 'SPECIFIC_USER', ''),
(1, 'node_2', '审批', 'APPROVAL', 'ROLE', ''),
(1, 'node_3', '结束', 'END', 'SPECIFIC_USER', '');
