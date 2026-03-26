package com.rightmanage.entity.flow;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.util.List;

/**
 * 流程统一查询结果
 */
@Data
public class FlowQueryResultVO<T> {

    /**
     * 查询类型：pending | myApproval | myInitiated
     */
    private String queryType;

    /**
     * 分页数据
     */
    private IPage<T> page;

    /**
     * 便捷访问：总记录数
     */
    private Long total;

    /**
     * 便捷访问：当前页码
     */
    private Long pageNum;

    /**
     * 便捷访问：每页条数
     */
    private Long pageSize;

    /**
     * 便捷访问：总页数
     */
    private Long totalPages;

    /**
     * 便捷访问：数据列表
     */
    private List<T> records;
}
