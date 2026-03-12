package com.toowe.stwe.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

public class K3cloudUtil {


    // HttpURLConnection
    private static HttpURLConnection initUrlConn(String url, JSONArray paras)
            throws Exception {
        URL postUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) postUrl
                .openConnection();

        if (!connection.getDoOutput()) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "application/json");
        DataOutputStream out = new DataOutputStream(
                connection.getOutputStream());

        UUID uuid = UUID.randomUUID();
        int hashCode = uuid.toString().hashCode();

        JSONObject jObj = new JSONObject();

        jObj.put("format", 1);
        jObj.put("useragent", "ApiClient");
        jObj.put("rid", hashCode);
        jObj.put("parameters", chinaToUnicode(paras.toString()));
        jObj.put("timestamp", new Date().toString());
        jObj.put("v", "1.0");

        out.writeBytes(jObj.toString());
        out.flush();
        out.close();

        return connection;
    }

    /**
     * 中文字符转Unicode
     */
    private static String chinaToUnicode(String str) {
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            int chr1 = (char) str.charAt(i);
            if (chr1 >= 19968 && chr1 <= 171941) {// 汉字范围 \u4e00-\u9fa5 (中文)
                result += "\\u" + Integer.toHexString(chr1);
            } else {
                result += str.charAt(i);
            }
        }
        return result;
    }

    /**
     * 查询报关单数据
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

        HttpURLConnection connection = initUrlConn(url, paras);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    /**
     * 执行单据查询
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

        URL postUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();

        if (!connection.getDoOutput()) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "application/json");

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());

        UUID uuid = UUID.randomUUID();
        int hashCode = uuid.toString().hashCode();

        JSONObject jObj = new JSONObject();
        jObj.put("format", 1);
        jObj.put("useragent", "ApiClient");
        jObj.put("rid", hashCode);
        jObj.put("data", data.toString());
        jObj.put("timestamp", new Date().toString());
        jObj.put("v", "1.0");

        out.writeBytes(jObj.toString());
        out.flush();
        out.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }
}
