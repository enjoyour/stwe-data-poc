package com.toowe.stwe.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 海关数据结果
 */
@Data
public class CustomsDataResult {
    /**
     * 表头（列名列表）
     */
    private List<String> headers;

    /**
     * 所有匹配的数据行
     */
    private List<Map<String, Object>> rows;

    /**
     * 汇总后的JSON数据（用于LLM比对）
     */
    private String summaryJson;
}
