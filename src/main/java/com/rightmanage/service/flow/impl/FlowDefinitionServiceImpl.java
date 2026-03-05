package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightmanage.entity.*;
import com.rightmanage.mapper.flow.FlowDefinitionMapper;
import com.rightmanage.mapper.flow.FlowNodePermissionMapper;
import com.rightmanage.service.flow.FlowDefinitionService;
import com.rightmanage.service.SysUserService;
import com.rightmanage.service.SysRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlowDefinitionServiceImpl extends ServiceImpl<FlowDefinitionMapper, FlowDefinition> implements FlowDefinitionService {

    @Autowired
    private FlowNodePermissionMapper flowNodePermissionMapper;
    
    @Autowired
    private SysUserService sysUserService;
    
    @Autowired
    private SysRoleService sysRoleService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<FlowDefinition> list() {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowDefinition>()
                .orderByDesc(FlowDefinition::getCreateTime));
    }

    @Override
    public FlowDefinition getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public FlowDefinitionDetailDTO getDetailById(Long id) {
        FlowDefinition definition = baseMapper.selectById(id);
        if (definition == null) {
            return null;
        }
        
        FlowDefinitionDetailDTO dto = new FlowDefinitionDetailDTO();
        BeanUtils.copyProperties(definition, dto);
        
        // 解析nodesJson为节点列表
        if (StringUtils.hasText(definition.getNodesJson())) {
            try {
                List<FlowNodeDTO> nodes = objectMapper.readValue(definition.getNodesJson(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FlowNodeDTO.class));
                dto.setNodes(nodes);
            } catch (Exception e) {
                // 解析失败则为空
                dto.setNodes(Collections.emptyList());
            }
        }
        
        // 查询发起角色名称
        if (StringUtils.hasText(definition.getStarterRoleIds())) {
            List<String> roleNames = getRoleNames(definition.getStarterRoleIds());
            dto.setStarterRoleNames(roleNames);
        }
        
        return dto;
    }

    @Override
    @Transactional
    public void save(FlowDefinitionDTO dto) {
        FlowDefinition definition = new FlowDefinition();
        BeanUtils.copyProperties(dto, definition);
        definition.setStatus("DRAFT");
        baseMapper.insert(definition);
    }

    @Override
    @Transactional
    public void saveDetail(FlowDefinitionDetailDTO dto) {
        FlowDefinition definition = new FlowDefinition();
        BeanUtils.copyProperties(dto, definition);
        definition.setStatus("DRAFT");
        
        // 将节点列表转为JSON
        if (dto.getNodes() != null && !dto.getNodes().isEmpty()) {
            try {
                definition.setNodesJson(objectMapper.writeValueAsString(dto.getNodes()));
            } catch (Exception e) {
                definition.setNodesJson("[]");
            }
        }
        
        baseMapper.insert(definition);
        
        // 保存节点权限
        saveNodePermissions(definition.getId(), dto.getNodes());
    }

    @Override
    @Transactional
    public void update(FlowDefinitionDTO dto) {
        FlowDefinition definition = new FlowDefinition();
        BeanUtils.copyProperties(dto, definition);
        baseMapper.updateById(definition);
    }

    @Override
    @Transactional
    public void updateDetail(FlowDefinitionDetailDTO dto) {
        FlowDefinition definition = new FlowDefinition();
        BeanUtils.copyProperties(dto, definition);
        
        // 将节点列表转为JSON
        if (dto.getNodes() != null && !dto.getNodes().isEmpty()) {
            try {
                definition.setNodesJson(objectMapper.writeValueAsString(dto.getNodes()));
            } catch (Exception e) {
                definition.setNodesJson("[]");
            }
        }
        
        baseMapper.updateById(definition);
        
        // 更新节点权限
        flowNodePermissionMapper.delete(new LambdaQueryWrapper<FlowNodePermission>()
                .eq(FlowNodePermission::getFlowDefinitionId, dto.getId()));
        saveNodePermissions(dto.getId(), dto.getNodes());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 删除节点权限
        flowNodePermissionMapper.delete(new LambdaQueryWrapper<FlowNodePermission>()
                .eq(FlowNodePermission::getFlowDefinitionId, id));
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        FlowDefinition definition = baseMapper.selectById(id);
        if (definition != null) {
            definition.setStatus("PUBLISHED");
            baseMapper.updateById(definition);
        }
    }

    @Override
    @Transactional
    public void disable(Long id) {
        FlowDefinition definition = baseMapper.selectById(id);
        if (definition != null) {
            definition.setStatus("DISABLED");
            baseMapper.updateById(definition);
        }
    }

    @Override
    public List<FlowDefinition> getStartableFlows(Long userId) {
        // 获取用户角色ID列表
        List<Long> userRoleIds = sysUserService.getRoleIdsByUserId(userId);
        
        if (userRoleIds == null || userRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 查询可发起的流程（角色ID在starter_role_ids中或starter_role_ids为空表示所有人可发起）
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<FlowDefinition>()
                .eq(FlowDefinition::getStatus, "PUBLISHED");
        
        // 查找starter_role_ids包含用户角色ID的流程
        wrapper.and(w -> w.eq(FlowDefinition::getStarterRoleIds, "")
                .or(o -> o.apply("FIND_IN_SET({0}, starter_role_ids)", userRoleIds.get(0))));
        
        return baseMapper.selectList(wrapper);
    }
    
    private void saveNodePermissions(Long definitionId, List<FlowNodeDTO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        for (FlowNodeDTO node : nodes) {
            FlowNodePermission permission = new FlowNodePermission();
            permission.setFlowDefinitionId(definitionId);
            permission.setNodeId(node.getId());
            permission.setNodeName(node.getName());
            permission.setNodeType(node.getType());
            permission.setAssigneeType(node.getAssigneeType());
            permission.setAssigneeIds(node.getAssigneeId());
            permission.setAllowDelegate(1);
            permission.setAllowReject(1);
            permission.setDueHours(0);
            flowNodePermissionMapper.insert(permission);
        }
    }
    
    private List<String> getRoleNames(String roleIds) {
        if (!StringUtils.hasText(roleIds)) {
            return Collections.emptyList();
        }
        
        List<Long> ids = Arrays.stream(roleIds.split(","))
                .map(String::trim)
                .filter(s -> s.length() > 0)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<SysRole> roles = sysRoleService.listByIds(ids);
        return roles.stream()
                .map(SysRole::getRoleName)
                .collect(Collectors.toList());
    }
}
