package com.toowe.stwe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaoguandanResponse {

    @JsonProperty("Result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("ResponseStatus")
        private ResponseStatus responseStatus;

        @JsonProperty("Result")
        private BaoguandanData result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseStatus {
        @JsonProperty("IsSuccess")
        private Boolean isSuccess;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaoguandanData {
        @JsonProperty("Id")
        private String id;

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

        @JsonProperty("F_Stwr_ZAmount")
        private Double totalAmount;

        @JsonProperty("FQTY")
        private Double quantity;

        @JsonProperty("FEntity")
        private List<Entity> fEntity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entity {
        @JsonProperty("FQty")
        private Double fQty;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MultiLanguageText {
        @JsonProperty("PkId")
        private String pkId;

        @JsonProperty("LocaleId")
        private Integer localeId;

        @JsonProperty("FDataValue")
        private String fDataValue;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrencyName {
        @JsonProperty("PkId")
        private Long pkId;

        @JsonProperty("LocaleId")
        private Integer localeId;

        @JsonProperty("Name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyValue {
        @JsonProperty("Key")
        private Integer key;

        @JsonProperty("Value")
        private String value;
    }
}
