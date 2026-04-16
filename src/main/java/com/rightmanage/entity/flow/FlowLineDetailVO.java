package com.rightmanage.entity.flow;

import lombok.Data;

@Data
public class FlowLineDetailVO {
    private String fromNodeKey;
    private String fromNodeName;
    private String toNodeKey;
    private String toNodeName;
}
