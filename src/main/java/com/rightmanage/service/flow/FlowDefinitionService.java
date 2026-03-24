package com.rightmanage.service.flow;

import com.rightmanage.entity.flow.FlowDefinition;
import com.rightmanage.entity.flow.FlowDefinitionDetailDTO;
import java.util.List;

/**
 * 流程定义服务接口
 */
public interface FlowDefinitionService {
    /**
     * 保存流程定义（含节点配置）
     */
    void saveFlowDefinition(FlowDefinitionDetailDTO dto, Long userId);

    /**
     * 更新流程定义
     */
    void updateFlowDefinition(FlowDefinitionDetailDTO dto);

    /**
     * 获取流程定义列表
     */
    List<FlowDefinition> listFlowDefinition();

    /**
     * 获取流程定义详情
     */
    FlowDefinitionDetailDTO getFlowDefinitionDetail(Long id);

    /**
     * 删除流程定义
     */
    void deleteFlowDefinition(Long id);

    /**
     * 获取当前用户可发起的流程列表
     */
    List<FlowDefinition> getStartableFlows(Long userId);

    /**
     * 检查流程是否需要租户（判断是否包含产品智能定制模块的角色）
     */
    boolean checkFlowNeedTenant(Long flowId);

    /**
     * 检查流程编码是否存在（排除指定ID，可用于编辑时的重复校验）
     * @param flowCode 流程编码
     * @param excludeId 排除的流程ID（编辑时传入，保存时传null）
     * @return true=已存在，false=可用
     */
    boolean checkFlowCodeExists(String flowCode, Long excludeId);
}
