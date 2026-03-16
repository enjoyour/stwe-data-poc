package com.toowe.stwe.dto;

import cn.hutool.json.JSONObject;
import lombok.Data;

@Data
public class BaoguandanComparisonVO {
    /**
     * 报关单数据 (JSON 字符串形式)
     */
    private String baoguandanData;

    /**
     * 报关单附件解析数据
     */
    private BaoguandanAttachmentVO attachmentData;

    /**
     * 海关数据 (JSON 对象形式，仅包含汇总数据)
     */
    private JSONObject customsData;

    /**
     * 大模型比对结果
     */
    private ComparisonResult comparisonResult;
}
