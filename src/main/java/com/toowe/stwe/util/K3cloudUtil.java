package com.toowe.stwe.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class K3cloudUtil {

    /**
     * 中文字符转Unicode
     */
    private static String chinaToUnicode(String str) {
        if (str == null) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            int chr1 = str.charAt(i);
            if (chr1 >= 19968 && chr1 <= 171941) { // 汉字范围
                result.append("\\u").append(Integer.toHexString(chr1));
            } else {
                result.append(str.charAt(i));
            }
        }
        return result.toString();
    }

    /**
     * 查询报关单数据 (View 接口)
     * @param url K3Cloud接口地址
     * @param number 单据编号
     * @return 查询结果
     */
    public static String viewBaoguandan(String url, String number) throws Exception {
        JSONArray paras = new JSONArray();
        paras.put("ora_baoguandan");

        JSONObject data = new JSONObject();
        data.put("CreateOrgId", 0);
        data.put("Number", number);
        paras.put(data);

        Map<String, Object> formParams = new HashMap<>();
        formParams.put("format", 1);
        formParams.put("useragent", "ApiClient");
        formParams.put("rid", UUID.randomUUID().toString().hashCode());
        formParams.put("parameters", chinaToUnicode(paras.toString()));
        formParams.put("timestamp", new Date().toString());
        formParams.put("v", "1.0");

        log.info("K3Cloud Request (View) [Form]: URL={}, Params={}", url, formParams);

        String response = HttpRequest.post(url)
                .form(formParams)
                .execute()
                .body();

        return response;
    }

    /**
     * 执行单据查询 (ExecuteBillQuery 接口)
     * @param url K3Cloud接口地址
     * @param number 报关单号
     * @return 单据编号
     */
    public static String executeBillQuery(String url, String number) throws Exception {
        JSONObject data = new JSONObject();
        data.put("FormId", "ora_baoguandan");
        data.put("FieldKeys", "F_ora_baoguanno,fbillno");

        JSONArray filterArray = new JSONArray();
        JSONObject filter = new JSONObject();
        filter.put("Left", "(");
        filter.put("FieldName", "F_ora_baoguanno");
        filter.put("Compare", "67");
        filter.put("Value", number);
        filter.put("Right", ")");
        filter.put("Logic", "0");
        filterArray.put(filter);

        data.put("FilterString", filterArray);
        data.put("OrderString", "");
        data.put("TopRowCount", 0);
        data.put("StartRow", 0);
        data.put("Limit", 0);

        Map<String, Object> formParams = new HashMap<>();
        formParams.put("format", 1);
        formParams.put("useragent", "ApiClient");
        formParams.put("rid", UUID.randomUUID().toString().hashCode());
        formParams.put("data", data.toString());
        formParams.put("timestamp", new Date().toString());
        formParams.put("v", "1.0");

        log.info("K3Cloud Request (Query) [Form]: URL={}, Params={}", url, formParams);

        String response = HttpRequest.post(url)
                .form(formParams)
                .execute()
                .body();

        return response;
    }
}
