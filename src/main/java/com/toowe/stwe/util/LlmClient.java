package com.toowe.stwe.util;

import cn.hutool.core.io.IORuntimeException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final LlmProperties properties;

    @Value("${app.llm-api.max-retries:3}")
    private int maxRetries;

    @Value("${app.llm-api.retry-delay-base:1000}")
    private int retryDelayBase; // 基础重试延迟（毫秒）

    @Value("${app.llm-api.timeout:60000}")
    private int timeout; // HTTP请求超时时间（毫秒）


    /**
     * 使用HTTP直接调用大模型API，支持传递thinking参数
     * 包含重试机制处理网络抖动和DNS解析失败等偶发错误
     */
    public String callLLMWithThinking(String systemPrompt, String userPrompt) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("尝试调用大模型API (第{}次)", attempt);

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
                        .timeout(timeout)
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
                    String content = message.getStr("content");

                    // 自动清理markdown代码块标记（防止大模型返回```json标记）
                    content = cleanMarkdownCodeBlock(content);
                    // 如果重试过，记录成功日志
                    if (attempt > 1) {
                        log.info("API调用成功（重试{}次后）", attempt - 1);
                    }

                    return content;
                }

                throw new RuntimeException("无法解析API响应: " + responseBody);

            } catch (IORuntimeException e) {
                lastException = e;
                Throwable cause = e.getCause();

                // 判断是否为可重试的错误（DNS解析失败、连接超时等）
                boolean isRetryable = isRetryableError(e, cause);

                if (isRetryable && attempt < maxRetries) {
                    long delayMs = calculateRetryDelay(attempt);
                    log.warn("请求失败（可重试），第{}次重试将在{}ms后进行。错误类型: {}, 错误信息: {}",
                            attempt, delayMs, cause != null ? cause.getClass().getSimpleName() : e.getClass().getSimpleName(),
                            cause != null ? cause.getMessage() : e.getMessage());

                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                    continue;
                }

                // 不可重试的错误或已达到最大重试次数
                log.error("调用大模型失败（已重试{}次）", attempt - 1, e);
                throw new RuntimeException("调用大模型失败：" + e.getMessage(), e);

            } catch (Exception e) {
                lastException = e;
                log.error("调用大模型失败（第{}次尝试）", attempt, e);

                // 非网络相关错误直接抛出，不重试
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("调用大模型失败：" + e.getMessage(), e);
            }
        }

        // 理论上不会到达这里，但为了完整性
        throw new RuntimeException("调用大模型失败（已重试" + maxRetries + "次）",
                lastException != null ? lastException : new Exception("未知错误"));
    }

    /**
     * 判断错误是否可重试
     * DNS解析失败、连接超时、网络IO错误等可以重试
     */
    private boolean isRetryableError(IORuntimeException e, Throwable cause) {
        if (cause instanceof UnknownHostException) {
            // DNS解析失败 - 可重试
            return true;
        }
        if (cause instanceof java.net.SocketTimeoutException) {
            // 连接或读取超时 - 可重试
            return true;
        }
        if (cause instanceof java.net.ConnectException) {
            // 连接被拒绝 - 可重试
            return true;
        }
        if (e.getMessage() != null) {
            String msg = e.getMessage();
            if (msg.contains("Connection reset") ||
                msg.contains("Connection timed out") ||
                msg.contains("No route to host")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算重试延迟时间（指数退避）
     * 第1次重试: 1000ms
     * 第2次重试: 2000ms
     * 第3次重试: 4000ms
     */
    private long calculateRetryDelay(int attempt) {
        return retryDelayBase * (1L << (attempt - 1)); // 2^(attempt-1) * baseDelay
    }

    /**
     * 清理markdown代码块标记
     * 防止大模型返回 ```json 或 ``` 等markdown标记
     */
    private String cleanMarkdownCodeBlock(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String trimmed = content.trim();

        // 检测并去除开头的 ```json 或 ``` 标记
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        // 去除结尾的 ``` 标记
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        trimmed = trimmed.trim();

        // 如果清理后的内容与原内容不同，记录日志
        if (!trimmed.equals(content)) {
            log.info("检测到markdown代码块标记，已自动清理");
        }

        return trimmed;
    }
}
