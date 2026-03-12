package com.toowe.stwe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BaoguandanVO {

    @JsonProperty("报关单号")
    private String baoGuanno;

    @JsonProperty("合同协议号")
    private String xieyiNo;

    @JsonProperty("实际开船日期")
    private String kaichuanDate;

    @JsonProperty("运输条款")
    private String terms;

    @JsonProperty("结算币别")
    private String settleCurrency;

    @JsonProperty("FCODE")
    private String fcode;

    @JsonProperty("FSYMBOL")
    private String fsymbol;

    @JsonProperty("总金额")
    private Double totalAmount;

    @JsonProperty("数量")
    private Double quantity;
}
