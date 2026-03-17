package com.toowe.stwe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Schema(description = "数据比对请求参数")
public class ComparisonRequest {

    @Schema(description = "报关单号集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> numbers;
}
