package com.rightmanage.service.flow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        flowDefinitionMapper.insert(definition);

        // 保存节点配置
        if (dto.getNodes() != null && !dto.getNodes().isEmpty()) {
            for (int i = 0; i < dto.getNodes().size(); i++) {
                FlowNodeConfig node = dto.getNodes().get(i);
                node.setFlowId(definition.getId());
                node.setSort(i + 1);
                flowNodeConfigMapper.insert(node);
            }
        }
    }

    @Override
    public void updateFlowDefinition(FlowDefinitionDetailDTO dto) {
        FlowDefinition definition = flowDefinitionMapper.selectById(dto.getId());
        if (definition == null) {
            throw new RuntimeException("流程定义不存在");
        }

        BeanUtils.copyProperties(dto, definition, "id", "creatorId", "createTime", "updateTime");
        flowDefinitionMapper.updateById(definition);

        // 删除原有节点配置，重新保存
        flowNodeConfigMapper.delete(new LambdaQueryWrapper<FlowNodeConfig>()
                .eq(FlowNodeConfig::getFlowId, dto.getId()));

        if (dto.getNodes() != null && !dto.getNodes().isEmpty()) {
            for (int i = 0; i < dto.getNodes().size(); i++) {
                FlowNodeConfig node = dto.getNodes().get(i);
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

        // 拼接角色ID条件查询
        String roleIdsStr = roleIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return baseMapper.selectList(new LambdaQueryWrapper<FlowDefinition>()
                .like(FlowDefinition::getStartRoleIds, roleIdsStr)
                .eq(FlowDefinition::getStatus, 1));
    }
}
