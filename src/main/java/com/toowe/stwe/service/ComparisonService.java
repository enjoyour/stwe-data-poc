package com.toowe.stwe.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.toowe.stwe.dto.BaoguandanAttachmentVO;
import com.toowe.stwe.dto.BaoguandanComparisonVO;
import com.toowe.stwe.util.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
            CompletableFuture<JSONObject> customsTask = CompletableFuture.supplyAsync(() -> customsDataService.getCustomsData(number));

            try {
                CompletableFuture.allOf(baoguandanTask, attachmentTask, customsTask).join();

                String bData = baoguandanTask.get();
                BaoguandanAttachmentVO aData = attachmentTask.get();
                JSONObject cData = customsTask.get();

                // 组装 VO
                BaoguandanComparisonVO vo = new BaoguandanComparisonVO();
                vo.setBaoguandanData(bData);
                vo.setAttachmentData(aData);
                vo.setCustomsData(cData);

                // 填充用户提示词模板中的变量
                String finalUserPrompt = userPromptTemplate
                        .replace("{baoguandanData}", bData != null ? bData : "无数据")
                        .replace("{attachmentData}", aData != null ? JSONUtil.toJsonStr(aData) : "无数据")
                        .replace("{customsData}", cData != null ? cData.toString() : "无数据");

                // 调用 LLM 进行比对分析
                log.info("正在调用 LLM 进行数据比对: {}", number);
                String comparisonResult = llmClient.callLLM(systemPrompt, finalUserPrompt);
                vo.setComparisonResult(comparisonResult);

                resultList.add(vo);
                log.info("单号 {} 的数据比对分析完成", number);

            } catch (Exception e) {
                log.error("处理单号 {} 时发生错误: {}", number, e.getMessage());
                throw new RuntimeException("处理单号 " + number + " 时发生错误: " + e.getMessage());
            }
        }

        return resultList;
    }
}
