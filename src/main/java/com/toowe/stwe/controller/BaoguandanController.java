package com.toowe.stwe.controller;

import com.toowe.stwe.service.BaoguandanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/baoguandan")
@RequiredArgsConstructor
@Tag(name = "报关单管理", description = "报关单数据查询接口")
public class BaoguandanController {

    private final BaoguandanService baoguandanService;

    /**
     * 获取报关单数据
     * @param number 单据编号，如 AC09008VT
     * @return 报关单数据
     */
    @GetMapping("/view")
    @Operation(summary = "查询报关单", description = "根据单据编号查询报关单数据")
    public String viewBaoguandan(
            @Parameter(description = "单据编号", example = "AC09008VT", required = true)
            @RequestParam String number) {
        log.info("收到查询报关单请求，Number: {}", number);
        return baoguandanService.getBaoguandan(number);
    }
}
