package com.toowe.stwe.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpCookie;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class K3cloudUtil {

    private static List<HttpCookie> cookies;

    /**
     * 登录 K3Cloud
     */
    public static boolean login(String url, String dbId, String user, String pwd, int lang) throws Exception {
        JSONArray paras = new JSONArray();
        paras.add(dbId);
        paras.add(user);
        paras.add(pwd);
        paras.add(lang);

        Map<String, Object> formParams = new HashMap<>();
        formParams.put("format", 1);
        formParams.put("useragent", "ApiClient");
        formParams.put("rid", UUID.randomUUID().toString().hashCode());
        formParams.put("parameters", paras.toString());
        formParams.put("timestamp", new Date().toString());
        formParams.put("v", "1.0");

        log.info("K3Cloud Login Request: URL={}, Params={}", url, formParams);

        HttpResponse response = HttpRequest.post(url)
                .form(formParams)
                .execute();

        String body = response.body();
        log.info("K3Cloud Login Response: {}", body);

        JSONObject json = JSONUtil.parseObj(body);
        if (json.getInt("LoginResultType") == 1) {
            cookies = response.getCookies();
            log.info("登录成功，获取到 {} 个 Cookie", cookies.size());
            return true;
        } else {
            log.error("登录失败: {}", body);
            return false;
        }
    }

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

        return HttpRequest.post(url)
                .form(formParams)
                .cookie(cookies)
                .execute()
                .body();
    }

    /**
     * 执行单据查询 (ExecuteBillQuery 接口)
     */
    public static String executeBillQuery(String url, String number) throws Exception {
        JSONObject data = new JSONObject();
        data.put("FormId", "ora_baoguandan");
        data.put("FieldKeys", "F_ora_baoguanno,fbillno,fid");

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

        return HttpRequest.post(url)
                .form(formParams)
                .cookie(cookies)
                .execute()
                .body();
    }

    /**
     * 获取附件列表
     */
    public static String queryAttachments(String url, String interId) {
        JSONObject queryParam = new JSONObject();
        queryParam.put("FormId", "BOS_Attachment");
        queryParam.put("FieldKeys", "FFileId,FAttachmentName");
        queryParam.put("TopRowCount", 0);
        queryParam.put("Limit", 0);
        queryParam.put("StartRow", 0);

        JSONArray filterArray = new JSONArray();
        // 过滤条件 1: FInterID
        JSONObject filter1 = new JSONObject();
        filter1.put("FieldName", "FInterID");
        filter1.put("Compare", "67");
        filter1.put("Value", interId);
        filter1.put("Left", "");
        filter1.put("Right", "");
        filter1.put("Logic", "0");
        filterArray.add(filter1);

        // 过滤条件 2: FBillType
        JSONObject filter2 = new JSONObject();
        filter2.put("FieldName", "FBillType");
        filter2.put("Compare", "67");
        filter2.put("Value", "ora_baoguandan");
        filter2.put("Left", "");
        filter2.put("Right", "");
        filter2.put("Logic", "0");
        filterArray.add(filter2);

        queryParam.put("FilterString", filterArray);
        queryParam.put("OrderString", "");

        JSONArray parameters = new JSONArray();
        parameters.add(queryParam);

        JSONObject root = new JSONObject();
        root.put("parameters", parameters);

        log.info("K3Cloud Request (Attachments) [JSON]: URL={}, Body={}", url, root.toString());

        return HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(root.toString())
                .cookie(cookies)
                .execute()
                .body();
    }

    /**
     * 下载附件分片
     */
    public static String downloadAttachment(String url, String fileId) {
        JSONObject param = new JSONObject();
        param.put("FileId", fileId);
        param.put("StartIndex", 0);

        JSONArray parameters = new JSONArray();
        parameters.add(param);

        JSONObject root = new JSONObject();
        root.put("parameters", parameters);

        log.info("K3Cloud Request (Download) [JSON]: URL={}, Body={}", url, root.toString());

        return HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(root.toString())
                .cookie(cookies)
                .execute()
                .body();
    }
}
