package com.toowe.stwe.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toowe.stwe.dto.BaoguandanAttachmentVO;
import com.toowe.stwe.dto.BaoguandanResponse;
import com.toowe.stwe.parser.RemoteFileParser;
import com.toowe.stwe.util.K3cloudUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaoguandanService {

    @Value("${k3cloud.url}")
    private String k3cloudUrl;

    @Value("${app.parser-api.parser-type.docx:to_html}")
    private String parserTypeDocx;

    @Value("${app.parser-api.parser-type.xlsx:pipline_html}")
    private String parserTypeXlsx;

    @Value("${app.parser-api.parser-type.pdf:mineru_api}")
    private String parserTypePdf;

    private static final String VIEW_URL_SUFFIX = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";
    private static final String QUERY_URL_SUFFIX = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String DOWNLOAD_URL_SUFFIX = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.AttachmentDownLoad.common.kdsvc";
    private static final String AUTH_URL_SUFFIX = "/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final int LOCALE_ID = 2052;  // 这个是定死的,只取2052

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RemoteFileParser remoteFileParser;

    /**
     * 获取报关单附件列表
     */
    public BaoguandanAttachmentVO getBaoguandanAttachments(String number) {
        try {
            // 第零步：登录
            String authUrl = k3cloudUrl + AUTH_URL_SUFFIX;
            boolean loginSuccess = K3cloudUtil.login(authUrl, "65f0680c543e6b", "999999", "pop909", 2052);
            if (!loginSuccess) {
                throw new RuntimeException("K3Cloud 登录失败");
            }

            // 第一步：查询单据内码 (FID)
            String queryUrl = k3cloudUrl + QUERY_URL_SUFFIX;
            String queryResult = K3cloudUtil.executeBillQuery(queryUrl, number);
            JSONArray resultArray = new JSONArray(queryResult);
            if (resultArray.isEmpty()) {
                throw new RuntimeException("未查询到单据数据");
            }
            JSONArray firstRow = resultArray.getJSONArray(0);
            String interId = firstRow.getStr(2); // 获取第三个值：订单内码
            log.info("获取到单据内码 (InterId): {}", interId);

            // 第二步：查询附件列表
            String attachmentResult = K3cloudUtil.queryAttachments(queryUrl, interId);
            log.info("附件查询结果: {}", attachmentResult);

            // 第三步：遍历查询结果，获取每个附件的字节信息并解析内容
            String downloadUrl = k3cloudUrl + DOWNLOAD_URL_SUFFIX;
            JSONArray attachmentArray = new JSONArray(attachmentResult);
            List<BaoguandanAttachmentVO.AttachmentItem> items = new ArrayList<>();

            for (int i = 0; i < attachmentArray.size(); i++) {
                JSONArray row = attachmentArray.getJSONArray(i);
                String fileId = row.getStr(0);
                String fileName = row.getStr(1);

                log.info("开始下载文件: {}, FileId: {}", fileName, fileId);
                String downloadResult = K3cloudUtil.downloadAttachment(downloadUrl, fileId);
                JSONObject downloadObj = new JSONObject(downloadResult);
                
                // 提取 FilePart (Base64 字符串)
                String filePart = downloadObj.getJSONObject("Result").getStr("FilePart");
                
                // Base64 解码为字节数组
                byte[] fileBytes = Base64.decode(filePart);
                
                // 调用远程解析接口
                log.info("开始解析文件内容: {}", fileName);
                String parserType = getParserTypeByExtension(fileName);
                String parsedContent = remoteFileParser.parse(fileBytes, fileName, parserType);
                // 封装结果，包含文件名、解析后的内容和fileId
                items.add(new BaoguandanAttachmentVO.AttachmentItem(fileName, parsedContent, fileId));
            }

            return new BaoguandanAttachmentVO(items);

        } catch (Exception e) {
            log.error("获取报关单附件失败，Number: {}", number, e);
            throw new RuntimeException("获取报关单附件失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件扩展名获取解析器类型
     */
    private String getParserTypeByExtension(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".docx"))
            return parserTypeDocx;
        if (lowerName.endsWith(".xlsx"))
            return parserTypeXlsx;
        if (lowerName.endsWith(".pdf"))
            return parserTypePdf;
        return "normal";
    }

    /**
     * 获取报关单数据
     * @param number 报关单号，如 310120210519750857
     * @return 查询结果JSON字符串
     */
    public String getBaoguandan(String number) {
        try {
            // 第零步：登录
            String authUrl = k3cloudUrl + AUTH_URL_SUFFIX;
            boolean loginSuccess = K3cloudUtil.login(authUrl, "65f0680c543e6b", "999999", "pop909", 2052);
            if (!loginSuccess) {
                throw new RuntimeException("K3Cloud 登录失败，请检查账号密码");
            }

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
            BaoguandanResponse response = objectMapper.readValue(viewResult, BaoguandanResponse.class);
            return convertToJson(response);

        } catch (Exception e) {
            log.error("查询报关单失败，Number: {}", number, e);
            throw new RuntimeException("查询报关单失败: " + e.getMessage());
        }
    }

    /**
     * 转换为JSON字符串
     */
    private String convertToJson(BaoguandanResponse response) {
        BaoguandanResponse.BaoguandanData data = response.getResult().getResult();

        JSONObject result = new JSONObject();

        result.set("报关单号", data.getForaBaoGuanno());
        result.set("合同协议号", data.getForaXieyiNo());
        result.set("实际开船日期", data.getKaichuanDate());

        // 获取运输条款 - 取LocaleId=2025的MultiLanguageText的FDataValue
        if (data.getForaTerms() != null && data.getForaTerms().getMultiLanguageText() != null) {
            String terms = data.getForaTerms().getMultiLanguageText().stream()
                    .filter(item -> LOCALE_ID == item.getLocaleId())
                    .findFirst()
                    .map(BaoguandanResponse.MultiLanguageText::getFDataValue)
                    .orElse(null);
            result.set("运输条款", terms);
        }

        // 获取结算币别 - 取LocaleId=2025的MultiLanguageText的Name
        if (data.getSettleCurrency() != null && data.getSettleCurrency().getMultiLanguageText() != null) {
            String currency = data.getSettleCurrency().getMultiLanguageText().stream()
                    .filter(item -> LOCALE_ID == item.getLocaleId())
                    .findFirst()
                    .map(BaoguandanResponse.CurrencyName::getName)
                    .orElse(null);
            result.set("结算币别", currency);
        }

        result.set("总金额", data.getTotalAmount());

        // 计算分录数量总和
        Double totalQty = 0.0;
        if (data.getFEntity() != null && !data.getFEntity().isEmpty()) {
            totalQty = data.getFEntity().stream()
                    .filter(e -> e.getFQty() != null)
                    .mapToDouble(BaoguandanResponse.Entity::getFQty)
                    .sum();
        } else {
            totalQty = data.getQuantity() != null ? data.getQuantity() : 0.0;
        }
        result.set("数量", totalQty);

        return result.toString();
    }
}
