package com.toowe.stwe.dto;

import lombok.Data;

/**
 * 海关数据检查结果
 */
@Data
public class CustomsCheckResult {
    /**
     * 是否匹配
     */
    private Boolean isMatch;

    /**
     * 检查原因说明
     */
    private String reason;
}
