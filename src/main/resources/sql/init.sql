-- 创建数据库
CREATE DATABASE IF NOT EXISTS right_manage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE right_manage;

-- 模块表
CREATE TABLE IF NOT EXISTS sys_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_code VARCHAR(50) NOT NULL COMMENT '模块代码(ABC)',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_module_code (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块表';

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    path VARCHAR(200) COMMENT '路由路径',
    component VARCHAR(200) COMMENT '组件路径',
    module_code VARCHAR(50) COMMENT '所属模块(ABC)',
    sort INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态(1启用0禁用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
    INDEX idx_module_code (module_code),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 接口表
CREATE TABLE IF NOT EXISTS sys_api (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_path VARCHAR(200) NOT NULL COMMENT '接口路径',
    api_desc VARCHAR(200) COMMENT '接口描述',
    module_code VARCHAR(50) COMMENT '所属模块(ABC)',
    request_method VARCHAR(10) DEFAULT 'GET' COMMENT '请求方法',
    status INT DEFAULT 1 COMMENT '状态(1启用0禁用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
    INDEX idx_module_code (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(500) COMMENT '描述',
    module_code VARCHAR(10) COMMENT '所属模块编码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
    UNIQUE KEY uk_role_code_module (role_code, module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码',
    status INT DEFAULT 1 COMMENT '状态(1启用0禁用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    module_code VARCHAR(50) COMMENT '模块代码(ABC)',
    tenant_id BIGINT(20) DEFAULT NULL COMMENT '关联租户ID（仅模块C租户角色有值）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id),
    INDEX idx_module_code (module_code),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 角色接口关联表
CREATE TABLE IF NOT EXISTS sys_role_api (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    api_id BIGINT NOT NULL COMMENT '接口ID',
    module_code VARCHAR(50) COMMENT '模块代码(ABC)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_role_id (role_id),
    INDEX idx_api_id (api_id),
    INDEX idx_module_code (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色接口关联表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    module_code VARCHAR(10) COMMENT '所属模块编码',
    tenant_id BIGINT(20) DEFAULT NULL COMMENT '关联租户ID（仅模块C）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    INDEX idx_module_tenant (module_code, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 插入模块数据
INSERT INTO sys_module (module_code, module_name) VALUES
('A', '模块A'),
('B', '模块B'),
('C', '模块C');

-- 插入菜单数据 - 模块A（支持三级和四级菜单）
INSERT INTO sys_menu (menu_name, parent_id, path, component, module_code, sort, status) VALUES
('模块A首页', 0, '/module-a/index', 'module-a/Index', 'A', 1, 1),
('模块A系统管理', 0, '/module-a/system', 'module-a/System', 'A', 2, 1),
('模块A用户管理', 3, '/module-a/user', 'module-a/User', 'A', 1, 1),
('模块A用户列表', 4, '/module-a/user/list', 'module-a/UserList', 'A', 1, 1),
('模块A用户详情', 4, '/module-a/user/detail', 'module-a/UserDetail', 'A', 2, 1),
('模块A角色管理', 3, '/module-a/role', 'module-a/Role', 'A', 2, 1),
('模块A角色列表', 6, '/module-a/role/list', 'module-a/RoleList', 'A', 1, 1),
('模块A角色权限', 6, '/module-a/role/permission', 'module-a/RolePermission', 'A', 2, 1),
('模块A菜单管理', 0, '/module-a/menu', 'module-a/Menu', 'A', 3, 1),
('模块A菜单列表', 9, '/module-a/menu/list', 'module-a/MenuList', 'A', 1, 1),
('模块A菜单详情', 9, '/module-a/menu/detail', 'module-a/MenuDetail', 'A', 2, 1),
('模块A接口管理', 0, '/module-a/api', 'module-a/Api', 'A', 4, 1),
('模块A接口列表', 12, '/module-a/api/list', 'module-a/ApiList', 'A', 1, 1);

-- 插入菜单数据 - 模块B（支持三级和四级菜单）
INSERT INTO sys_menu (menu_name, parent_id, path, component, module_code, sort, status) VALUES
('模块B首页', 0, '/module-b/index', 'module-b/Index', 'B', 1, 1),
('模块B系统管理', 0, '/module-b/system', 'module-b/System', 'B', 2, 1),
('模块B用户管理', 14, '/module-b/user', 'module-b/User', 'B', 1, 1),
('模块B用户列表', 15, '/module-b/user/list', 'module-b/UserList', 'B', 1, 1),
('模块B用户详情', 15, '/module-b/user/detail', 'module-b/UserDetail', 'B', 2, 1),
('模块B角色管理', 14, '/module-b/role', 'module-b/Role', 'B', 2, 1),
('模块B角色列表', 17, '/module-b/role/list', 'module-b/RoleList', 'B', 1, 1),
('模块B角色权限', 17, '/module-b/role/permission', 'module-b/RolePermission', 'B', 2, 1),
('模块B菜单管理', 0, '/module-b/menu', 'module-b/Menu', 'B', 3, 1),
('模块B菜单列表', 20, '/module-b/menu/list', 'module-b/MenuList', 'B', 1, 1),
('模块B菜单详情', 20, '/module-b/menu/detail', 'module-b/MenuDetail', 'B', 2, 1),
('模块B接口管理', 0, '/module-b/api', 'module-b/Api', 'B', 4, 1),
('模块B接口列表', 23, '/module-b/api/list', 'module-b/ApiList', 'B', 1, 1);

-- 插入菜单数据 - 模块C（支持三级和四级菜单）
INSERT INTO sys_menu (menu_name, parent_id, path, component, module_code, sort, status) VALUES
('模块C首页', 0, '/module-c/index', 'module-c/Index', 'C', 1, 1),
('模块C系统管理', 0, '/module-c/system', 'module-c/System', 'C', 2, 1),
('模块C用户管理', 26, '/module-c/user', 'module-c/User', 'C', 1, 1),
('模块C用户列表', 27, '/module-c/user/list', 'module-c/UserList', 'C', 1, 1),
('模块C用户详情', 27, '/module-c/user/detail', 'module-c/UserDetail', 'C', 2, 1),
('模块C角色管理', 26, '/module-c/role', 'module-c/Role', 'C', 2, 1),
('模块C角色列表', 29, '/module-c/role/list', 'module-c/RoleList', 'C', 1, 1),
('模块C角色权限', 29, '/module-c/role/permission', 'module-c/RolePermission', 'C', 2, 1),
('模块C菜单管理', 0, '/module-c/menu', 'module-c/Menu', 'C', 3, 1),
('模块C菜单列表', 32, '/module-c/menu/list', 'module-c/MenuList', 'C', 1, 1),
('模块C菜单详情', 32, '/module-c/menu/detail', 'module-c/MenuDetail', 'C', 2, 1),
('模块C接口管理', 0, '/module-c/api', 'module-c/Api', 'C', 4, 1),
('模块C接口列表', 35, '/module-c/api/list', 'module-c/ApiList', 'C', 1, 1);

-- 插入公共菜单
INSERT INTO sys_menu (menu_name, parent_id, path, component, module_code, sort, status) VALUES
('系统管理', 0, '/system', 'system/Index', NULL, 100, 1),
('用户管理', 17, '/system/user', 'system/User', NULL, 1, 1),
('角色管理', 17, '/system/role', 'system/Role', NULL, 2, 1),
('权限管理', 0, '/permission', 'permission/Index', NULL, 101, 1);

-- 插入接口数据 - 模块A
INSERT INTO sys_api (api_path, api_desc, module_code, request_method, status) VALUES
('/api/module-a/menu/list', '获取模块A菜单列表', 'A', 'GET', 1),
('/api/module-a/menu/add', '新增模块A菜单', 'A', 'POST', 1),
('/api/module-a/menu/update', '修改模块A菜单', 'A', 'PUT', 1),
('/api/module-a/menu/delete', '删除模块A菜单', 'A', 'DELETE', 1),
('/api/module-a/api/list', '获取模块A接口列表', 'A', 'GET', 1),
('/api/module-a/api/add', '新增模块A接口', 'A', 'POST', 1),
('/api/module-a/api/update', '修改模块A接口', 'A', 'PUT', 1),
('/api/module-a/api/delete', '删除模块A接口', 'A', 'DELETE', 1);

-- 插入接口数据 - 模块B
INSERT INTO sys_api (api_path, api_desc, module_code, request_method, status) VALUES
('/api/module-b/menu/list', '获取模块B菜单列表', 'B', 'GET', 1),
('/api/module-b/menu/add', '新增模块B菜单', 'B', 'POST', 1),
('/api/module-b/menu/update', '修改模块B菜单', 'B', 'PUT', 1),
('/api/module-b/menu/delete', '删除模块B菜单', 'B', 'DELETE', 1),
('/api/module-b/api/list', '获取模块B接口列表', 'B', 'GET', 1),
('/api/module-b/api/add', '新增模块B接口', 'B', 'POST', 1),
('/api/module-b/api/update', '修改模块B接口', 'B', 'PUT', 1),
('/api/module-b/api/delete', '删除模块B接口', 'B', 'DELETE', 1);

-- 插入接口数据 - 模块C
INSERT INTO sys_api (api_path, api_desc, module_code, request_method, status) VALUES
('/api/module-c/menu/list', '获取模块C菜单列表', 'C', 'GET', 1),
('/api/module-c/menu/add', '新增模块C菜单', 'C', 'POST', 1),
('/api/module-c/menu/update', '修改模块C菜单', 'C', 'PUT', 1),
('/api/module-c/menu/delete', '删除模块C菜单', 'C', 'DELETE', 1),
('/api/module-c/api/list', '获取模块C接口列表', 'C', 'GET', 1),
('/api/module-c/api/add', '新增模块C接口', 'C', 'POST', 1),
('/api/module-c/api/update', '修改模块C接口', 'C', 'PUT', 1),
('/api/module-c/api/delete', '删除模块C接口', 'C', 'DELETE', 1);

-- 插入公共接口
INSERT INTO sys_api (api_path, api_desc, module_code, request_method, status) VALUES
('/api/user/list', '获取用户列表', NULL, 'GET', 1),
('/api/user/add', '新增用户', NULL, 'POST', 1),
('/api/user/update', '修改用户', NULL, 'PUT', 1),
('/api/user/delete', '删除用户', NULL, 'DELETE', 1),
('/api/role/list', '获取角色列表', NULL, 'GET', 1),
('/api/role/add', '新增角色', NULL, 'POST', 1),
('/api/role/update', '修改角色', NULL, 'PUT', 1),
('/api/role/delete', '删除角色', NULL, 'DELETE', 1),
('/api/module/list', '获取模块列表', NULL, 'GET', 1);

-- 插入角色数据
INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有所有权限'),
('模块A管理员', 'MODULE_A_ADMIN', '模块A管理员'),
('模块B操作员', 'MODULE_B_OPERATOR', '模块B操作员'),
('模块C只读', 'MODULE_C_READ', '模块C只读用户'),
('普通用户', 'USER', '普通用户');

-- 插入用户数据 (密码都是123456，使用BCrypt加密)
INSERT INTO sys_user (username, password, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 1),
('userA', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 1),
('userB', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 1),
('userC', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 1);

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin -> 超级管理员
(2, 2),  -- userA -> 模块A管理员
(3, 3),  -- userB -> 模块B操作员
(4, 4);  -- userC -> 模块C只读

-- 角色菜单关联 - 超级管理员拥有所有菜单
INSERT INTO sys_role_menu (role_id, menu_id, module_code) VALUES
(1, 1, 'A'),
(1, 2, 'A'),
(1, 3, 'A'),
(1, 4, 'A'),
(1, 5, 'A'),
(1, 6, 'B'),
(1, 7, 'B'),
(1, 8, 'B'),
(1, 9, 'B'),
(1, 10, 'B'),
(1, 11, 'C'),
(1, 12, 'C'),
(1, 13, 'C'),
(1, 14, 'C'),
(1, 15, 'C'),
(1, 17, NULL),
(1, 18, NULL),
(1, 19, NULL),
(1, 21, NULL),
(1, 22, NULL);

-- 角色菜单关联 - 模块A管理员
INSERT INTO sys_role_menu (role_id, menu_id, module_code) VALUES
(2, 1, 'A'),
(2, 2, 'A'),
(2, 3, 'A'),
(2, 4, 'A'),
(2, 5, 'A');

-- 角色菜单关联 - 模块B操作员
INSERT INTO sys_role_menu (role_id, menu_id, module_code) VALUES
(3, 6, 'B'),
(3, 7, 'B'),
(3, 8, 'B'),
(3, 9, 'B'),
(3, 10, 'B');

-- 角色菜单关联 - 模块C只读
INSERT INTO sys_role_menu (role_id, menu_id, module_code) VALUES
(4, 11, 'C'),
(4, 12, 'C'),
(4, 13, 'C'),
(4, 14, 'C'),
(4, 15, 'C');

-- 角色接口关联 - 超级管理员拥有所有接口
INSERT INTO sys_role_api (role_id, api_id, module_code)
SELECT 1, id, module_code FROM sys_api;

-- 角色接口关联 - 模块A管理员
INSERT INTO sys_role_api (role_id, api_id, module_code)
SELECT 2, id, module_code FROM sys_api WHERE module_code = 'A';

-- 角色接口关联 - 模块B操作员
INSERT INTO sys_role_api (role_id, api_id, module_code)
SELECT 3, id, module_code FROM sys_api WHERE module_code = 'B';

-- 角色接口关联 - 模块C只读
INSERT INTO sys_role_api (role_id, api_id, module_code)
SELECT 4, id, module_code FROM sys_api WHERE module_code = 'C';

-- 资产库表（资产基础表）
CREATE TABLE IF NOT EXISTS sys_asset (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '资产ID',
  module_code varchar(10) NOT NULL COMMENT '所属模块（A/B/C）',
  asset_name varchar(100) NOT NULL COMMENT '资产名称',
  asset_code varchar(50) DEFAULT '' COMMENT '资产编码',
  asset_type varchar(30) DEFAULT '' COMMENT '资产类型（如设备、权限、资源）',
  asset_desc varchar(255) DEFAULT '' COMMENT '资产描述',
  status tinyint(1) DEFAULT 1 COMMENT '状态（1启用 0禁用）',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted int DEFAULT 0 COMMENT '逻辑删除(0未删除1已删除)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_asset_code (module_code, asset_code) COMMENT '模块+资产编码唯一索引',
  KEY idx_module_code (module_code) COMMENT '模块索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产库表';

-- 用户资产关联表（用户绑定资产）
CREATE TABLE IF NOT EXISTS sys_user_asset (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id bigint(20) NOT NULL COMMENT '关联用户ID',
  asset_id bigint(20) NOT NULL COMMENT '关联资产ID',
  module_code varchar(10) NOT NULL COMMENT '所属模块（A/B/C）',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_asset (user_id, asset_id) COMMENT '用户+资产唯一索引',
  KEY idx_user_module (user_id, module_code) COMMENT '用户+模块索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资产关联表';

-- 初始化资产库数据
INSERT INTO sys_asset (module_code, asset_name, asset_code, asset_type, asset_desc, status) VALUES
-- 模块A资产
('A', '模块A-设备001', 'ASSET-A-001', '设备', '模块A的通用设备', 1),
('A', '模块A-设备002', 'ASSET-A-002', '设备', '模块A的通用设备', 1),
('A', '模块A-权限001', 'ASSET-A-003', '权限', '模块A的管理权限', 1),
('A', '模块A-权限002', 'ASSET-A-004', '权限', '模块A的操作权限', 1),
('A', '模块A-资源001', 'ASSET-A-005', '资源', '模块A的业务资源', 1),
('A', '模块A-资源002', 'ASSET-A-006', '资源', '模块A的数据资源', 1),
-- 模块B资产
('B', '模块B-设备001', 'ASSET-B-001', '设备', '模块B的通用设备', 1),
('B', '模块B-设备002', 'ASSET-B-002', '设备', '模块B的通用设备', 1),
('B', '模块B-权限001', 'ASSET-B-003', '权限', '模块B的管理权限', 1),
('B', '模块B-权限002', 'ASSET-B-004', '权限', '模块B的操作权限', 1),
('B', '模块B-资源001', 'ASSET-B-005', '资源', '模块B的业务资源', 1),
-- 模块C资产
('C', '模块C-设备001', 'ASSET-C-001', '设备', '模块C的通用设备', 1),
('C', '模块C-设备002', 'ASSET-C-002', '设备', '模块C的通用设备', 1),
('C', '模块C-权限001', 'ASSET-C-003', '权限', '模块C的只读权限', 1),
('C', '模块C-权限002', 'ASSET-C-004', '权限', '模块C的操作权限', 1),
('C', '模块C-资源001', 'ASSET-C-005', '资源', '模块C的业务资源', 1);

-- 初始化用户资产绑定数据
INSERT INTO sys_user_asset (user_id, asset_id, module_code) VALUES
-- admin 绑定的资产
(1, 1, 'A'),  -- admin -> 模块A-设备001
(1, 3, 'A'),  -- admin -> 模块A-权限001
(1, 5, 'A'),  -- admin -> 模块A-资源001
(1, 7, 'B'),  -- admin -> 模块B-设备001
(1, 11, 'B'), -- admin -> 模块B-资源001
(1, 12, 'C'), -- admin -> 模块C-设备001
-- userA 绑定的资产
(2, 2, 'A'),  -- userA -> 模块A-设备002
(2, 4, 'A'),  -- userA -> 模块A-权限002
(2, 6, 'A'),  -- userA -> 模块A-资源002
-- userB 绑定的资产
(3, 8, 'B'),  -- userB -> 模块B-设备002
(3, 10, 'B'), -- userB -> 模块B-权限002
(3, 11, 'B'), -- userB -> 模块B-资源001
-- userC 绑定的资产
(4, 13, 'C'), -- userC -> 模块C-设备002
(4, 14, 'C'); -- userC -> 模块C-权限001

-- 为已有角色添加模块编码（如果列已存在则忽略）
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS module_code VARCHAR(10) COMMENT '所属模块编码';

-- 更新现有角色的模块编码
UPDATE sys_role SET module_code = 'A' WHERE module_code IS NULL OR module_code = '';

-- 租户表（模块C专属）
CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '租户ID',
    tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称',
    tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码（唯一）',
    module_code VARCHAR(10) DEFAULT 'C' COMMENT '所属模块（固定为C）',
    status INT DEFAULT 1 COMMENT '状态（1启用 0禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_code (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块C租户表';

-- 初始化模块C租户数据
INSERT INTO sys_tenant (tenant_name, tenant_code, module_code, status) VALUES
('租户1', 'TENANT_001', 'C', 1),
('租户2', 'TENANT_002', 'C', 1),
('租户3', 'TENANT_003', 'C', 1);

-- 为模块C创建共用角色（所有租户共用同一套角色配置）
-- 注意：角色本身不保存tenant_id，tenant_id保存在sys_role_menu和sys_user_role中
INSERT INTO sys_role (role_name, role_code, description, module_code) VALUES
('租户管理员', 'TENANT_ADMIN', '租户超级管理员', 'C'),
('租户操作员', 'TENANT_OPERATOR', '租户操作人员', 'C'),
('租户查看员', 'TENANT_VIEWER', '租户只读人员', 'C');

-- 为sys_user_role表新增模块和租户字段
ALTER TABLE sys_user_role ADD COLUMN IF NOT EXISTS module_code VARCHAR(10) COMMENT '所属模块编码';
ALTER TABLE sys_user_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT(20) DEFAULT NULL COMMENT '关联租户ID（仅模块C）';
ALTER TABLE sys_user_role ADD KEY IF NOT EXISTS idx_module_tenant (module_code, tenant_id);

-- 更新现有用户角色关联的模块编码（根据角色表）
UPDATE sys_user_role ur
INNER JOIN sys_role r ON ur.role_id = r.id
SET ur.module_code = r.module_code, ur.tenant_id = r.tenant_id
WHERE ur.module_code IS NULL OR ur.module_code = '';