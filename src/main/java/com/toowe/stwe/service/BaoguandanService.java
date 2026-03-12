package com.toowe.stwe.service;

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

    /**
     * 获取报关单数据
     * @param number 单据编号
     * @return 查询结果
     */
    public String getBaoguandan(String number) {
        try {
            String url = k3cloudUrl + VIEW_URL_SUFFIX;
            log.info("查询报关单，URL: {}, Number: {}", url, number);
            return K3cloudUtil.viewBaoguandan(url, number);
        } catch (Exception e) {
            log.error("查询报关单失败，Number: {}", number, e);
            throw new RuntimeException("查询报关单失败: " + e.getMessage());
        }
    }
}
