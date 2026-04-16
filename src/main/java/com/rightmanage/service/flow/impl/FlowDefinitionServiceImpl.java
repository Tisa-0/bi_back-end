package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightmanage.entity.SysRole;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.flow.FlowDefinition;
import com.rightmanage.entity.flow.FlowDefinitionDetailDTO;
import com.rightmanage.entity.flow.FlowNodeConfig;
import com.rightmanage.enums.FlowNodeType;
import com.rightmanage.mapper.flow.FlowDefinitionMapper;
import com.rightmanage.mapper.flow.FlowNodeConfigMapper;
import com.rightmanage.service.flow.FlowDefinitionService;
import com.rightmanage.service.SysModuleService;
import com.rightmanage.service.SysRoleService;
import com.rightmanage.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Transactional
public class FlowDefinitionServiceImpl extends ServiceImpl<FlowDefinitionMapper, FlowDefinition> implements FlowDefinitionService {

    @Autowired
    private FlowDefinitionMapper flowDefinitionMapper;
    @Autowired
    private FlowNodeConfigMapper flowNodeConfigMapper;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysRoleService sysRoleService;
    @Autowired
    private SysModuleService sysModuleService;

    @Override
    public void saveFlowDefinition(FlowDefinitionDetailDTO dto, Long userId) {
        // 校验流程编码唯一性
        if (checkFlowCodeExists(dto.getFlowCode(), null)) {
            throw new RuntimeException("流程编码已存在，请使用其他编码");
        }

        FlowDefinition definition = new FlowDefinition();
        definition.setFlowName(dto.getFlowName());
        definition.setFlowCode(dto.getFlowCode());
        definition.setFlowJson(dto.getFlowJson());
        definition.setStartRoleIds(dto.getStartRoleIds());
        definition.setStatus(1);
        definition.setCreatorId(userId);
        definition.setCanInitiate(dto.getCanInitiate() != null ? dto.getCanInitiate() : 1); // 默认允许主动发起
        definition.setNeedAttachment(dto.getNeedAttachment() != null ? dto.getNeedAttachment() : 0); // 默认不需要上传凭证
        flowDefinitionMapper.insert(definition);

        // 保存节点配置（按连线顺序）
        if (dto.getNodes() != null && !dto.getNodes().isEmpty() && dto.getLines() != null) {
            List<FlowNodeConfig> sortedNodes = sortNodesByLines(dto.getNodes(), dto.getLines());
            for (int i = 0; i < sortedNodes.size(); i++) {
                FlowNodeConfig node = sortedNodes.get(i);
                node.setFlowCode(definition.getFlowCode());
                if (!StringUtils.hasText(node.getNodeId())) {
                    node.setNodeId(UUID.randomUUID().toString().replace("-", ""));
                }
                node.setSort(i + 1);
                flowNodeConfigMapper.insert(node);
            }
        }
    }

    /**
     * 根据连线顺序对节点进行排序（使用 Kahn 正向 BFS + 最长路径 + 过滤后重映射层级）
     * 与 buildNodeDetailListWithParallelism 算法保持一致
     */
    private List<FlowNodeConfig> sortNodesByLines(List<FlowNodeConfig> nodes, List<?> lines) {
        if (nodes == null || nodes.isEmpty()) {
            return nodes;
        }
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>(nodes);
        }

        // 获取节点唯一标识
        java.util.function.Function<FlowNodeConfig, String> getNodeId = node -> {
            if (node.getNodeId() != null && !node.getNodeId().isEmpty()) {
                return node.getNodeId();
            }
            return String.valueOf(nodes.indexOf(node));
        };

        // 所有节点
        List<FlowNodeConfig> allNodes = new ArrayList<>(nodes);

        // 【过滤前】保留逻辑节点用于层级计算
        Map<String, FlowNodeConfig> nodeMap = new LinkedHashMap<>();
        for (FlowNodeConfig node : allNodes) {
            nodeMap.put(getNodeId.apply(node), node);
        }

        // 构建邻接表和入度（使用所有节点，保证拓扑完整性）
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adjacency = new LinkedHashMap<>();
        Map<String, Integer> level = new LinkedHashMap<>();
        for (FlowNodeConfig node : allNodes) {
            String id = getNodeId.apply(node);
            inDegree.put(id, 0);
            adjacency.put(id, new ArrayList<>());
            level.put(id, 0);
        }

