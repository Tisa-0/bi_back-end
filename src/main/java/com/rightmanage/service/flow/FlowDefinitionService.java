package com.rightmanage.service.flow;

import com.rightmanage.entity.FlowDefinition;
import com.rightmanage.entity.FlowDefinitionDTO;
import com.rightmanage.entity.FlowDefinitionDetailDTO;
import java.util.List;

public interface FlowDefinitionService {
    // 流程定义CRUD
    List<FlowDefinition> list();
    
    FlowDefinition getById(Long id);
    
    FlowDefinitionDetailDTO getDetailById(Long id);
    
    void save(FlowDefinitionDTO dto);
    
    void saveDetail(FlowDefinitionDetailDTO dto);
    
    void update(FlowDefinitionDTO dto);
    
    void updateDetail(FlowDefinitionDetailDTO dto);
    
    void delete(Long id);
    
    void publish(Long id);
    
    void disable(Long id);
    
    // 获取当前用户可发起的流程列表
    List<FlowDefinition> getStartableFlows(Long userId);
}
