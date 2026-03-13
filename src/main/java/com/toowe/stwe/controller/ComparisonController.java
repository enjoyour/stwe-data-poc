package com.toowe.stwe.controller;

import com.toowe.stwe.dto.BaoguandanComparisonVO;
import com.toowe.stwe.dto.ComparisonRequest;
import com.toowe.stwe.service.ComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comparison")
@RequiredArgsConstructor
@Tag(name = "数据比对管理", description = "报关单多源数据比对接口")
public class ComparisonController {

    private final ComparisonService comparisonService;

    /**
     * 批量对比报关单数据
     * @param request 比对请求参数，包含报关单号列表
     * @return 汇总后的对比结果
     */
    @PostMapping("/compare")
    @Operation(summary = "多源数据汇总对比", description = "并发调用K3Cloud业务接口、K3Cloud附件解析接口、海关Excel接口，并合并数据")
    public List<BaoguandanComparisonVO> compareData(@RequestBody ComparisonRequest request) {
        log.info("收到批量对比请求，单号总数: {}", request.getNumbers() != null ? request.getNumbers().size() : 0);
        
        return comparisonService.compareData(request.getNumbers());
    }
}
