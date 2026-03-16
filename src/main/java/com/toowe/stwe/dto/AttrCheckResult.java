package com.toowe.stwe.dto;

import lombok.Data;

/**
 * 附件检查结果
 */
@Data
public class AttrCheckResult {
    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 是否匹配
     */
    private Boolean isMatch;

    /**
     * 检查原因说明
     */
    private String reason;

    /**
     * 附件名称
     */
    private String attrName;
}
