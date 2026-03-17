package com.toowe.stwe.controller;

import com.toowe.stwe.dto.BaoguandanComparisonVO;
import com.toowe.stwe.dto.ComparisonRequest;
import com.toowe.stwe.service.ComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comparison")
@RequiredArgsConstructor
@Tag(name = "数据比对管理", description = "报关单多源数据比对接口")
public class ComparisonController {

    private final ComparisonService comparisonService;

    @Value("${app.comparison.server-url}")
    private String serverUrl;

    /**
     * 批量对比报关单数据
     * @param request 比对请求参数，包含报关单号列表
     * @return 下载链接
     */
    @PostMapping("/compare")
    @Operation(summary = "多源数据汇总对比", description = "并发调用K3Cloud业务接口、K3Cloud附件解析接口、海关Excel接口，并合并数据，返回下载链接")
    public String compareData(@RequestBody ComparisonRequest request) {
        log.info("收到批量对比请求，单号总数: {}", request.getNumbers() != null ? request.getNumbers().size() : 0);

        // 执行数据比对并返回下载链接
        String downloadPath = comparisonService.compareData(request.getNumbers());

        // 返回完整的下载URL
        return serverUrl + downloadPath;
    }

    /**
     * 下载比对结果Excel文件
     * @return Excel文件
     */
    @GetMapping("/downloadComparisonResult")
    @Operation(summary = "下载比对结果Excel", description = "下载最新的比对结果Excel文件（包含所有报关单号的比对结果）")
    public ResponseEntity<Resource> downloadComparisonResult() {
        log.info("收到下载请求");

        try {
            // 查找最新的Excel文件
            File excelFile = comparisonService.findLatestComparisonExcelFile();

            // 创建文件资源
            Resource resource = new FileSystemResource(excelFile);

            // 设置响应头
            String filename = excelFile.getName();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(excelFile.length());

            // 对文件名进行URL编码，支持中文文件名
            String encodedFilename = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            headers.setContentDispositionFormData("attachment", encodedFilename);

            log.info("开始下载文件: {}", excelFile.getAbsolutePath());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (RuntimeException e) {
            log.warn("下载失败: {}", e.getMessage());
            // 返回404状态码和错误信息
            return ResponseEntity.status(404).body(null);
        }
    }
}
