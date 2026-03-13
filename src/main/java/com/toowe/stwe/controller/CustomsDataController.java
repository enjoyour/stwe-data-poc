package com.toowe.stwe.controller;

import cn.hutool.json.JSONObject;
import com.toowe.stwe.service.CustomsDataService;
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
@RequestMapping("/api/customs")
@RequiredArgsConstructor
@Tag(name = "海关数据管理", description = "获取共享目录下的海关 Excel 数据")
public class CustomsDataController {

    private final CustomsDataService customsDataService;

    /**
     * 获取汇总后的海关数据
     * @param number 报关单号
     * @return 汇总后的海关数据
     */
    @GetMapping("/view")
    @Operation(summary = "根据单号查询海关数据", description = "读取指定 Excel 文件并汇总匹配单号（前18位）的数据")
    public JSONObject viewCustomsData(
            @Parameter(description = "报关单号 (如: 310120210519750857)", example = "310120210519750857", required = true)
            @RequestParam String number) {
        log.info("收到根据单号获取海关数据请求，Number: {}", number);
        JSONObject result = customsDataService.getCustomsData(number);
        if (result == null) {
            throw new RuntimeException("在海关数据文件中未找到匹配单号: " + number);
        }
        
        return result;
    }
}
