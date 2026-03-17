package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rightmanage.entity.SysUser;
import com.rightmanage.entity.flow.FlowDefinition;
import com.rightmanage.entity.flow.FlowDefinitionDetailDTO;
import com.rightmanage.entity.flow.FlowNodeConfig;
import com.rightmanage.mapper.flow.FlowDefinitionMapper;
import com.rightmanage.mapper.flow.FlowNodeConfigMapper;
import com.rightmanage.service.flow.FlowDefinitionService;
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

    @Override
    public void saveFlowDefinition(FlowDefinitionDetailDTO dto, Long userId) {
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
                node.setFlowId(definition.getId());
                node.setSort(i + 1);
                flowNodeConfigMapper.insert(node);
            }
        }
    }

    /**
     * 根据连线顺序对节点进行排序（使用拓扑排序）
     */
    private List<FlowNodeConfig> sortNodesByLines(List<FlowNodeConfig> nodes, List<?> lines) {
        if (nodes == null || nodes.isEmpty() || lines == null || lines.isEmpty()) {
            return nodes;
        }

        // 构建节点映射（使用uuid字段）
        Map<String, FlowNodeConfig> nodeMap = new HashMap<>();
        for (FlowNodeConfig node : nodes) {
            String nodeUuid = node.getUuid();
            if (nodeUuid != null) {
                nodeMap.put(nodeUuid, node);
            }
        }

        // 计算每个节点的入度
        Map<String, Integer> inDegree = new HashMap<>();
        for (FlowNodeConfig node : nodes) {
            String nodeUuid = node.getUuid();
            if (nodeUuid != null) {
                inDegree.put(nodeUuid, 0);
            }
        }

        // 构建邻接表（from -> to）并更新入度
        Map<String, List<String>> adjacencyMap = new HashMap<>();
        for (Object lineObj : lines) {
            try {
                Map<String, Object> line = convertToMap(lineObj);
                String fromNode = String.valueOf(line.get("fromNode"));
                String toNode = String.valueOf(line.get("toNode"));
                if (fromNode != null && !fromNode.equals("null") && toNode != null && !toNode.equals("null")) {
                    adjacencyMap.computeIfAbsent(fromNode, k -> new ArrayList<>()).add(toNode);
                    inDegree.put(toNode, inDegree.getOrDefault(toNode, 0) + 1);
                }
            } catch (Exception e) {
                // 忽略转换异常
            }
        }

        // 拓扑排序：使用队列存储入度为0的节点
        Queue<String> queue = new LinkedList<>();
        for (String nodeId : inDegree.keySet()) {
            if (inDegree.get(nodeId) == 0) {
                queue.offer(nodeId);
            }
        }

        List<FlowNodeConfig> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            FlowNodeConfig node = nodeMap.get(nodeId);
            if (node != null) {
                sorted.add(node);
            }

            // 处理所有出边
            List<String> nextNodes = adjacencyMap.get(nodeId);
            if (nextNodes != null) {
                for (String nextId : nextNodes) {
                    int newDegree = inDegree.get(nextId) - 1;
                    inDegree.put(nextId, newDegree);
                    if (newDegree == 0) {
                        queue.offer(nextId);
                    }
                }
            }
        }

        // 如果有环或未遍历到的节点，添加备用节点
        for (FlowNodeConfig node : nodes) {
            String nodeUuid = node.getUuid();
            if (nodeUuid != null && !sorted.contains(node)) {
                sorted.add(node);
            }
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
        FlowDefinition definition = flowDefinitionMapper.selectById(dto.getId());
        if (definition == null) {
            throw new RuntimeException("流程定义不存在");
        }

        BeanUtils.copyProperties(dto, definition, "id", "creatorId", "createTime", "updateTime", "flowCode");
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
                .eq(FlowNodeConfig::getFlowId, dto.getId()));

        // 保存节点配置（按连线顺序）
        if (dto.getNodes() != null && !dto.getNodes().isEmpty() && dto.getLines() != null) {
            List<FlowNodeConfig> sortedNodes = sortNodesByLines(dto.getNodes(), dto.getLines());
            for (int i = 0; i < sortedNodes.size(); i++) {
                FlowNodeConfig node = sortedNodes.get(i);
                node.setFlowId(definition.getId());
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
    public FlowDefinitionDetailDTO getFlowDefinitionDetail(Long id) {
        FlowDefinition definition = flowDefinitionMapper.selectById(id);
        if (definition == null) {
            return null;
        }

        FlowDefinitionDetailDTO dto = new FlowDefinitionDetailDTO();
        BeanUtils.copyProperties(definition, dto);

        // 获取节点配置
        List<FlowNodeConfig> nodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, id)
                        .orderByAsc(FlowNodeConfig::getSort)
        );
        dto.setNodes(nodes);

        return dto;
    }

    @Override
    public void deleteFlowDefinition(Long id) {
        FlowDefinition definition = flowDefinitionMapper.selectById(id);
        if (definition == null) {
            throw new RuntimeException("流程定义不存在");
        }

        // 删除节点配置
        flowNodeConfigMapper.delete(new LambdaQueryWrapper<FlowNodeConfig>()
                .eq(FlowNodeConfig::getFlowId, id));

        // 逻辑删除流程定义
        flowDefinitionMapper.deleteById(id);
    }

    @Override
    public List<FlowDefinition> getStartableFlows(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        List<Long> roleIds = sysUserService.getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询所有启用的流程，然后过滤用户有权限发起的
        List<FlowDefinition> allFlows = baseMapper.selectList(
                new LambdaQueryWrapper<FlowDefinition>()
                        .eq(FlowDefinition::getStatus, 1)
        );

        List<FlowDefinition> result = new ArrayList<>();
        for (FlowDefinition flow : allFlows) {
            if (hasStartRolePermission(flow, user, roleIds)) {
                flow.setNeedTenant(checkFlowNeedTenant(flow.getId()));
                result.add(flow);
            }
        }

        return result;
    }

    /**
     * 校验用户是否有发起权限
     */
    private boolean hasStartRolePermission(FlowDefinition flow, SysUser user, List<Long> userRoleIds) {
        if (flow == null || !StringUtils.hasText(flow.getStartRoleIds())) {
            return false;
        }
        String[] startRoles = flow.getStartRoleIds().split(",");
        for (Long roleId : userRoleIds) {
            for (String startRole : startRoles) {
                if (roleId.toString().equals(startRole.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查流程是否需要租户（判断是否包含产品智能定制模块的角色）
     */
    @Override
    public boolean checkFlowNeedTenant(Long flowId) {
        List<FlowNodeConfig> nodes = flowNodeConfigMapper.selectList(
                new LambdaQueryWrapper<FlowNodeConfig>()
                        .eq(FlowNodeConfig::getFlowId, flowId)
        );

        for (FlowNodeConfig node : nodes) {
            // 如果是审批节点且处理人类型为角色
            if ("approve".equals(node.getNodeType()) && "role".equals(node.getHandlerType())) {
                // 检查是否涉及产品智能定制模块（moduleCode = "C"）
                if ("C".equals(node.getModuleCode())) {
                    return true;
                }
            }
        }
        return false;
    }
}
