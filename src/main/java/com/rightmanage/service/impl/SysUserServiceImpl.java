package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.SysUserOrgAuth;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.entity.flow.FlowInstance;
import com.rightmanage.entity.flow.FlowNodeConfig;
import com.rightmanage.entity.flow.FlowTask;
import com.rightmanage.mapper.SysUserMapper;
import com.rightmanage.mapper.SysUserOrgAuthMapper;
import com.rightmanage.mapper.SysUserRoleMapper;
import com.rightmanage.mapper.BankOrgMapper;
import com.rightmanage.mapper.flow.FlowDefinitionMapper;
import com.rightmanage.mapper.flow.FlowInstanceMapper;
import com.rightmanage.mapper.flow.FlowNodeConfigMapper;
import com.rightmanage.mapper.flow.FlowTaskMapper;
import com.rightmanage.service.SysModuleService;
import com.rightmanage.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class SysUserServiceImpl implements SysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysUserOrgAuthMapper sysUserOrgAuthMapper;
    @Autowired
    private BankOrgMapper bankOrgMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private FlowDefinitionMapper flowDefinitionMapper;
    @Autowired
    private FlowInstanceMapper flowInstanceMapper;
    @Autowired
    private FlowNodeConfigMapper flowNodeConfigMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private SysModuleService sysModuleService;

    private boolean isMultiTenantModule(String moduleCode) {
        return StringUtils.hasText(moduleCode) && sysModuleService.isMultiTenant(moduleCode);
    }

    private String normalizeTenantCode(String moduleCode, String tenantCode) {
        if (isMultiTenantModule(moduleCode)) {
            return tenantCode != null ? tenantCode : null;
        }
        return "";
    }

    @Override
    public List<SysUser> list() {
        return sysUserMapper.selectList(null);
    }
    @Override
    public IPage<SysUser> page(Integer pageNum, Integer pageSize, String username) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectPage(page, wrapper);
    }
    @Override
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }
    @Override
    public boolean save(SysUser user) {
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return sysUserMapper.insert(user) > 0;
    }
    @Override
    public boolean updateById(SysUser user) {
        // 如果密码有值则加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return sysUserMapper.updateById(user) > 0;
    }
    @Override
    public boolean deleteById(Long id) {
        return sysUserMapper.deleteById(id) > 0;
    }
    @Override
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id)
               .set(SysUser::getStatus, status);
        return sysUserMapper.update(null, wrapper) > 0;
    }
    @Override
    public List<String> getRoleIdsByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getRoleCode).collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleIdsByUserId(Long userId, String moduleCode, String tenantCode) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysUserRole::getModuleCode, moduleCode);
            if (isMultiTenantModule(moduleCode)) {
                if (tenantCode != null && !tenantCode.trim().isEmpty()) {
                    wrapper.eq(SysUserRole::getTenantCode, tenantCode);
                }
            } else {
                wrapper.eq(SysUserRole::getTenantCode, "");
            }
        }
        if ((moduleCode == null || moduleCode.isEmpty()) && tenantCode != null && !tenantCode.trim().isEmpty()) {
            wrapper.eq(SysUserRole::getTenantCode, tenantCode);
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getRoleCode).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindRoles(Long userId, List<String> roleCodes, String moduleCode, String tenantCode) {
        // 获取新增之前的角色列表
        List<String> oldRoleCodes = getRoleIdsByUserId(userId, moduleCode, tenantCode);

        // 删除原有角色（如果指定了moduleCode，则只删除该模块的）
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysUserRole::getModuleCode, moduleCode);
            if (isMultiTenantModule(moduleCode)) {
                if (tenantCode != null && !tenantCode.trim().isEmpty()) {
                    deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
                }
            } else {
                deleteWrapper.eq(SysUserRole::getTenantCode, "");
            }
        } else if (tenantCode != null && !tenantCode.trim().isEmpty()) {
                deleteWrapper.eq(SysUserRole::getTenantCode, tenantCode);
        }
        sysUserRoleMapper.delete(deleteWrapper);

        // 添加新角色
        List<String> newRoleCodes = new ArrayList<>();
        if (roleCodes != null && !roleCodes.isEmpty()) {
            for (String roleCode : roleCodes) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleCode(roleCode);
                userRole.setModuleCode(moduleCode);
                userRole.setTenantCode(normalizeTenantCode(moduleCode, tenantCode));
                sysUserRoleMapper.insert(userRole);
                newRoleCodes.add(roleCode);
            }
        }

        // 找出新增的角色（新增的角色需要分配待办任务）
        Set<String> addedRoleCodes = new HashSet<>(newRoleCodes);
        addedRoleCodes.removeAll(oldRoleCodes);

        // 如果有新角色，为用户分配相关待办任务
        if (!addedRoleCodes.isEmpty()) {
            assignPendingTasksForNewRoles(userId, addedRoleCodes, moduleCode, tenantCode);
        }

        return true;
    }

    /**
     * 当用户获得新角色时，自动分配相关的待办任务
     */
    private void assignPendingTasksForNewRoles(Long userId, Set<String> newRoleCodes, String moduleCode, String tenantCode) {
        // 获取用户信息
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) return;

        // 查询所有运行中的流程实例
        List<FlowInstance> runningInstances = flowInstanceMapper.selectList(
            new LambdaQueryWrapper<FlowInstance>()
                .eq(FlowInstance::getStatus, 1) // 运行中
        );

        for (FlowInstance instance : runningInstances) {
            // 如果指定了模块，需要检查流程是否属于该模块
            if (StringUtils.hasText(moduleCode)) {
                if (!moduleCode.equals(instance.getModuleCode())) {
                    continue;
                }
            }

            // 获取流程的节点配置
            List<FlowNodeConfig> nodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                    .eq(FlowNodeConfig::getFlowCode, instance.getFlowCode())
                    .orderByAsc(FlowNodeConfig::getSort)
            );

            // 检查每个节点，如果有新角色的处理人节点，且当前用户不在处理人中，则添加待办任务
            for (FlowNodeConfig node : nodes) {
                if (!"approve".equals(node.getNodeType())) continue;
                if (!"role".equals(node.getHandlerType())) continue;
                if (!StringUtils.hasText(node.getHandlerIds())) continue;

                // 检查该节点是否包含新角色
                String matchedRoleCode = null;
                List<String> handlerRoleCodes = Arrays.stream(node.getHandlerIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
                for (String roleCode : newRoleCodes) {
                    if (handlerRoleCodes.contains(roleCode)) {
                        matchedRoleCode = roleCode;
                        break;
                    }
                }
                if (matchedRoleCode == null) continue;

                // 检查该节点是否需要按租户过滤（模块bi_wx_product）
                String effectiveTenantCode = null;
                if ("bi_wx_product".equals(node.getModuleCode()) && tenantCode != null && !tenantCode.trim().isEmpty()) {
                    effectiveTenantCode = tenantCode;
                }

                // 检查用户是否真的属于该角色（按租户过滤）
                LambdaQueryWrapper<SysUserRole> userRoleQuery = new LambdaQueryWrapper<>();
                userRoleQuery.eq(SysUserRole::getUserId, userId);
                userRoleQuery.eq(SysUserRole::getRoleCode, matchedRoleCode);
                if (effectiveTenantCode != null) {
                    userRoleQuery.eq(SysUserRole::getTenantCode, effectiveTenantCode);
                }
                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
                if (userRoles.isEmpty()) continue;

                // 检查是否已存在该节点的待办任务
                LambdaQueryWrapper<FlowTask> existingTaskQuery = new LambdaQueryWrapper<>();
                existingTaskQuery.eq(FlowTask::getInstanceId, instance.getInstanceId());
                existingTaskQuery.eq(FlowTask::getNodeKey, node.getNodeKey());
                existingTaskQuery.eq(FlowTask::getHandlerId, userId);
                existingTaskQuery.eq(FlowTask::getStatus, 0); // 待处理
                List<FlowTask> existingTasks = flowTaskMapper.selectList(existingTaskQuery);
                if (!existingTasks.isEmpty()) continue; // 已存在待办任务，跳过

                // 创建新的待办任务
                FlowTask task = new FlowTask();
                task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
                task.setInstanceId(instance.getInstanceId());
                task.setNodeKey(node.getNodeKey());
                task.setNodeName(node.getNodeName());
                task.setNodeType(node.getNodeType());
                task.setHandlerId(userId);
                task.setHandlerName(user.getUsername());
                task.setStatus(0); // 待处理
                flowTaskMapper.insert(task);

                // 更新流程实例的当前节点信息
                instance.setCurrentNodeKey(node.getNodeKey());
                instance.setCurrentNodeName(node.getNodeName());
                flowInstanceMapper.updateById(instance);

                System.out.println("Assigned pending task to user " + user.getUsername() + " for flow instance " + instance.getInstanceId());
            }
        }
    }

    @Override
    public BankOrg getAuthorizedOrg(Long userId, String moduleCode, String tenantCode) {
        String normalizedTenantCode = normalizeTenantCode(moduleCode, tenantCode);
        LambdaQueryWrapper<SysUserOrgAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrgAuth::getUserId, userId)
                .eq(SysUserOrgAuth::getModuleCode, moduleCode);

        if (normalizedTenantCode == null) {
            wrapper.isNull(SysUserOrgAuth::getTenantCode);
        } else {
            wrapper.eq(SysUserOrgAuth::getTenantCode, normalizedTenantCode);
        }
        wrapper.last("limit 1");
        SysUserOrgAuth auth = sysUserOrgAuthMapper.selectOne(wrapper);
        if (auth == null || auth.getOrgId() == null) {
            return null;
        }
        return bankOrgMapper.selectById(auth.getOrgId());
    }

    @Override
    @Transactional
    public boolean bindAuthorizedOrg(Long userId, String moduleCode, String tenantCode, String orgId) {
        String normalizedTenantCode = normalizeTenantCode(moduleCode, tenantCode);
        LambdaQueryWrapper<SysUserOrgAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserOrgAuth::getUserId, userId)
                .eq(SysUserOrgAuth::getModuleCode, moduleCode);
        if (normalizedTenantCode == null) {
            queryWrapper.isNull(SysUserOrgAuth::getTenantCode);
        } else {
            queryWrapper.eq(SysUserOrgAuth::getTenantCode, normalizedTenantCode);
        }

        SysUserOrgAuth existingAuth = sysUserOrgAuthMapper.selectOne(queryWrapper);
        if (existingAuth != null) {
            existingAuth.setOrgId(orgId);

            LambdaUpdateWrapper<SysUserOrgAuth> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysUserOrgAuth::getUserId, userId)
                    .eq(SysUserOrgAuth::getModuleCode, moduleCode);
            if (normalizedTenantCode == null) {
                updateWrapper.isNull(SysUserOrgAuth::getTenantCode);
            } else {
                updateWrapper.eq(SysUserOrgAuth::getTenantCode, normalizedTenantCode);
            }
            return sysUserOrgAuthMapper.update(existingAuth, updateWrapper) > 0;
        }

        SysUserOrgAuth auth = new SysUserOrgAuth();
        auth.setUserId(userId);
        auth.setModuleCode(moduleCode);
        auth.setTenantCode(normalizedTenantCode);
        auth.setOrgId(orgId);
        return sysUserOrgAuthMapper.insert(auth) > 0;
    }

    @Override
    public List<String> getAncestorOrgIds(String orgId) {
        if (!StringUtils.hasText(orgId)) {
            return Collections.emptyList();
        }
        List<BankOrg> all = bankOrgMapper.selectActiveAll(today());
        Map<String, BankOrg> byId = new LinkedHashMap<>();
        for (BankOrg org : all) {
            byId.put(org.getId(), org);
        }
        List<String> ancestors = new ArrayList<>();
        String current = orgId;
        while (current != null) {
            ancestors.add(current);
            BankOrg currentOrg = byId.get(current);
            current = (currentOrg != null) ? currentOrg.getParentId() : null;
        }
        return ancestors;
    }

    @Override
    public List<String> getDescendantOrgIds(String orgId) {
        if (!StringUtils.hasText(orgId)) {
            return Collections.emptyList();
        }
        List<BankOrg> all = bankOrgMapper.selectActiveAll(today());
        Map<String, List<BankOrg>> childrenMap = new LinkedHashMap<>();
        for (BankOrg org : all) {
            childrenMap.computeIfAbsent(org.getParentId(), k -> new ArrayList<>()).add(org);
        }
        List<String> descendants = new ArrayList<>();
        descendants.add(orgId);
        collectDescendants(orgId, childrenMap, descendants);
        return descendants;
    }

    private void collectDescendants(String parentId, Map<String, List<BankOrg>> childrenMap, List<String> result) {
        List<BankOrg> children = childrenMap.get(parentId);
        if (children != null) {
            for (BankOrg child : children) {
                result.add(child.getId());
                collectDescendants(child.getId(), childrenMap, result);
            }
        }
    }

    /**
     * 判断用户在指定模块/租户下，是否有权限审批机构相关任务
     * <p>核心逻辑（可扩展）：
     * 从 sourceOrgId 出发，依次向上遍历（通过 parentId），直到 parentId = 0（顶层根机构）。
     * 在此路径上的所有机构ID（包括 sourceOrgId 自身）构成「可审批机构集」。
     * 若用户的授权机构（userOrgId）在此集合中，则有权审批。
     *
     * @param userId      用户ID
     * @param moduleCode  模块编码
     * @param tenantCode  租户编码（可为null，表示无租户限制）
     * @param sourceOrgId 发起机构ID（流程发起时选择的起始机构）
     * @return true=有权审批，false=无权
     */
    @Override
    public boolean isUserAuthorizedForOrgLevel(Long userId, String moduleCode, String tenantCode, String sourceOrgId) {
        // 1. 获取用户在该模块/租户下的授权机构
        BankOrg userOrg = getAuthorizedOrg(userId, moduleCode, tenantCode);
        if (userOrg == null || userOrg.getId() == null) {
            // 用户无授权机构，无权审批机构相关节点
            return false;
        }

        // 2. sourceOrgId 为空时，默认有权（不需要机构层级判断）
        if (!StringUtils.hasText(sourceOrgId)) {
            return true;
        }

        // 3. 优先从机构层级扁平化表查询 sourceOrgId -> 根路径
        List<String> orgPathToRoot = getOrgPathToRootByFlattenTable(sourceOrgId);

        // 4. 兜底：扁平化表无数据时，按父级链路回溯
        if (orgPathToRoot.isEmpty()) {
            List<BankOrg> allOrgs = bankOrgMapper.selectActiveAll(today());
            Map<String, BankOrg> orgById = new LinkedHashMap<>();
            for (BankOrg org : allOrgs) {
                orgById.put(org.getId(), org);
            }
            String current = sourceOrgId;
            while (StringUtils.hasText(current)) {
                orgPathToRoot.add(current);
                BankOrg currentOrg = orgById.get(current);
                current = (currentOrg != null) ? currentOrg.getParentId() : null;
            }
        }

        // 5. 判断用户授权机构是否在路径上
        return orgPathToRoot.contains(userOrg.getId());
    }

    private List<String> getOrgPathToRootByFlattenTable(String sourceOrgId) {
        String dte = yesterday();
        String yyyymm = yesterdayMonth();
        String tableName = "bmip_000_orglevcmp_" + yyyymm;
        if (!tableName.matches("^bmip_000_orglevcmp_\\d{6}$")) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> row = bankOrgMapper.selectOrgFlattenRow(tableName, dte, sourceOrgId);
            if (row == null || row.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> path = new ArrayList<>();
            for (int index = 0; index <= 7; index++) {
                Object value = row.get("orglev" + index);
                String orgCode = value == null ? null : String.valueOf(value).trim();
                if (StringUtils.hasText(orgCode)) {
                    path.add(orgCode);
                }
            }
            return path;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String yesterday() {
        return LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String yesterdayMonth() {
        return LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
