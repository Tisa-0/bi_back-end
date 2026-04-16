package com.rightmanage.service.flow.impl;

import com.rightmanage.entity.flow.FlowNodeConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowInstanceServiceImplLineSafetyTest {

    @Test
    @SuppressWarnings("unchecked")
    void findNextNodesByLines_ignoresInvalidLinesWithoutThrowingNpe() throws Exception {
        FlowInstanceServiceImpl service = new FlowInstanceServiceImpl();

        FlowNodeConfig currentNode = new FlowNodeConfig();
        currentNode.setNodeKey("start");
        currentNode.setNodeId("start-id");

        FlowNodeConfig targetNode = new FlowNodeConfig();
        targetNode.setNodeKey("approve-1");
        targetNode.setNodeId("approve-1-id");

        FlowInstanceServiceImpl.FlowLine invalidLine = new FlowInstanceServiceImpl.FlowLine();
        invalidLine.setFromNode(null);
        invalidLine.setToNode("approve-1");

        FlowInstanceServiceImpl.FlowLine validLine = new FlowInstanceServiceImpl.FlowLine();
        validLine.setFromNode("start");
        validLine.setToNode("approve-1");

        List<FlowInstanceServiceImpl.FlowLine> lines = new ArrayList<>();
        lines.add(invalidLine);
        lines.add(validLine);

        Map<String, FlowNodeConfig> nodeKeyMap = new HashMap<>();
        nodeKeyMap.put("approve-1", targetNode);
        Map<String, FlowNodeConfig> nodeIdMap = new HashMap<>();
        nodeIdMap.put("approve-1-id", targetNode);

        Method method = FlowInstanceServiceImpl.class.getDeclaredMethod(
                "findNextNodesByLines",
                FlowNodeConfig.class,
                List.class,
                List.class,
                Map.class,
                Map.class
        );
        method.setAccessible(true);

        List<FlowNodeConfig> result = (List<FlowNodeConfig>) method.invoke(
                service,
                currentNode,
                new ArrayList<>(),
                lines,
                nodeKeyMap,
                nodeIdMap
        );

        assertEquals(1, result.size());
        assertEquals("approve-1", result.get(0).getNodeKey());
    }
}
