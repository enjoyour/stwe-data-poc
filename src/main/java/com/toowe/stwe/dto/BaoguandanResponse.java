package com.toowe.stwe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BaoguandanResponse {

    @JsonProperty("Result")
    private Result result;

    @Data
    public static class Result {
        @JsonProperty("ResponseStatus")
        private ResponseStatus responseStatus;

        @JsonProperty("Result")
        private BaoguandanData result;
    }

    @Data
    public static class ResponseStatus {
        @JsonProperty("IsSuccess")
        private Boolean isSuccess;
    }

    @Data
    public static class BaoguandanData {
        @JsonProperty("F_ora_baoguanno")
        private String foraBaoGuanno;

        @JsonProperty("F_ora_xieyino")
        private String foraXieyiNo;

        @JsonProperty("FkaichuanDate")
        private String kaichuanDate;

        @JsonProperty("F_ora_terms")
        private ForaTerms foraTerms;

        @JsonProperty("F_ora_SETTLECURRID")
        private SettleCurrency settleCurrency;

        // 可以根据需要添加更多字段
    }

    @Data
    public static class ForaTerms {
        @JsonProperty("Id")
        private String id;

        @JsonProperty("FNumber")
        private String fNumber;

        @JsonProperty("MultiLanguageText")
        private List<MultiLanguageText> multiLanguageText;

        @JsonProperty("FDataValue")
        private List<KeyValue> fDataValue;
    }

    @Data
    public static class SettleCurrency {
        @JsonProperty("Id")
        private Long id;

        @JsonProperty("msterID")
        private Long masterId;

        @JsonProperty("MultiLanguageText")
        private List<CurrencyName> multiLanguageText;

        @JsonProperty("Name")
        private List<KeyValue> name;
    }

    @Data
    public static class MultiLanguageText {
        @JsonProperty("PkId")
        private String pkId;

        @JsonProperty("LocaleId")
        private Integer localeId;

        @JsonProperty("FDataValue")
        private String fDataValue;
    }

    @Data
    public static class CurrencyName {
        @JsonProperty("PkId")
        private Long pkId;

        @JsonProperty("LocaleId")
        private Integer localeId;

        @JsonProperty("Name")
        private String name;
    }

    @Data
    public static class KeyValue {
        @JsonProperty("Key")
        private Integer key;

        @JsonProperty("Value")
        private String value;
    }
}
