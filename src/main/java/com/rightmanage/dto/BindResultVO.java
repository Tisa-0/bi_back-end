package com.rightmanage.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class BindResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer successCount;
    private Integer failCount;
    private List<Long> failUserIds;
    private String message;

    public static BindResultVO success(Integer successCount, String message) {
        BindResultVO result = new BindResultVO();
        result.setSuccessCount(successCount);
        result.setFailCount(0);
        result.setMessage(message);
        return result;
    }

    public static BindResultVO partial(Integer successCount, Integer failCount, List<Long> failUserIds, String message) {
        BindResultVO result = new BindResultVO();
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setFailUserIds(failUserIds);
        result.setMessage(message);
        return result;
    }
}
