package com.toowe.stwe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.llm-api")
public class LlmProperties {
    /**
     * 大模型 API URL (例如: https://api.openai.com/v1/chat/completions)
     */
    private String url;

    /**
     * 大模型使用的模型名称 (例如: gpt-4o)
     */
    private String model;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 是否启用思考模式（默认关闭）
     * true: 启用思考模式
     * false: 禁用思考模式（传递 thinking: {type: "disabled"}）
     */
    private Boolean thinkingEnabled = false;
}
