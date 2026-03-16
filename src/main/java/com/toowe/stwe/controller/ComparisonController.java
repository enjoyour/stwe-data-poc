package com.toowe.stwe.controller;

import com.toowe.stwe.dto.BaoguandanComparisonVO;
import com.toowe.stwe.dto.ComparisonRequest;
import com.toowe.stwe.service.ComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