        for (Object lineObj : lines) {
            try {
                Map<String, Object> line = convertToMap(lineObj);
                String fromNode = String.valueOf(line.get("fromNode"));
                String toNode = String.valueOf(line.get("toNode"));
                if (fromNode != null && !fromNode.equals("null") && toNode != null && !toNode.equals("null")) {
                    if (adjacency.containsKey(fromNode) && inDegree.containsKey(toNode)) {
                        adjacency.get(fromNode).add(toNode);
                        inDegree.put(toNode, inDegree.get(toNode) + 1);
                    }
                }
            } catch (Exception e) {
                // 忽略
            }
        }

        // Kahn 正向 BFS 计算最长路径（level）
        Map<String, Integer> mutableInDegree = new LinkedHashMap<>(inDegree);
        Queue<String> queue = new LinkedList<>();
        for (String id : nodeMap.keySet()) {
            if (mutableInDegree.get(id) == 0) {
                level.put(id, 0);
                queue.add(id);
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLevel = level.get(current);
            for (String next : adjacency.get(current)) {
                level.put(next, Math.max(level.get(next), currentLevel + 1));
                int newInDegree = mutableInDegree.get(next) - 1;
                mutableInDegree.put(next, newInDegree);
                if (newInDegree == 0) {
                    queue.add(next);
                }
            }
        }

        // 【过滤逻辑节点后重映射层级，使层级连续】
        // 构建原始层级到显示层级的映射
        Map<Integer, Integer> originalToDisplayLevel = new LinkedHashMap<>();
        int displayLevelCounter = 0;
        int maxOrigLevel = Collections.max(level.values());
        for (int origLevel = 0; origLevel <= maxOrigLevel; origLevel++) {
            final int lvl = origLevel;
            boolean hasDisplayNode = nodes.stream()
                    .anyMatch(n -> !FlowNodeType.isLogicNode(n.getNodeType())
                            && level.get(getNodeId.apply(n)) == lvl);
            if (hasDisplayNode) {
                originalToDisplayLevel.put(origLevel, displayLevelCounter);
                displayLevelCounter++;
            }
        }

        // 按重映射后的层级分组，同一层级内按原始顺序
        Map<Integer, List<FlowNodeConfig>> levelMap = new TreeMap<>();
        for (FlowNodeConfig node : nodes) {
            String id = getNodeId.apply(node);
            int origLevel = level.get(id);
            int dispLevel = originalToDisplayLevel.getOrDefault(origLevel, origLevel);
            levelMap.computeIfAbsent(dispLevel, k -> new ArrayList<>()).add(node);
        }

        List<FlowNodeConfig> sorted = new ArrayList<>();
        for (List<FlowNodeConfig> levelNodes : levelMap.values()) {
            sorted.addAll(levelNodes);
        }
        return sorted;
    }

