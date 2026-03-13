package com.toowe.stwe.service;

import cn.hutool.json.JSONObject;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CustomsDataService {

    @Value("${app.customs-data.base-dir}")
    private String baseDir;

    private static final String DEFAULT_FILE_NAME = "免退税申报_出口报关单管理_20260202.xls";

    /**
     * 获取并解析海关数据
     * @param targetNumber 目标报关单号 (18位)
     * @return 汇总后的JSON数据
     */
    public JSONObject getCustomsData(String targetNumber) {
        log.info("开始查询海关数据，目标单号: {}, 目录: {}", targetNumber, baseDir);

        Path filePath = Paths.get(baseDir, DEFAULT_FILE_NAME);
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new RuntimeException("海关数据文件不存在: " + file.getAbsolutePath());
        }

        ExcelReader reader = ExcelUtil.getReader(file);
        try {
            // 读取所有数据，第一行为表头
            List<Map<String, Object>> readAll = reader.readAll();
            log.info("Excel读取完成，共 {} 行数据", readAll.size());

            JSONObject result = new JSONObject();
            double sumTotalPrice = 0.0;
            double sumQuantity = 0.0;
            boolean found = false;

            for (Map<String, Object> row : readAll) {
                Object rawNo = row.get("出口货物报关单号");
                if (rawNo == null) continue;

                String fullNo = rawNo.toString();
                // 逻辑：去掉后三位进行比较
                if (fullNo.length() > 3) {
                    String matchNo = fullNo.substring(0, fullNo.length() - 3);
                    if (matchNo.equals(targetNumber)) {
                        if (!found) {
                            // 第一条匹配到的数据，初始化基本字段
                            result.set("出口货物报关单号", matchNo);
                            result.set("出口日期", row.get("出口日期"));
                            result.set("成交方式", row.get("成交方式"));
                            result.set("成交货币", row.get("成交货币"));
                            result.set("成交单位", row.get("成交单位"));
                            found = true;
                        }

                        // 汇总金额和数量
                        sumTotalPrice += Double.parseDouble(row.getOrDefault("成交总价", 0.0).toString());
                        sumQuantity += Double.parseDouble(row.getOrDefault("成交数量", 0.0).toString());
                    }
                }
            }

            if (!found) {
                log.warn("未在Excel中找到匹配的报关单号: {}", targetNumber);
                return null;
            }

            result.set("成交总价", sumTotalPrice);
            result.set("成交数量", sumQuantity);

            log.info("数据汇总完成: {}", result);
            return result;

        } catch (Exception e) {
            log.error("解析海关数据Excel失败", e);
            throw new RuntimeException("解析海关数据失败: " + e.getMessage());
        } finally {
            reader.close();
        }
    }

    /**
     * 获取海关数据 Excel 文件 (保留原方法供参考)
     */
    public File getCustomsDataFile(String fileName) {
        Path filePath = Paths.get(baseDir, fileName);
        return filePath.toFile();
    }
}
