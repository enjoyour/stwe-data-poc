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

    @Value("${app.llm-api.max-retries:3}")
    private int maxRetries;

    @Value("${app.llm-api.retry-delay-base:1000}")
    private int retryDelayBase;

    @Value("${app.llm-api.timeout:60000}")
    private int timeout;

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
     * 上传文件到Moonshot API（带重试机制）
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return file_id
     */
    private String uploadFileToMoonshot(byte[] fileBytes, String fileName) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("尝试上传文件到Moonshot (第{}次)", attempt);

                // 使用Hutool上传文件
                HttpResponse response = HttpRequest.post("https://api.moonshot.cn/v1/files")
                        .header("Authorization", "Bearer " + llmApiKey)
                        .form("purpose", "file-extract")
                        .form("file", fileBytes, fileName)
                        .timeout(timeout)
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

                // 如果重试过，记录成功日志
                if (attempt > 1) {
                    log.info("文件上传成功（重试{}次后），file_id: {}", attempt - 1, fileId);
                } else {
                    log.info("文件上传成功，file_id: {}", fileId);
                }

                return fileId;

            } catch (cn.hutool.core.io.IORuntimeException e) {
                lastException = e;
                Throwable cause = e.getCause();

                // 判断是否为可重试的错误
                boolean isRetryable = isRetryableError(e, cause);

                if (isRetryable && attempt < maxRetries) {
                    long delayMs = calculateRetryDelay(attempt);
                    log.warn("文件上传失败（可重试），第{}次重试将在{}ms后进行。错误类型: {}, 错误信息: {}",
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
                log.error("上传文件到Moonshot失败（已重试{}次）", attempt - 1, e);
                throw new RuntimeException("上传文件失败: " + e.getMessage(), e);

            } catch (Exception e) {
                lastException = e;
                log.error("上传文件到Moonshot失败（第{}次尝试）", attempt, e);

                // 非网络相关错误直接抛出，不重试
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("上传文件失败: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("上传文件到Moonshot失败（已重试" + maxRetries + "次）",
                lastException != null ? lastException : new Exception("未知错误"));
    }

    /**
     * 从Moonshot API获取文件内容（带重试机制）
     * @param fileId 文件ID
     * @return 文件内容
     */
    private String getFileContentFromMoonshot(String fileId) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("尝试获取文件内容 (第{}次)", attempt);

                String url = String.format("https://api.moonshot.cn/v1/files/%s/content", fileId);

                HttpResponse response = HttpRequest.get(url)
                        .header("Authorization", "Bearer " + llmApiKey)
                        .timeout(timeout)
                        .execute();

                String responseBody = response.body();
                log.info("获取文件内容响应状态: {}", response.getStatus());

                if (!response.isOk()) {
                    throw new RuntimeException("获取文件内容失败: " + response.getStatus() + ", Body: " + responseBody);
                }

                // 解析响应获取content
                JSONObject responseJson = JSONUtil.parseObj(responseBody);
                String content = responseJson.getStr("content");

                // 清理文本内容
                if (content != null) {
                    content = cleanParsedContent(content);
                }

                // 如果重试过，记录成功日志
                if (attempt > 1) {
                    log.info("获取文件内容成功（重试{}次后）", attempt - 1);
                }

                return content;

            } catch (cn.hutool.core.io.IORuntimeException e) {
                lastException = e;
                Throwable cause = e.getCause();

                // 判断是否为可重试的错误
                boolean isRetryable = isRetryableError(e, cause);

                if (isRetryable && attempt < maxRetries) {
                    long delayMs = calculateRetryDelay(attempt);
                    log.warn("获取文件内容失败（可重试），第{}次重试将在{}ms后进行。错误类型: {}, 错误信息: {}",
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
                log.error("从Moonshot获取文件内容失败（已重试{}次）", attempt - 1, e);
                throw new RuntimeException("获取文件内容失败: " + e.getMessage(), e);

            } catch (Exception e) {
                lastException = e;
                log.error("从Moonshot获取文件内容失败（第{}次尝试）", attempt, e);

                // 非网络相关错误直接抛出，不重试
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("获取文件内容失败: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("从Moonshot获取文件内容失败（已重试" + maxRetries + "次）",
                lastException != null ? lastException : new Exception("未知错误"));
    }

    /**
     * 判断错误是否可重试
     */
    private boolean isRetryableError(cn.hutool.core.io.IORuntimeException e, Throwable cause) {
        if (cause instanceof java.net.UnknownHostException) {
            return true;
        }
        if (cause instanceof java.net.SocketTimeoutException) {
            return true;
        }
        if (cause instanceof java.net.ConnectException) {
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
     */
    private long calculateRetryDelay(int attempt) {
        return retryDelayBase * (1L << (attempt - 1));
    }

    /**
     * 清理解析后的文本内容
     * 去除多余的制表符、空白字符等
     */
    private String cleanParsedContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // 去除null字符
        content = content.replace("\0", "");

        // 将多个连续的制表符替换为单个制表符
        content = content.replaceAll("\t+", "\t");

        // 去除行首尾的制表符
        content = content.replaceAll("^\t+|\t+$", "");

        // 去除行首尾的空格
        content = content.replaceAll("^ +| +$", "");

        // 将连续的空格替换为单个空格
        content = content.replaceAll(" +", " ");

        // 去除过多的连续换行符（保留最多2个换行）
        content = content.replaceAll("\n\n\n+", "\n\n");

        // 逐行处理：去除行尾的制表符和空格
        String[] lines = content.split("\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            // 去除行尾的制表符和空格
            line = line.replaceAll("[\t ]+$", "");
            cleaned.append(line).append("\n");
        }
        if (cleaned.length() > 0) {
            content = cleaned.toString();
            // 移除最后的换行符
            if (content.endsWith("\n")) {
                content = content.substring(0, content.length() - 1);
            }
        }

        return content;
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
                    // 清理文本内容：去除多余的制表符和空白字符
                    if (resultTxt != null) {
                        resultTxt = cleanParsedContent(resultTxt);
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