    /**
     * 将对象转换为Map（使用Jackson）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(mapper.writeValueAsString(obj), Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Override
    public void updateFlowDefinition(FlowDefinitionDetailDTO dto) {
        FlowDefinition definition = flowDefinitionMapper.selectByFlowCode(dto.getFlowCode());
        if (definition == null) {
            throw new RuntimeException("流程定义不存在");
        }

        // 校验流程编码唯一性（排除自身）
        if (checkFlowCodeExists(dto.getFlowCode(), dto.getFlowCode())) {
            throw new RuntimeException("流程编码已存在，请使用其他编码");
        }

        BeanUtils.copyProperties(dto, definition, "creatorId", "createTime", "updateTime");
        // 手动赋值 flowCode，允许编辑时修改
        definition.setFlowCode(dto.getFlowCode());
        // 确保 canInitiate 字段被更新
        if (dto.getCanInitiate() != null) {
            definition.setCanInitiate(dto.getCanInitiate());
        }
        // 确保 needAttachment 字段被更新
        if (dto.getNeedAttachment() != null) {
            definition.setNeedAttachment(dto.getNeedAttachment());
        }
        flowDefinitionMapper.updateById(definition);

        // 删除原有节点配置，重新保存
        flowNodeConfigMapper.delete(new LambdaQueryWrapper<FlowNodeConfig>()
                .eq(FlowNodeConfig::getFlowCode, definition.getFlowCode()));

        // 保存节点配置（按连线顺序）
        if (dto.getNodes() != null && !dto.getNodes().isEmpty() && dto.getLines() != null) {
            List<FlowNodeConfig> sortedNodes = sortNodesByLines(dto.getNodes(), dto.getLines());
            for (int i = 0; i < sortedNodes.size(); i++) {
                FlowNodeConfig node = sortedNodes.get(i);
                node.setFlowCode(definition.getFlowCode());
                if (!StringUtils.hasText(node.getNodeId())) {
                    node.setNodeId(UUID.randomUUID().toString().replace("-", ""));
                }
                node.setSort(i + 1);
                flowNodeConfigMapper.insert(node);
            }
        }
    }

    @Override
    public List<FlowDefinition> listFlowDefinition() {
        return baseMapper.selectList(new LambdaQueryWrapper<FlowDefinition>()
                .orderByDesc(FlowDefinition::getCreateTime));
    }

    @Override
    public FlowDefinitionDetailDTO getFlowDefinitionDetail(String flowCode) {
        FlowDefinition definition = flowDefinitionMapper.selectByFlowCode(flowCode);
        if (definition == null) {
            return null;
        }

        FlowDefinitionDetailDTO dto = new FlowDefinitionDetailDTO();
        BeanUtils.copyProperties(definition, dto);

        // 获取节点配置
        List<FlowNodeConfig> nodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, definition.getFlowCode())
                        .orderByAsc(FlowNodeConfig::getSort)
        );
        dto.setNodes(nodes);

        return dto;
    }

    @Override
    public void deleteFlowDefinition(String flowCode) {
        FlowDefinition definition = flowDefinitionMapper.selectByFlowCode(flowCode);
        if (definition == null) {
            throw new RuntimeException("流程定义不存在");
        }

        // 删除节点配置
        flowNodeConfigMapper.delete(new LambdaQueryWrapper<FlowNodeConfig>()
                .eq(FlowNodeConfig::getFlowCode, definition.getFlowCode()));

        // 逻辑删除流程定义
        flowDefinitionMapper.deleteById(definition.getFlowCode());
    }

    @Override
    public List<FlowDefinition> getStartableFlows(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        List<String> roleCodes = sysUserService.getRoleIdsByUserId(userId);
        if (roleCodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询所有启用的流程，然后过滤用户有权限发起的
        List<FlowDefinition> allFlows = baseMapper.selectList(
                new LambdaQueryWrapper<FlowDefinition>()
                        .eq(FlowDefinition::getStatus, 1)
        );

        List<FlowDefinition> result = new ArrayList<>();
        for (FlowDefinition flow : allFlows) {
            if (hasStartRolePermission(flow, user, roleCodes)) {
                flow.setNeedTenant(checkFlowNeedTenant(flow.getFlowCode()));
                result.add(flow);
            }
        }

        return result;
    }

    /**
     * 校验用户是否有发起权限
     */
    private boolean hasStartRolePermission(FlowDefinition flow, SysUser user, List<String> userRoleCodes) {
        if (flow == null || !StringUtils.hasText(flow.getStartRoleIds())) {
            return false;
        }
        String[] startRoles = flow.getStartRoleIds().split(",");
        for (String roleCode : userRoleCodes) {
            for (String startRole : startRoles) {
                if (roleCode != null && roleCode.equals(startRole.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查流程是否需要租户（判断是否包含多租户审批节点：handlerType=role 且审批人模块 multiTenant=1 或 角色 org_related=1）
     */
    @Override
    public boolean checkFlowNeedTenant(String flowCode) {
        FlowDefinition definition = flowDefinitionMapper.selectByFlowCode(flowCode);
        if (definition == null || !StringUtils.hasText(definition.getFlowCode())) {
            return false;
        }
        List<FlowNodeConfig> nodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowCode, definition.getFlowCode())
        );

        for (FlowNodeConfig node : nodes) {
            // 如果是审批节点且处理人类型为角色
            if ("approve".equals(node.getNodeType()) && "role".equals(node.getHandlerType())
                    && StringUtils.hasText(node.getModuleCode())) {
                // 检查该模块是否是多租户
                if (sysModuleService.isMultiTenant(node.getModuleCode())) {
                    return true;
                }
                // 检查所选角色是否有 org_related=1
                if (StringUtils.hasText(node.getHandlerIds())) {
                    for (String roleId : node.getHandlerIds().split(",")) {
                        if (StringUtils.hasText(roleId)) {
                            SysRole role = sysRoleService.getById(roleId.trim());
                            if (role != null && Boolean.TRUE.equals(role.getOrgRelated())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查流程编码是否存在
     */
    @Override
    public boolean checkFlowCodeExists(String flowCode, String excludeFlowCode) {
        if (!StringUtils.hasText(flowCode)) {
            return false;
        }
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDefinition::getFlowCode, flowCode);
        if (excludeFlowCode != null) {
            wrapper.ne(FlowDefinition::getFlowCode, excludeFlowCode);
        }
        return baseMapper.selectCount(wrapper) > 0;
    }
}
