package com.toowe.stwe.controller;

import com.toowe.stwe.service.CustomsFileAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/customs-file")
@RequiredArgsConstructor
@Tag(name = "海关文件访问测试", description = "用于测试海关数据文件访问的接口")
public class CustomsFileTestController {

    private final CustomsFileAccessService fileAccessService;

    /**
     * 测试文件访问连接
     */
    @GetMapping("/test-connection")
    @Operation(summary = "测试文件访问连接", description = "测试当前配置的文件访问模式是否正常")
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取文件列表
            List<CustomsFileAccessService.FileResource> files = fileAccessService.listExcelFiles();

            result.put("success", true);
            result.put("fileCount", files.size());
            result.put("message", "文件访问正常");

            // 列出文件信息
            if (!files.isEmpty()) {
                Map<String, Long> fileInfo = new HashMap<>();
                for (CustomsFileAccessService.FileResource file : files) {
                    fileInfo.put(file.getName(), file.getSize());
                }
                result.put("files", fileInfo);
            }

            log.info("文件访问测试成功，找到 {} 个文件", files.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("文件访问测试失败", e);
        }

        return result;
    }

    /**
     * 测试读取第一个Excel文件
     */
    @GetMapping("/test-read-first")
    @Operation(summary = "测试读取第一个Excel文件", description = "尝试读取第一个Excel文件验证数据访问")
    public Map<String, Object> testReadFirstFile() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<CustomsFileAccessService.FileResource> files = fileAccessService.listExcelFiles();

            if (files.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有找到Excel文件");
                return result;
            }

            // 读取第一个文件
            CustomsFileAccessService.FileResource firstFile = files.get(0);
            try (var inputStream = firstFile.getInputStream()) {
                byte[] content = inputStream.readAllBytes();
                result.put("success", true);
                result.put("fileName", firstFile.getName());
                result.put("fileSize", firstFile.getSize());
                result.put("bytesRead", content.length);
                result.put("message", "成功读取文件");
                log.info("成功读取文件: {}, 大小: {} bytes", firstFile.getName(), content.length);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("读取文件失败", e);
        }

        return result;
    }
}