package com.toowe.stwe.parser;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toowe.stwe.dto.ParserResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class RemoteFileParser {

    @Value("${app.parser-api.url}")
    private String parserApiUrl;

    @Value("${app.parser-api.grant-code}")
    private String grantCode;

    @Value("${app.llm-api.url}")
    private String llmApiUrl;

    @Value("${app.llm-api.api-key}")
    private String llmApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 支持的图片格式
     */
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            "jpeg", "jpg", "png", "bmp", "gif", "svg", "svgz", "webp",
            "ico", "xbm", "dib", "pjp", "tif", "tiff", "pjpeg", "avif", "apng"
    );

    /**
     * 判断文件是否为图片格式
     */
    public boolean isImageFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        int lastDotIndex = lowerName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return false;
        }
        String extension = lowerName.substring(lastDotIndex + 1);
        return IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * 使用大模型解析图片内容
     * 使用Moonshot API的文件提取功能
     * @param imageBytes 图片字节数组
     * @param fileName 文件名
     * @return 图片中的文本内容
     */
    public String parseImageWithLLM(byte[] imageBytes, String fileName) {
        log.info("使用Moonshot API解析图片: {}, 大小: {} bytes", fileName, imageBytes.length);

        try {
            // 第一步：上传文件
            String fileId = uploadFileToMoonshot(imageBytes, fileName);
            log.info("文件上传成功，file_id: {}", fileId);

            // 第二步：获取文件内容
            String content = getFileContentFromMoonshot(fileId);
            log.info("图片解析成功，提取文本长度: {}", content != null ? content.length() : 0);

            return content != null ? content : "";

        } catch (Exception e) {
            log.error("使用Moonshot API解析图片失败", e);
            throw new RuntimeException("图片解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到Moonshot API
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return file_id
     */
    private String uploadFileToMoonshot(byte[] fileBytes, String fileName) {
        try {
            // 使用Hutool上传文件
            HttpResponse response = HttpRequest.post("https://api.moonshot.cn/v1/files")
                    .header("Authorization", "Bearer " + llmApiKey)
                    .form("purpose", "file-extract")
                    .form("file", fileBytes, fileName)
                    .timeout(60000)
                    .execute();

            String responseBody = response.body();
            log.info("文件上传响应状态: {}", response.getStatus());

            if (!response.isOk()) {
                throw new RuntimeException("文件上传失败: " + response.getStatus() + ", Body: " + responseBody);
            }

            // 解析响应获取file_id
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            String fileId = responseJson.getStr("id");

            if (fileId == null || fileId.isEmpty()) {
                throw new RuntimeException("未能获取file_id: " + responseBody);
            }

            return fileId;

        } catch (Exception e) {
            log.error("上传文件到Moonshot失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从Moonshot API获取文件内容
     * @param fileId 文件ID
     * @return 文件内容
     */
    private String getFileContentFromMoonshot(String fileId) {
        try {
            String url = String.format("https://api.moonshot.cn/v1/files/%s/content", fileId);

            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", "Bearer " + llmApiKey)
                    .timeout(60000)
                    .execute();

            String responseBody = response.body();
            log.info("获取文件内容响应状态: {}", response.getStatus());

            if (!response.isOk()) {
                throw new RuntimeException("获取文件内容失败: " + response.getStatus() + ", Body: " + responseBody);
            }

            // 解析响应获取content
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            String content = responseJson.getStr("content");

            return content;

        } catch (Exception e) {
            log.error("从Moonshot获取文件内容失败", e);
            throw new RuntimeException("获取文件内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用远程解析接口,返回解析后的文件内容
     */
    public String parse(byte[] fileBytes, String fileName, String parserType) {
        // 判断是否为图片文件
        if (isImageFile(fileName)) {
            log.info("检测到图片文件: {}, 使用大模型解析", fileName);
            return parseImageWithLLM(fileBytes, fileName);
        }

        // 非图片文件使用原有的解析逻辑
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
            int responseLength = responseBody.length();
            log.info("Remote Parser Response received (Status: {}, Length: {} bytes, Size: {} MB)",
                    response.getStatus(), responseLength, responseLength / (1024 * 1024));

            if (!response.isOk()) {
                throw new RuntimeException("接口返回非200状态码: " + response.getStatus() + ", Body: " + responseBody);
            }

            try {
                // 使用JSONObject直接解析，只提取需要的字段，避免加载result.json大字段
                JSONObject responseJson = JSONUtil.parseObj(responseBody);
                // 检查响应状态
                Integer code = responseJson.getInt("code");
                if (code == null || code != 200) {
                    String msg = responseJson.getStr("msg");
                    log.error("======= 文件解析失败: {}", msg);
                    return "文件解析失败: " + msg;
                }
                Boolean success = responseJson.getBool("success");
                if (success == null || !success) {
                    String msg = responseJson.getStr("msg");
                    log.error("======= 文件解析失败: {}", msg);
                    return "文件解析失败: " + msg;
                }
                // 获取data对象
                JSONObject dataJson = responseJson.getJSONObject("data");
                if (dataJson == null) {
                    log.error("======= 文件解析结果为空");
                    return "文件解析结果为空";
                }
                // 获取process_results数组
                Object processResultsObj = dataJson.get("process_results");
                if (processResultsObj == null) {
                    log.error("======= 文件解析处理结果列表为空");
                    return "文件解析处理结果列表为空";
                }

                // 提取第一个处理结果的output中的result.txt
                // 不反序列化整个对象，避免加载result.json大字段
                Object firstResult = JSONUtil.parseArray(processResultsObj).get(0);
                if (firstResult instanceof JSONObject) {
                    JSONObject firstResultJson = (JSONObject) firstResult;
                    JSONObject outputJson = firstResultJson.getJSONObject("output");
                    if (outputJson == null) {
                        log.error("======= 文件解析输出为空");
                        return "文件解析输出为空";
                    }
                    String resultTxt = outputJson.getStr("result.txt");
                    // 去除字符串中的空字符
                    if (resultTxt != null) {
                        resultTxt = resultTxt.replace("\0", "");
                    }
                    return resultTxt;
                } else {
                    log.error("======= process_results格式错误");
                    return "文件解析处理结果格式错误";
                }

            } catch (Exception e) {
                log.error("解析响应数据失败! 响应长度: {}", responseBody.length(), e);
                throw new RuntimeException("解析接口返回数据格式错误: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Failed to call parser API", e);
            throw new RuntimeException("远程解析接口调用失败: " + e.getMessage(), e);
        }
    }

}