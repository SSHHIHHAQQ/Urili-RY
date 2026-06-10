package com.ruoyi.logistics.agg56;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.logistics.support.LogisticsConstants;
import com.ruoyi.logistics.support.LogisticsHashUtils;
import com.ruoyi.logistics.support.LogisticsMaskUtils;

/**
 * AGG56 OpenAPI 客户端。
 */
public class Agg56OpenApiClient
{
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
        .build();

    private final String baseUrl;

    private final Agg56RequestLogger requestLogger;

    public Agg56OpenApiClient(String baseUrl, Agg56RequestLogger requestLogger)
    {
        this.baseUrl = StringUtils.defaultIfBlank(baseUrl, "https://www.agg56.com").replaceAll("/$", "");
        this.requestLogger = requestLogger;
    }

    public Agg56AuthResult getToken(Agg56Credentials credentials, String traceId)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_token", credentials.getAppToken());
        body.put("app_key", credentials.getAppKey());
        JSONObject object = post("/api/svc/getToken", null, body, LogisticsConstants.OP_AUTH,
            StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString()), null, null);
        JSONObject result = object.getJSONObject("result");
        if (result == null || StringUtils.isBlank(result.getString("access_token")))
        {
            throw new Agg56ClientException("AGG56 授权响应缺少 access_token");
        }
        JSONObject userInfo = result.getJSONObject("user_info");
        Agg56AuthResult authResult = new Agg56AuthResult();
        authResult.setAccessToken(result.getString("access_token"));
        if (userInfo != null)
        {
            authResult.setUserId(StringUtils.defaultString(userInfo.getString("u_id")));
            authResult.setUserAccount(userInfo.getString("u_account"));
            authResult.setCustomerCode(userInfo.getString("u_customer_code"));
        }
        return authResult;
    }

    public List<Agg56ShippingMethod> listShippingMethods(String accessToken, String traceId)
    {
        JSONObject object = post("/api/svc/getShippingMethod", accessToken, new LinkedHashMap<>(),
            LogisticsConstants.OP_CHANNEL_SYNC, StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString()),
            null, null);
        JSONArray result = object.getJSONArray("result");
        if (result == null)
        {
            throw new Agg56ClientException("AGG56 物流产品响应不是数组");
        }
        List<Agg56ShippingMethod> methods = new ArrayList<>();
        for (Object item : result)
        {
            if (item instanceof JSONObject row)
            {
                String code = StringUtils.trimToEmpty(row.getString("sm_code"));
                String name = StringUtils.trimToEmpty(row.getString("sm_name"));
                if (StringUtils.isNotBlank(code) && StringUtils.isNotBlank(name))
                {
                    Agg56ShippingMethod method = new Agg56ShippingMethod();
                    method.setCode(code);
                    method.setName(name);
                    String sourcePayload = JSON.toJSONString(row);
                    method.setSourcePayloadJson(sourcePayload);
                    method.setSourcePayloadHash(LogisticsHashUtils.sha256Hex(sourcePayload));
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    public JSONObject rates(String accessToken, Map<String, Object> body, String traceId, String businessOrderNo)
    {
        return post("/api/svc/rates", accessToken, body, LogisticsConstants.OP_QUOTE,
            StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString()), businessOrderNo, null);
    }

    public JSONObject createOrder(String accessToken, Map<String, Object> body, String traceId, String businessOrderNo)
    {
        return post("/api/svc/createOrder", accessToken, body, LogisticsConstants.OP_CREATE_LABEL,
            StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString()), businessOrderNo, null);
    }

    public JSONObject getLabel(String accessToken, String orderCode, String traceId, String businessOrderNo)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_code", orderCode);
        return post("/api/svc/getLabel", accessToken, body, LogisticsConstants.OP_GET_LABEL,
            StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString()), businessOrderNo, orderCode);
    }

    public JSONObject cancelOrder(String accessToken, String orderCode, String traceId, String businessOrderNo)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_code", orderCode);
        return post("/api/svc/cancelOrder", accessToken, body, LogisticsConstants.OP_CANCEL_LABEL,
            StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString()), businessOrderNo, orderCode);
    }

    private JSONObject post(String path, String accessToken, Map<String, Object> body, String operation, String traceId,
        String businessOrderNo, String providerOrderNo)
    {
        String payload = JSON.toJSONString(body == null ? new LinkedHashMap<>() : body);
        Date requestTime = new Date();
        long started = System.currentTimeMillis();
        Agg56RequestLogEntry log = new Agg56RequestLogEntry();
        log.setTraceId(traceId);
        log.setOperation(operation);
        log.setEndpoint(path);
        log.setHttpMethod("POST");
        log.setRequestTime(requestTime);
        log.setBusinessOrderNo(businessOrderNo);
        log.setProviderOrderNo(providerOrderNo);
        log.setRequestPayloadRedacted(LogisticsMaskUtils.redactJson(payload));
        Integer httpStatus = null;
        String responseBody = "";
        try
        {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
            if (StringUtils.isNotBlank(accessToken))
            {
                builder.header("Authorization", accessToken);
            }
            HttpResponse<String> response = SHARED_HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            httpStatus = response.statusCode();
            responseBody = StringUtils.defaultString(response.body());
            JSONObject object = JSON.parseObject(responseBody);
            log.setProviderCode(StringUtils.defaultString(object.getString("code")));
            log.setProviderMessage(object.getString("msg"));
            if (httpStatus < 200 || httpStatus >= 300)
            {
                throw new Agg56ClientException("AGG56 HTTP 调用失败：" + httpStatus);
            }
            Integer code = object.getInteger("code");
            boolean pendingLabel = LogisticsConstants.OP_GET_LABEL.equals(operation)
                && code != null && code.intValue() == 202;
            if ((code == null || code.intValue() != 200) && !pendingLabel)
            {
                throw new Agg56ClientException(StringUtils.defaultIfBlank(object.getString("msg"), "AGG56 业务调用失败"));
            }
            log.setStatus("SUCCESS");
            return object;
        }
        catch (Agg56ClientException ex)
        {
            log.setStatus("FAILED");
            throw ex;
        }
        catch (Exception ex)
        {
            log.setStatus("FAILED");
            throw new Agg56ClientException("AGG56 接口调用失败：" + ex.getMessage(), ex);
        }
        finally
        {
            Date responseTime = new Date();
            log.setResponseTime(responseTime);
            log.setDurationMs(System.currentTimeMillis() - started);
            log.setHttpStatus(httpStatus);
            log.setResponsePayloadRedacted(LogisticsMaskUtils.redactJson(responseBody));
            if (requestLogger != null)
            {
                requestLogger.log(log);
            }
        }
    }
}
