package com.toowe.stwe.parser;

import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toowe.stwe.dto.ParserResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class RemoteFileParser {

    @Value("${app.parser-api.url}")
    private String parserApiUrl;

    @Value("${app.parser-api.grant-code}")
    private String grantCode;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用远程解析接口,返回解析后的文件内容
     */
    public String parse(byte[] fileBytes, String fileName, String parserType) {
        log.info("Calling Remote Parser API: {}, file: {}, type: {}", parserApiUrl, fileName, parserType);

        try {
            // 参数 JSON
            String paramsJson = String.format("{\"parser_type\": \"%s\", \"async\": false}",
                    parserType != null ? parserType : "auto");

            // 使用 Hutool 发送请求
            var response = HttpRequest.post(parserApiUrl)
                    .header("grant_code", grantCode)
                    .form("file", fileBytes, fileName)
                    .form("parser_params", paramsJson)
                    .timeout(120000)
                    .execute();

            String responseBody = response.body();
            log.info("Remote Parser Response received (Status: {}, Length: {})", response.getStatus(), responseBody.length());

            if (!response.isOk()) {
                throw new RuntimeException("接口返回非200状态码: " + response.getStatus() + ", Body: " + responseBody);
            }

            try {
                ParserResponseDTO parserResp = objectMapper.readValue(responseBody, ParserResponseDTO.class);
                if (200 != parserResp.getCode()) {
                    log.error("======= 文件解析失败: {}", parserResp.getMsg());
                    return "文件解析失败: " + parserResp.getMsg();
                }
                if (!Boolean.TRUE.equals(parserResp.isSuccess())) {
                    log.error("======= 文件解析失败: {}", parserResp.getMsg());
                    return "文件解析失败: " + parserResp.getMsg();
                }
                if (parserResp.getData() == null) {
                    log.error("======= 文件解析结果为空");
                    return "文件解析结果为空";
                }
                if (CollectionUtils.isEmpty(parserResp.getData().getProcessResults())) {
                    log.error("======= 文件解析处理结果列表为空");
                    return "文件解析处理结果列表为空";
                }
                ParserResponseDTO.ProcessResult processResult = parserResp.getData().getProcessResults().get(0);
                if (processResult.getOutput() == null) {
                    log.error("======= 文件解析输出为空");
                    return "文件解析输出为空";
                }
                String resultTxt = (String) processResult.getOutput().get("result.txt");
                // 去除字符串中的空字符
                if (resultTxt != null) {
                    resultTxt = resultTxt.replace("\0", "");
                }
                return resultTxt;
            } catch (Exception e) {
                log.error("JSON反序列化失败! 响应内容: {}", responseBody);
                throw new RuntimeException("解析接口返回数据格式错误: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Failed to call parser API", e);
            throw new RuntimeException("远程解析接口调用失败: " + e.getMessage(), e);
        }
    }

}