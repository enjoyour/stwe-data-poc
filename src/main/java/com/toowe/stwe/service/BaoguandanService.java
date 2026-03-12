package com.toowe.stwe.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toowe.stwe.dto.BaoguandanResponse;
import com.toowe.stwe.util.K3cloudUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BaoguandanService {

    @Value("${k3cloud.url}")
    private String k3cloudUrl;

    private static final String VIEW_URL_SUFFIX = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";
    private static final String QUERY_URL_SUFFIX = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取报关单数据
     * @param number 报关单号，如 310120210519750857
     * @return 查询结果
     */
    public BaoguandanResponse getBaoguandan(String number) {
        try {
            // 第一步：执行单据查询，获取单据编号
            String queryUrl = k3cloudUrl + QUERY_URL_SUFFIX;
            log.info("执行单据查询，URL: {}, Number: {}", queryUrl, number);
            String queryResult = K3cloudUtil.executeBillQuery(queryUrl, number);
            log.info("单据查询结果: {}", queryResult);

            // 解析查询结果，取出第二个值作为单据编号
            JSONArray resultArray = new JSONArray(queryResult);
            if (resultArray.isEmpty()) {
                throw new RuntimeException("未查询到相关数据，Number: " + number);
            }
            JSONArray firstRow = resultArray.getJSONArray(0);
            String billNo = firstRow.getStr(1);
            log.info("获取到单据编号: {}", billNo);

            // 第二步：使用单据编号查询报关单详情
            String viewUrl = k3cloudUrl + VIEW_URL_SUFFIX;
            log.info("查询报关单详情，URL: {}, BillNo: {}", viewUrl, billNo);
            String viewResult = K3cloudUtil.viewBaoguandan(viewUrl, billNo);

            // 解析为响应对象
            return objectMapper.readValue(viewResult, BaoguandanResponse.class);

        } catch (Exception e) {
            log.error("查询报关单失败，Number: {}", number, e);
            throw new RuntimeException("查询报关单失败: " + e.getMessage());
        }
    }
}
