package com.toowe.stwe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 文件解析接口返回 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParserResponseDTO {
    private int code;
    private String msg;
    private Data data;
    private List<Image> images;
    private boolean success;
    private String time;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("task_id")
        private String taskId;
        @JsonProperty("file_id")
        private String fileId;
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("file_suffix")
        private String fileSuffix;
        @JsonProperty("consume_id")
        private String consumeId;
        @JsonProperty("task_type")
        private String taskType;
        @JsonProperty("parser_type")
        private String parserType;
        @JsonProperty("local_file_path")
        private String localFilePath;
        @JsonProperty("storage_type")
        private String storageType;
        @JsonProperty("storage_path")
        private String storagePath;
        @JsonProperty("process_results")
        private List<ProcessResult> processResults;
        private List<Image> images;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessResult {
        @JsonProperty("process_id")
        private String processId;
        @JsonProperty("parser_id")
        private int parserId;
        private String result;
        /**
         * 包含 result.txt, result.json 以及可能存在的 images 数组
         */
        private Map<String, Object> output;
        @JsonProperty("process_type")
        private String processType;
        @JsonProperty("parser_type")
        private String parserType;
        @JsonProperty("start_time")
        private String startTime;
        @JsonProperty("end_time")
        private String endTime;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String path;
        private String name;
        private String base64;
    }
}
