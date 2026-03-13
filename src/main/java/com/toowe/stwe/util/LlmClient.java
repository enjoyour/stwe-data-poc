package com.toowe.stwe.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.toowe.stwe.config.LlmProperties;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final LlmProperties properties;


    public String callLLM(String systemPrompt, String userPrompt) {
        try {
            // 使用 LangChain4j 的 ChatMessage 类型
            List<ChatMessage> messages = List.of(
                    dev.langchain4j.data.message.SystemMessage.from(systemPrompt),
                    dev.langchain4j.data.message.UserMessage.from(userPrompt)
            );

            // 动态创建 ChatLanguageModel，使用配置中心指定的模型
            dev.langchain4j.model.chat.ChatLanguageModel chatModel =
                    dev.langchain4j.model.openai.OpenAiChatModel.builder()
                            .apiKey(properties.getApiKey())
                            .baseUrl(properties.getUrl())
                            .modelName(properties.getModel())
                            .build();

            // 调用大模型
            ChatResponse chatResponse = chatModel.chat(messages);
            return chatResponse.aiMessage().text();
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            throw new RuntimeException("调用大模型失败：" + e.getMessage(), e);
        }
    }
}
