package com.toowe.stwe.controller;

import com.toowe.stwe.parser.RemoteFileParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "测试接口", description = "用于测试各种功能的接口")
public class TestController {

    private final RemoteFileParser remoteFileParser;

    /**
     * 测试图片内容提取功能
     */
    @PostMapping(value = "/extract-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "测试图片内容提取", description = "上传图片并使用Moonshot API提取文本内容")
    public ResponseEntity<Map<String, Object>> extractImageContent(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请上传文件");
                return ResponseEntity.badRequest().body(result);
            }

            String fileName = file.getOriginalFilename();
            byte[] fileBytes = file.getBytes();

            log.info("开始测试图片提取功能，文件名: {}, 大小: {} bytes", fileName, fileBytes.length);

            // 判断是否为图片文件
            boolean isImage = remoteFileParser.isImageFile(fileName);
            result.put("isImage", isImage);

            if (!isImage) {
                result.put("success", false);
                result.put("message", "上传的文件不是图片格式");
                result.put("fileName", fileName);
                return ResponseEntity.ok().body(result);
            }

            // 提取图片内容
            long startTime = System.currentTimeMillis();
            String content = remoteFileParser.parseImageWithLLM(fileBytes, fileName);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            result.put("success", true);
            result.put("fileName", fileName);
            result.put("fileSize", fileBytes.length);
            result.put("content", content);
            result.put("contentLength", content != null ? content.length() : 0);
            result.put("duration", duration + " ms");
            result.put("message", "图片内容提取成功");

            log.info("图片内容提取成功，文件名: {}, 耗时: {} ms, 提取文本长度: {}",
                    fileName, duration, content != null ? content.length() : 0);

            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            log.error("图片内容提取失败", e);
            result.put("success", false);
            result.put("message", "提取失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 检查文件是否为图片格式
     */
    @GetMapping("/check-image")
    @Operation(summary = "检查文件是否为图片", description = "根据文件名判断是否为支持的图片格式")
    public ResponseEntity<Map<String, Object>> checkIfImage(@RequestParam("fileName") String fileName) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean isImage = remoteFileParser.isImageFile(fileName);
            result.put("fileName", fileName);
            result.put("isImage", isImage);
            result.put("message", isImage ? "是图片格式" : "不是图片格式");

            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            log.error("检查文件格式失败", e);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "stwe-data-poc");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok().body(result);
    }
}