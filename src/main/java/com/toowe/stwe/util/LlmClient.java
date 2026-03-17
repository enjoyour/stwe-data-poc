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


    /**
     * 使用HTTP直接调用大模型API，支持传递thinking参数
     */
    public String callLLMWithThinking(String systemPrompt, String userPrompt) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", properties.getModel());

            // 构建消息数组
            JSONArray messages = new JSONArray();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.set("messages", messages);

            // 添加thinking参数
            if (!properties.getThinkingEnabled()) {
                JSONObject thinking = new JSONObject();
                thinking.set("type", "disabled");
                requestBody.set("thinking", thinking);
            } else {
                log.info("思考模式已启用");
            }

            // 发送HTTP请求
            String bodyString = requestBody.toString();
            log.info("发送HTTP请求: {}", bodyString);
            HttpResponse response = HttpRequest.post(properties.getUrl())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(bodyString)
                    .execute();

            String responseBody = response.body();
            log.info("API响应状态码: {}", response.getStatus());

            if (!response.isOk()) {
                throw new RuntimeException("API调用失败，状态码: " + response.getStatus() + ", 响应: " + responseBody);
            }

            // 解析响应
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            JSONArray choices = responseJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getStr("content");
            }

            throw new RuntimeException("无法解析API响应: " + responseBody);
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            throw new RuntimeException("调用大模型失败：" + e.getMessage(), e);
        }
    }
}
