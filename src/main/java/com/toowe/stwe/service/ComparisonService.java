package com.toowe.stwe.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.toowe.stwe.dto.BaoguandanAttachmentVO;
import com.toowe.stwe.dto.BaoguandanComparisonVO;
import com.toowe.stwe.dto.ComparisonResult;
import com.toowe.stwe.dto.CustomsDataResult;
import com.toowe.stwe.util.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final BaoguandanService baoguandanService;
    private final CustomsDataService customsDataService;
    private final LlmClient llmClient;

    @Value("${app.comparison.prompt-file}")
    private String promptFilePath;

    @Value("${app.comparison.output-dir}")
    private String outputDir;

    /**
     * 批量比对报关单数据
     */
    public List<BaoguandanComparisonVO> compareData(List<String> numbers) {
        log.info("开始进行数据比对，单号总数: {}", numbers.size());
        List<BaoguandanComparisonVO> resultList = new ArrayList<>();

        // 提前解析提示词文件
        String systemPrompt = "";
        String userPromptTemplate = "";
        try {
            String fullPrompt = FileUtil.readUtf8String(new File(promptFilePath));
            systemPrompt = StrUtil.subBetween(fullPrompt, "### SYSTEM_PROMPT", "### USER_PROMPT").trim();
            userPromptTemplate = StrUtil.subAfter(fullPrompt, "### USER_PROMPT", true).trim();
            log.info("提示词文件解析成功，系统提示词长度: {}, 用户提示词模板长度: {}", systemPrompt.length(), userPromptTemplate.length());
        } catch (Exception e) {
            log.error("解析提示词文件失败: {}", promptFilePath, e);
            throw new RuntimeException("读取提示词文件失败: " + e.getMessage());
        }

        for (String number : numbers) {
            log.info("正在并发获取数据: {}", number);

            // 并发获取三方数据
            CompletableFuture<String> baoguandanTask = CompletableFuture.supplyAsync(() -> baoguandanService.getBaoguandan(number));
            CompletableFuture<BaoguandanAttachmentVO> attachmentTask = CompletableFuture.supplyAsync(() -> baoguandanService.getBaoguandanAttachments(number));
            CompletableFuture<CustomsDataResult> customsTask = CompletableFuture.supplyAsync(() -> customsDataService.getCustomsData(number));

            try {
                CompletableFuture.allOf(baoguandanTask, attachmentTask, customsTask).join();

                String bData = baoguandanTask.get();
                BaoguandanAttachmentVO aData = attachmentTask.get();
                CustomsDataResult cData = customsTask.get();

                // 组装 VO
                BaoguandanComparisonVO vo = new BaoguandanComparisonVO();
                vo.setBaoguandanData(bData);
                vo.setAttachmentData(aData);
                vo.setCustomsData(cData != null ? JSONUtil.parseObj(cData.getSummaryJson()) : null);

                // 填充用户提示词模板中的变量
                String finalUserPrompt = userPromptTemplate
                        .replace("{baoguandanData}", bData != null ? bData : "无数据")
                        .replace("{attachmentData}", aData != null ? JSONUtil.toJsonStr(aData.getAttachments()) : "无数据")
                        .replace("{customsData}", cData != null ? cData.getSummaryJson() : "无数据");

                // 调用 LLM 进行比对分析
                log.info("正在调用 LLM 进行数据比对: {}", number);
                log.info("系统提示词: {}", systemPrompt);
                log.info("用户提示词: {}", finalUserPrompt);
                String comparisonResultJson = llmClient.callLLM(systemPrompt, finalUserPrompt);
                log.info("LLM 返回的 JSON 结果: {}", comparisonResultJson);

                // 将 JSON 字符串解析为 ComparisonResult 对象
                ComparisonResult comparisonResult = JSONUtil.toBean(comparisonResultJson, ComparisonResult.class);
                vo.setComparisonResult(comparisonResult);

                resultList.add(vo);
                log.info("单号 {} 的数据比对分析完成，解析结果: no={}, 海关检查结果={}, 附件检查项数={}",
                        number,
                        comparisonResult.getNo(),
                        comparisonResult.getCustomsCheckResult().getIsMatch(),
                        comparisonResult.getAttrCheckResult().size());

                // 导出Excel文件
                exportComparisonExcel(number, cData, bData, aData, comparisonResult);

            } catch (Exception e) {
                log.error("处理单号 {} 时发生错误: {}", number, e.getMessage());
                throw new RuntimeException("处理单号 " + number + " 时发生错误: " + e.getMessage());
            }
        }

        return resultList;
    }

    /**
     * 导出比对结果到Excel文件
     */
    private void exportComparisonExcel(String number, CustomsDataResult customsData,
                                       String baoguandanData, BaoguandanAttachmentVO attachmentData,
                                       ComparisonResult comparisonResult) {
        // 文件名只使用报关单号，不使用时间戳
        String fileName = String.format("数据比对_%s.xlsx", number);
        File outputFile = new File(outputDir, fileName);

        // 如果文件已存在，先删除
        if (outputFile.exists()) {
            boolean deleted = outputFile.delete();
            log.info("删除已存在的文件: {}, 删除结果: {}", outputFile.getAbsolutePath(), deleted);
        }

        // 确保输出目录存在
        FileUtil.mkdir(outputFile.getParentFile());

        ExcelWriter writer = null;
        try {
            writer = ExcelUtil.getWriter(outputFile);

            // 第一个sheet：海关数据
            writer.renameSheet("海关数据");
            if (customsData != null && customsData.getHeaders() != null && !customsData.getHeaders().isEmpty()) {
                // 写入表头
                List<String> headers = customsData.getHeaders();
                writer.writeRow(headers);

                // 写入数据行
                if (customsData.getRows() != null && !customsData.getRows().isEmpty()) {
                    for (Map<String, Object> row : customsData.getRows()) {
                        List<Object> rowData = new ArrayList<>();
                        for (String header : headers) {
                            rowData.add(row.get(header));
                        }
                        writer.writeRow(rowData);
                    }
                }
                log.info("海关数据写入完成: 表头数={}, 数据行数={}", headers.size(),
                        customsData.getRows() != null ? customsData.getRows().size() : 0);
            } else {
                log.warn("海关数据为空，仅写入表头");
            }

            // 第二个sheet：报关单数据
            writer.setSheet("报关单数据");
            if (baoguandanData != null) {
                // 将JSON字符串转换为Map，写入Excel
                JSONObject baoguandanJson = JSONUtil.parseObj(baoguandanData);
                List<String> bgdHeaders = new ArrayList<>(baoguandanJson.keySet());
                List<Object> bgdData = new ArrayList<>(baoguandanJson.values());

                writer.writeRow(bgdHeaders);
                writer.writeRow(bgdData);
                log.info("报关单数据写入完成: 字段数={}", bgdHeaders.size());
            } else {
                // 如果没有数据，只写表头
                List<String> defaultHeaders = List.of("报关单号", "合同协议号", "实际开船日期", "运输条款",
                        "结算币别", "总金额", "数量");
                writer.writeRow(defaultHeaders);
                log.warn("报关单数据为空，仅写入默认表头");
            }

            // 第三个sheet：比对结果
            writer.setSheet("比对结果");

            // 第一行：5个大标题
            // 报关单数据(0-6列，7列) | 海关数据(7-13列，7列) | 海关核对结果(14列) | 附件核对结果(15列) | 附件内容(16列)
            List<Object> row1Data = new ArrayList<>();
            row1Data.add("报关单数据");  // 0
            row1Data.addAll(Collections.nCopies(6, ""));  // 1-6
            row1Data.add("海关数据");  // 7
            row1Data.addAll(Collections.nCopies(6, ""));  // 8-13
            row1Data.add("海关核对结果");  // 14
            row1Data.add("附件核对结果");  // 15
            row1Data.add("附件内容");  // 16
            writer.writeRow(row1Data);

            // 合并第一行单元格（使用Apache POI的方式）
            try {
                org.apache.poi.ss.usermodel.Sheet sheet = writer.getSheet();
                // 报关单数据：合并第0行的0-6列（A-G）
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));
                // 海关数据：合并第0行的7-13列（H-N）
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 7, 13));
                log.debug("第一行单元格合并完成");
            } catch (Exception e) {
                log.warn("合并单元格失败，继续执行: {}", e.getMessage());
            }

            // 第二行：具体字段名
            List<String> row2Headers = new ArrayList<>();
            // 报关单数据的7个字段
            row2Headers.addAll(List.of("报关单号", "合同协议号", "实际开船日期", "运输条款", "结算币别", "总金额", "数量"));
            // 海关数据的7个字段
            row2Headers.addAll(List.of("出口货物报关单号", "出口日期", "成交方式", "成交单位", "成交数量", "成交货币", "成交总价"));
            // 核对结果字段
            row2Headers.addAll(List.of("海关核对结果", "附件核对结果", "附件内容"));
            writer.writeRow(row2Headers);

            // 第三行：数据值
            List<Object> row3Data = new ArrayList<>();

            // 报关单数据（7个字段）
            if (baoguandanData != null) {
                JSONObject bgdJson = JSONUtil.parseObj(baoguandanData);
                row3Data.add(bgdJson.get("报关单号"));
                row3Data.add(bgdJson.get("合同协议号"));
                row3Data.add(bgdJson.get("实际开船日期"));
                row3Data.add(bgdJson.get("运输条款"));
                row3Data.add(bgdJson.get("结算币别"));
                row3Data.add(bgdJson.get("总金额"));
                row3Data.add(bgdJson.get("数量"));
            } else {
                row3Data.addAll(Collections.nCopies(7, ""));
            }

            // 海关数据（7个字段）- 从汇总JSON中获取
            if (customsData != null && customsData.getSummaryJson() != null) {
                JSONObject cJson = JSONUtil.parseObj(customsData.getSummaryJson());
                row3Data.add(cJson.get("出口货物报关单号"));
                row3Data.add(cJson.get("出口日期"));
                row3Data.add(cJson.get("成交方式"));
                row3Data.add(cJson.get("成交单位"));
                row3Data.add(cJson.get("成交数量"));
                row3Data.add(cJson.get("成交货币"));
                row3Data.add(cJson.get("成交总价"));
            } else {
                row3Data.addAll(Collections.nCopies(7, ""));
            }

            // 海关核对结果
            if (comparisonResult != null && comparisonResult.getCustomsCheckResult() != null) {
                row3Data.add(comparisonResult.getCustomsCheckResult().getIsMatch() ? "匹配" : "不匹配");
                if (comparisonResult.getCustomsCheckResult().getReason() != null) {
                    row3Data.set(row3Data.size() - 1,
                            row3Data.get(row3Data.size() - 1) + ": " + comparisonResult.getCustomsCheckResult().getReason());
                }
            } else {
                row3Data.add("");
            }

            // 附件核对结果
            if (comparisonResult != null && comparisonResult.getAttrCheckResult() != null && !comparisonResult.getAttrCheckResult().isEmpty()) {
                StringBuilder attrResult = new StringBuilder();
                for (com.toowe.stwe.dto.AttrCheckResult attr : comparisonResult.getAttrCheckResult()) {
                    if (attrResult.length() > 0) {
                        attrResult.append("\n");
                    }
                    attrResult.append(attr.getRuleName()).append(": ")
                            .append(attr.getIsMatch() ? "匹配" : "不匹配");
                    if (attr.getReason() != null) {
                        attrResult.append("\n").append(attr.getReason());
                    }
                }
                row3Data.add(attrResult.toString());
            } else {
                row3Data.add("");
            }

            // 附件内容（将所有附件的解析内容合并）
            if (attachmentData != null && attachmentData.getAttachments() != null) {
                StringBuilder attachmentContent = new StringBuilder();
                for (BaoguandanAttachmentVO.AttachmentItem item : attachmentData.getAttachments()) {
                    if (attachmentContent.length() > 0) {
                        attachmentContent.append("\n---\n");
                    }
                    attachmentContent.append("【").append(item.getFileName()).append("】\n");
                    attachmentContent.append(item.getParsedContent());
                }
                row3Data.add(attachmentContent.toString());
            } else {
                row3Data.add("");
            }

            writer.writeRow(row3Data);
            log.info("比对结果写入完成");

            log.info("Excel文件导出成功: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("导出Excel文件失败", e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * 根据报关单号查找比对结果Excel文件
     * @param declarationNo 报关单号
     * @return 找到的Excel文件
     * @throws RuntimeException 如果文件不存在则抛出异常
     */
    public File findComparisonExcelFile(String declarationNo) {
        File dir = new File(outputDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("输出目录不存在: {}", outputDir);
            throw new RuntimeException("没有找到对应的比对结果文件，请先进行数据比对");
        }

        // 文件名格式：数据比对_{报关单号}.xlsx
        String fileName = String.format("数据比对_%s.xlsx", declarationNo);
        File file = new File(dir, fileName);

        if (!file.exists()) {
            log.warn("未找到报关单号 {} 的比对结果Excel文件: {}", declarationNo, file.getAbsolutePath());
            throw new RuntimeException("没有找到对应的比对结果文件，请先进行数据比对");
        }

        log.info("找到比对结果Excel文件: {}", file.getAbsolutePath());
        return file;
    }
}
