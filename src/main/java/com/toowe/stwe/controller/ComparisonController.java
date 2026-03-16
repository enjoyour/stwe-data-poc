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

        // 执行数据比对
        List<BaoguandanComparisonVO> comparisonResults = comparisonService.compareData(request.getNumbers());

        // 如果只有一个报关单号，直接返回下载链接
        if (comparisonResults.size() == 1) {
            BaoguandanComparisonVO result = comparisonResults.get(0);
            String declarationNo = extractDeclarationNo(result);
            return buildDownloadUrl(declarationNo);
        }

        // 如果有多个报关单号，返回第一个的下载链接（或者可以根据需求修改逻辑）
        BaoguandanComparisonVO result = comparisonResults.get(0);
        String declarationNo = extractDeclarationNo(result);
        return buildDownloadUrl(declarationNo);
    }

    /**
     * 从比对结果中提取报关单号
     */
    private String extractDeclarationNo(BaoguandanComparisonVO result) {
        if (result.getComparisonResult() != null && result.getComparisonResult().getNo() != null) {
            return result.getComparisonResult().getNo();
        }
        // 如果没有从比对结果中获取到，尝试从其他字段获取
        return "unknown";
    }

    /**
     * 构建下载链接
     */
    private String buildDownloadUrl(String declarationNo) {
        return String.format("%s/api/comparison/downloadComparisonResult?declarationNo=%s",
                serverUrl, declarationNo);
    }

    /**
     * 下载比对结果Excel文件
     * @param declarationNo 报关单号
     * @return Excel文件
     */
    @GetMapping("/downloadComparisonResult")
    @Operation(summary = "下载比对结果Excel", description = "根据报关单号下载比对结果Excel文件")
    public ResponseEntity<Resource> downloadComparisonResult(
            @RequestParam("declarationNo") String declarationNo) {
        log.info("收到下载请求，报关单号: {}", declarationNo);

        try {
            // 查找Excel文件
            File excelFile = comparisonService.findComparisonExcelFile(declarationNo);

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
