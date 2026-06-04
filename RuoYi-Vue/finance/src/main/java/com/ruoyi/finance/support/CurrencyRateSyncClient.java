package com.ruoyi.finance.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.finance.domain.FinanceCurrencySyncConfig;

/**
 * ShowAPI 银行汇率查询客户端。
 */
@Component
public class CurrencyRateSyncClient
{
    private static final ZoneId RATE_ZONE = ZoneId.of("Asia/Shanghai");

    private static final String SHOWAPI_RATE_URL = "https://route.showapi.com/105-30";

    private static final BigDecimal RATE_UNIT = new BigDecimal("100");

    public CurrencyRateSyncResponse fetchRates(FinanceCurrencySyncConfig config, String appKey)
    {
        if (StringUtils.isBlank(appKey))
        {
            throw new ServiceException("ShowAPI appKey 不能为空");
        }
        int timeoutMs = config.getRequestTimeoutMs() == null ? 10000 : config.getRequestTimeoutMs();
        try
        {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildRequestUrl(appKey)))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("ShowAPI 汇率接口响应异常：" + response.statusCode());
            }
            return parseResponse(response.body());
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("ShowAPI 汇率接口调用失败：" + ex.getMessage());
        }
    }

    public String maskRequestUrl(String appKey)
    {
        return SHOWAPI_RATE_URL + "?appKey=" + maskCredential(appKey);
    }

    private String buildRequestUrl(String appKey)
    {
        return SHOWAPI_RATE_URL + "?appKey=" + URLEncoder.encode(appKey, StandardCharsets.UTF_8);
    }

    private CurrencyRateSyncResponse parseResponse(String body)
    {
        JSONObject root = JSON.parseObject(body);
        Integer responseCode = root.getInteger("showapi_res_code");
        if (responseCode == null || responseCode != 0)
        {
            throw new ServiceException("ShowAPI 汇率接口失败：" + StringUtils.defaultIfBlank(root.getString("showapi_res_error"), String.valueOf(responseCode)));
        }
        JSONObject responseBody = root.getJSONObject("showapi_res_body");
        if (responseBody == null)
        {
            throw new ServiceException("ShowAPI 汇率接口未返回响应体");
        }
        Integer retCode = responseBody.getInteger("ret_code");
        if (retCode == null || retCode != 0)
        {
            throw new ServiceException("ShowAPI 汇率查询失败：" + StringUtils.defaultIfBlank(responseBody.getString("remark"), String.valueOf(retCode)));
        }
        JSONArray list = responseBody.getJSONArray("list");
        if (list == null || list.isEmpty())
        {
            throw new ServiceException("ShowAPI 汇率接口未返回汇率列表");
        }

        List<CurrencyRateCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            JSONObject item = list.getJSONObject(i);
            CurrencyRateCandidate candidate = parseCandidate(item);
            if (candidate != null)
            {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty())
        {
            throw new ServiceException("ShowAPI 汇率接口未返回可用现汇卖出价");
        }

        CurrencyRateSyncResponse response = new CurrencyRateSyncResponse();
        response.setBaseCurrencyCode("CNY");
        response.setCandidates(candidates);
        response.setResponseSummary(limit(body, 2000));
        return response;
    }

    private CurrencyRateCandidate parseCandidate(JSONObject item)
    {
        String currencyCode = StringUtils.trimToEmpty(item.getString("code")).toUpperCase();
        String cashSellingRate = StringUtils.trimToEmpty(item.getString("hui_out"));
        if (StringUtils.isBlank(currencyCode) || StringUtils.isBlank(cashSellingRate))
        {
            return null;
        }
        CurrencyRateCandidate candidate = new CurrencyRateCandidate();
        candidate.setCurrencyCode(currencyCode);
        candidate.setOfficialRate(parseOfficialRate(currencyCode, cashSellingRate));
        candidate.setOfficialRateTime(parseRateTime(item.getString("day"), item.getString("time")));
        return candidate;
    }

    private BigDecimal parseOfficialRate(String currencyCode, String cashSellingRate)
    {
        try
        {
            return new BigDecimal(cashSellingRate).divide(RATE_UNIT, 10, RoundingMode.HALF_UP);
        }
        catch (NumberFormatException ex)
        {
            throw new ServiceException("ShowAPI 现汇卖出价格式异常：" + currencyCode);
        }
    }

    private java.util.Date parseRateTime(String day, String time)
    {
        try
        {
            LocalDate date = LocalDate.parse(StringUtils.trimToEmpty(day));
            LocalTime localTime = LocalTime.parse(StringUtils.defaultIfBlank(time, "00:00:00").trim());
            return java.util.Date.from(LocalDateTime.of(date, localTime).atZone(RATE_ZONE).toInstant());
        }
        catch (Exception ex)
        {
            throw new ServiceException("ShowAPI 汇率时间格式异常：" + day + " " + time);
        }
    }

    private String maskCredential(String credential)
    {
        if (StringUtils.isBlank(credential))
        {
            return "";
        }
        String trimmed = credential.trim();
        if (trimmed.length() <= 8)
        {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String limit(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
