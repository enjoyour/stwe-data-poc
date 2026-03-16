package com.toowe.stwe.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.toowe.stwe.dto.CustomsDataResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomsDataService {

    @Value("${app.customs-data.base-dir}")
    private String baseDir;

    private static final String DEFAULT_FILE_NAME = "免退税申报_出口报关单管理_20260202.xls";

    /**
     * 获取并解析海关数据
     * @param targetNumber 目标报关单号 (18位)
     * @return 海关数据结果（包含表头、数据行和汇总JSON）
     */
    public CustomsDataResult getCustomsData(String targetNumber) {
        log.info("开始查询海关数据，目标单号: {}, 目录: {}", targetNumber, baseDir);

        // 获取文件夹下所有Excel文件并按名称排序
        File dir = new File(baseDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("海关数据目录不存在: " + dir.getAbsolutePath());
        }

        // 获取所有Excel文件（.xls, .xlsx）
        List<File> excelFiles = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(f -> f.isFile() && (f.getName().toLowerCase().endsWith(".xls") || f.getName().toLowerCase().endsWith(".xlsx")))
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        log.info("找到 {} 个Excel文件", excelFiles.size());

        if (excelFiles.isEmpty()) {
            throw new RuntimeException("海关数据目录中没有Excel文件: " + dir.getAbsolutePath());
        }

        // 存储所有匹配的数据
        List<String> headers = new ArrayList<>();
        List<Map<String, Object>> matchedRows = new ArrayList<>();
        double sumTotalPrice = 0.0;
        double sumQuantity = 0.0;
        boolean found = false;

        // 依次读取每个Excel文件
        for (File excelFile : excelFiles) {
            log.info("正在读取文件: {}", excelFile.getName());
            ExcelReader reader = null;
            try {
                reader = ExcelUtil.getReader(excelFile);
                List<Map<String, Object>> allRows = reader.readAll();

                if (allRows.isEmpty()) {
                    log.warn("文件 {} 没有数据", excelFile.getName());
                    continue;
                }

                // 从第一个文件获取表头
                if (headers.isEmpty()) {
                    headers = new ArrayList<>(allRows.get(0).keySet());
                    log.info("获取表头: {}", headers);
                }

                // 查找匹配的行
                for (Map<String, Object> row : allRows) {
                    Object rawNo = row.get("出口货物报关单号");
                    if (rawNo == null) continue;

                    String fullNo = rawNo.toString();
                    // 逻辑：去掉后三位进行比较
                    if (fullNo.length() > 3) {
                        String matchNo = fullNo.substring(0, fullNo.length() - 3);
                        if (matchNo.equals(targetNumber)) {
                            matchedRows.add(row);
                            found = true;

                            // 汇总金额和数量
                            Object priceObj = row.getOrDefault("成交总价", 0.0);
                            Object qtyObj = row.getOrDefault("成交数量", 0.0);

                            if (priceObj != null) {
                                try {
                                    sumTotalPrice += Double.parseDouble(priceObj.toString());
                                } catch (NumberFormatException e) {
                                    log.warn("无法解析成交总价: {}", priceObj);
                                }
                            }
                            if (qtyObj != null) {
                                try {
                                    sumQuantity += Double.parseDouble(qtyObj.toString());
                                } catch (NumberFormatException e) {
                                    log.warn("无法解析成交数量: {}", qtyObj);
                                }
                            }

                            log.info("找到匹配数据: 文件={}, 报关单号={}, 成交总价={}, 成交数量={}",
                                    excelFile.getName(), fullNo, priceObj, qtyObj);
                        }
                    }
                }

            } catch (Exception e) {
                log.error("读取文件 {} 失败", excelFile.getName(), e);
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }

        if (!found) {
            log.warn("未在所有Excel文件中找到匹配的报关单号: {}", targetNumber);
            return null;
        }

        // 构建汇总JSON
        JSONObject summary = new JSONObject();
        if (!matchedRows.isEmpty()) {
            Map<String, Object> firstRow = matchedRows.get(0);
            summary.set("出口货物报关单号", targetNumber);
            summary.set("出口日期", firstRow.get("出口日期"));
            summary.set("成交方式", firstRow.get("成交方式"));
            summary.set("成交货币", firstRow.get("成交货币"));
            summary.set("成交单位", firstRow.get("成交单位"));
        }
        summary.set("成交总价", sumTotalPrice);
        summary.set("成交数量", sumQuantity);

        // 构建返回结果
        CustomsDataResult result = new CustomsDataResult();
        result.setHeaders(headers);
        result.setRows(matchedRows);
        result.setSummaryJson(summary.toString());

        log.info("数据汇总完成: 匹配行数={}, 成交总价={}, 成交数量={}",
                matchedRows.size(), sumTotalPrice, sumQuantity);

        return result;
    }

    /**
     * 获取海关数据 Excel 文件 (保留原方法供参考)
     */
    public File getCustomsDataFile(String fileName) {
        Path filePath = Paths.get(baseDir, fileName);
        return filePath.toFile();
    }
}
