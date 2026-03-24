package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.SysUserOrgAuth;
import com.rightmanage.entity.SysUserRole;
import com.rightmanage.entity.flow.FlowDefinition;
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
import com.rightmanage.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    public List<Long> getRoleIdsByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId, String moduleCode, Long tenantId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            wrapper.eq(SysUserRole::getModuleCode, moduleCode);
        }
        if (tenantId != null) {
            wrapper.eq(SysUserRole::getTenantId, tenantId);
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        return userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindRoles(Long userId, List<Long> roleIds, String moduleCode, Long tenantId) {
        // 获取新增之前的角色列表
        List<Long> oldRoleIds = getRoleIdsByUserId(userId, moduleCode, tenantId);

        // 删除原有角色（如果指定了moduleCode，则只删除该模块的）
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId);
        if (moduleCode != null && !moduleCode.isEmpty()) {
            deleteWrapper.eq(SysUserRole::getModuleCode, moduleCode);
            if (tenantId != null) {
                deleteWrapper.eq(SysUserRole::getTenantId, tenantId);
            }
        }
        sysUserRoleMapper.delete(deleteWrapper);

        // 添加新角色
        List<Long> newRoleIds = new ArrayList<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setModuleCode(moduleCode);
                userRole.setTenantId(tenantId);
                sysUserRoleMapper.insert(userRole);
                newRoleIds.add(roleId);
            }
        }

        // 找出新增的角色（新增的角色需要分配待办任务）
        Set<Long> addedRoleIds = new HashSet<>(newRoleIds);
        addedRoleIds.removeAll(oldRoleIds);

        // 如果有新角色，为用户分配相关待办任务
        if (!addedRoleIds.isEmpty()) {
            assignPendingTasksForNewRoles(userId, addedRoleIds, moduleCode, tenantId);
        }

        return true;
    }

    /**
     * 当用户获得新角色时，自动分配相关的待办任务
     */
    private void assignPendingTasksForNewRoles(Long userId, Set<Long> newRoleIds, String moduleCode, Long tenantId) {
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
                FlowDefinition flow = flowDefinitionMapper.selectById(instance.getFlowId());
                if (flow == null || !moduleCode.equals(flow.getModuleCode())) {
                    continue;
                }
            }

            // 获取流程的节点配置
            List<FlowNodeConfig> nodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                    .eq(FlowNodeConfig::getFlowId, instance.getFlowId())
                    .orderByAsc(FlowNodeConfig::getSort)
            );

            // 检查每个节点，如果有新角色的处理人节点，且当前用户不在处理人中，则添加待办任务
            for (FlowNodeConfig node : nodes) {
                if (!"approve".equals(node.getNodeType())) continue;
                if (!"role".equals(node.getHandlerType())) continue;
                if (!StringUtils.hasText(node.getHandlerIds())) continue;

                // 检查该节点是否包含新角色
                Long matchedRoleId = null;
                for (Long roleId : newRoleIds) {
                    if (node.getHandlerIds().contains(roleId.toString())) {
                        matchedRoleId = roleId;
                        break;
                    }
                }
                if (matchedRoleId == null) continue;

                // 检查该节点是否需要按租户过滤（模块bi_wx_product）
                Long effectiveTenantId = null;
                if ("bi_wx_product".equals(node.getModuleCode()) && tenantId != null) {
                    effectiveTenantId = tenantId;
                }

                // 检查用户是否真的属于该角色（按租户过滤）
                LambdaQueryWrapper<SysUserRole> userRoleQuery = new LambdaQueryWrapper<>();
                userRoleQuery.eq(SysUserRole::getUserId, userId);
                userRoleQuery.eq(SysUserRole::getRoleId, matchedRoleId);
                if (effectiveTenantId != null) {
                    userRoleQuery.eq(SysUserRole::getTenantId, effectiveTenantId);
                }
                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
                if (userRoles.isEmpty()) continue;

                // 检查是否已存在该节点的待办任务
                LambdaQueryWrapper<FlowTask> existingTaskQuery = new LambdaQueryWrapper<>();
                existingTaskQuery.eq(FlowTask::getInstanceId, instance.getId());
                existingTaskQuery.eq(FlowTask::getNodeKey, node.getNodeKey());
                existingTaskQuery.eq(FlowTask::getHandlerId, userId);
                existingTaskQuery.eq(FlowTask::getStatus, 0); // 待处理
                List<FlowTask> existingTasks = flowTaskMapper.selectList(existingTaskQuery);
                if (!existingTasks.isEmpty()) continue; // 已存在待办任务，跳过

                // 创建新的待办任务
                FlowTask task = new FlowTask();
                task.setInstanceId(instance.getId());
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

                System.out.println("Assigned pending task to user " + user.getUsername() + " for flow instance " + instance.getId());
            }
        }
    }

    @Override
    public BankOrg getAuthorizedOrg(Long userId, String moduleCode, Long tenantId) {
        LambdaQueryWrapper<SysUserOrgAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrgAuth::getUserId, userId)
                .eq(SysUserOrgAuth::getModuleCode, moduleCode);

        if (tenantId == null) {
            wrapper.isNull(SysUserOrgAuth::getTenantId);
        } else {
            wrapper.eq(SysUserOrgAuth::getTenantId, tenantId);
        }

        wrapper.orderByDesc(SysUserOrgAuth::getId).last("limit 1");
        SysUserOrgAuth auth = sysUserOrgAuthMapper.selectOne(wrapper);
        if (auth == null || auth.getOrgId() == null) {
            return null;
        }
        return bankOrgMapper.selectById(auth.getOrgId());
    }

    @Override
    @Transactional
    public boolean bindAuthorizedOrg(Long userId, String moduleCode, Long tenantId, Long orgId) {
        // 每个 user + module + tenant 只保留一条授权机构记录
        LambdaQueryWrapper<SysUserOrgAuth> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserOrgAuth::getUserId, userId)
                .eq(SysUserOrgAuth::getModuleCode, moduleCode);
        if (tenantId == null) {
            deleteWrapper.isNull(SysUserOrgAuth::getTenantId);
        } else {
            deleteWrapper.eq(SysUserOrgAuth::getTenantId, tenantId);
        }
        sysUserOrgAuthMapper.delete(deleteWrapper);

        SysUserOrgAuth auth = new SysUserOrgAuth();
        auth.setUserId(userId);
        auth.setModuleCode(moduleCode);
        auth.setTenantId(tenantId);
        auth.setOrgId(orgId);
        return sysUserOrgAuthMapper.insert(auth) > 0;
    }
}
