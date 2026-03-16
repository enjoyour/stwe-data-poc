package com.toowe.stwe.dto;

import lombok.Data;
import java.util.List;

/**
 * 报关单比对结果
 */
@Data
public class ComparisonResult {
    /**
     * 报关单号
     */
    private String no;

    /**
     * 海关数据检查结果
     */
    private CustomsCheckResult customsCheckResult;

    /**
     * 附件检查结果列表
     */
    private List<AttrCheckResult> attrCheckResult;
}
