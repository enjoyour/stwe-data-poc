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
}
