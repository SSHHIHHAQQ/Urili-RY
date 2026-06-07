package com.ruoyi.integration.lingxing;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.integration.support.UpstreamMaskUtils;
import com.ruoyi.integration.support.UpstreamSystemConstants;

/**
 * 领星 WMS OpenAPI 客户端。
 */
public class LingxingOpenApiClient
{
    private static final String DEFAULT_BASE_URL = "https://api.xlwms.com/openapi/v1";

    private static final int DEFAULT_TIMEOUT_MS = 10000;

    private static final int DEFAULT_MAX_RETRIES = 2;

    private static final int DEFAULT_RETRY_DELAY_MS = 250;

    private static final ExecutorService HTTP_EXECUTOR = Executors.newFixedThreadPool(8, new ThreadFactory()
    {
        private final AtomicInteger sequence = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable)
        {
            Thread thread = new Thread(runnable, "lingxing-http-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    });

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
        .executor(HTTP_EXECUTOR)
        .build();

    private final LingxingCredentials credentials;

    private final LingxingRequestLogger requestLogger;

    private final HttpClient httpClient;

    private final String baseUrl;

    public LingxingOpenApiClient(LingxingCredentials credentials, LingxingRequestLogger requestLogger)
    {
        this(credentials, requestLogger, DEFAULT_BASE_URL);
    }

    public LingxingOpenApiClient(LingxingCredentials credentials, LingxingRequestLogger requestLogger, String baseUrl)
    {
        this.credentials = credentials;
        this.requestLogger = requestLogger;
        this.baseUrl = StringUtils.defaultIfBlank(baseUrl, DEFAULT_BASE_URL).replaceAll("/$", "");
        this.httpClient = SHARED_HTTP_CLIENT;
    }

    public List<Object> listWorkOrderTypes(String traceId)
    {
        Object data = post("/workOrder/types", Collections.emptyMap(), UpstreamSystemConstants.OP_AUTH_CHECK, traceId);
        if (!(data instanceof JSONArray array))
        {
            throw new LingxingClientException("LINGXING_RESPONSE_ERROR", "领星工单类型响应不是数组", false);
        }
        return new ArrayList<>(array);
    }

    public void checkWarehouseAccess(String traceId)
    {
        Object data = post("/warehouse/options", Collections.emptyMap(), UpstreamSystemConstants.OP_AUTH_CHECK, traceId);
        if (!(data instanceof JSONArray))
        {
            throw new LingxingClientException("LINGXING_RESPONSE_ERROR", "领星仓库授权校验响应不是数组", false);
        }
    }

    public List<LingxingWarehouse> listWarehouses(String traceId)
    {
        Object data = post("/warehouse/options", Collections.emptyMap(), UpstreamSystemConstants.OP_WAREHOUSE_SYNC, traceId);
        if (!(data instanceof JSONArray array))
        {
            throw new LingxingClientException("LINGXING_RESPONSE_ERROR", "领星仓库响应不是数组", false);
        }
        List<LingxingWarehouse> warehouses = new ArrayList<>();
        for (Object item : array)
        {
            if (item instanceof JSONObject object)
            {
                String code = object.getString("whCode");
                String name = object.getString("whNameCn");
                if (StringUtils.isNotBlank(code) && StringUtils.isNotBlank(name))
                {
                    LingxingWarehouse warehouse = new LingxingWarehouse();
                    warehouse.setWarehouseCode(code.trim());
                    warehouse.setWarehouseName(name.trim());
                    warehouse.setCountryCode(StringUtils.defaultString(object.getString("countryCode")).trim());
                    String sourcePayload = JSON.toJSONString(object);
                    warehouse.setSourcePayloadJson(sourcePayload);
                    warehouse.setSourcePayloadHash(sha256Hex(sourcePayload));
                    warehouses.add(warehouse);
                }
            }
        }
        return warehouses;
    }

    public List<LingxingLogisticsChannel> listLogisticsChannels(String warehouseCode, String traceId)
    {
        Map<String, Object> dataRequest = new LinkedHashMap<>();
        dataRequest.put("whCode", warehouseCode);
        Object data = post("/logistics/channel/list", dataRequest, UpstreamSystemConstants.OP_LOGISTICS_CHANNEL_SYNC, traceId);
        if (!(data instanceof JSONArray array))
        {
            throw new LingxingClientException("LINGXING_RESPONSE_ERROR", "领星物流渠道响应不是数组", false);
        }
        List<LingxingLogisticsChannel> channels = new ArrayList<>();
        for (Object item : array)
        {
            if (item instanceof JSONObject object)
            {
                String code = object.getString("channelCode");
                String name = object.getString("channelName");
                if (StringUtils.isNotBlank(code) && StringUtils.isNotBlank(name))
                {
                    LingxingLogisticsChannel channel = new LingxingLogisticsChannel();
                    channel.setWarehouseCode(warehouseCode);
                    channel.setChannelCode(code.trim());
                    channel.setChannelName(name.trim());
                    String sourcePayload = JSON.toJSONString(object);
                    channel.setSourcePayloadJson(sourcePayload);
                    channel.setSourcePayloadHash(sha256Hex(sourcePayload));
                    channels.add(channel);
                }
            }
        }
        return channels;
    }

    public LingxingProductPage listProductSkuPage(int current, int size, String traceId)
    {
        int safeCurrent = Math.max(1, current);
        int safeSize = Math.max(1, Math.min(100, size));
        Map<String, Object> dataRequest = new LinkedHashMap<>();
        dataRequest.put("page", safeCurrent);
        dataRequest.put("pageSize", safeSize);
        return listProductSkuPage(dataRequest, safeCurrent, safeSize, UpstreamSystemConstants.OP_SKU_SYNC, traceId);
    }

    public LingxingProductPage listProductSkuPageBySkuList(List<String> skuList, String traceId)
    {
        return listProductSkuPageBySkuList(skuList, UpstreamSystemConstants.OP_SKU_DIMENSION_SYNC, traceId);
    }

    public LingxingProductPage listProductSkuPageBySkuList(List<String> skuList, String operation, String traceId)
    {
        List<String> safeSkuList = skuList == null ? Collections.emptyList() : skuList.stream()
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .limit(100)
            .collect(Collectors.toList());
        if (safeSkuList.isEmpty())
        {
            LingxingProductPage empty = new LingxingProductPage();
            empty.setCurrent(1);
            empty.setSize(0);
            empty.setTotal(0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        Map<String, Object> dataRequest = new LinkedHashMap<>();
        dataRequest.put("page", 1);
        dataRequest.put("pageSize", safeSkuList.size());
        dataRequest.put("skuList", safeSkuList);
        return listProductSkuPage(dataRequest, 1, safeSkuList.size(),
            StringUtils.defaultIfBlank(operation, UpstreamSystemConstants.OP_SKU_DIMENSION_SYNC), traceId);
    }

    public LingxingInventoryProductPage listInventoryProductPage(int current, int size, String traceId)
    {
        return listInventoryProductPage(current, size, null, null, traceId);
    }

    public LingxingInventoryProductPage listInventoryProductPage(int current, int size, String startTime,
        String endTime, String traceId)
    {
        int safeCurrent = Math.max(1, current);
        int safeSize = Math.max(1, Math.min(100, size));
        Map<String, Object> dataRequest = new LinkedHashMap<>();
        dataRequest.put("page", safeCurrent);
        dataRequest.put("pageSize", safeSize);
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime))
        {
            dataRequest.put("timeType", "operateTime");
            dataRequest.put("startTime", startTime);
            dataRequest.put("endTime", endTime);
        }
        Object data = post("/integratedInventory/pageOpen", dataRequest, UpstreamSystemConstants.OP_INVENTORY_SYNC, traceId);
        if (!(data instanceof JSONObject object))
        {
            throw new LingxingClientException("LINGXING_RESPONSE_ERROR", "领星库存响应不是对象", false);
        }
        LingxingInventoryProductPage page = new LingxingInventoryProductPage();
        page.setCurrent(firstInt(object, safeCurrent, "page", "current"));
        page.setSize(firstInt(object, safeSize, "pageSize", "size"));
        JSONArray records = firstArray(object, "records", "list", "rows");
        page.setTotal(firstInt(object, records.size(), "total", "totalCount", "count"));
        List<LingxingInventoryProductStock> stocks = new ArrayList<>();
        for (Object item : records)
        {
            if (item instanceof JSONObject row)
            {
                parseIntegratedInventoryStocks(row, stocks);
            }
        }
        page.setRecords(stocks);
        return page;
    }

    private void parseIntegratedInventoryStocks(JSONObject row, List<LingxingInventoryProductStock> stocks)
    {
        appendIntegratedInventoryStock(stocks, row, "COMPREHENSIVE", firstLong(row, "totalAmount"),
            combinedStockDetail(row), firstLong(row, "boxTotalAmount"), firstLong(row, "productTotalAmount"));
        appendIntegratedInventoryStock(stocks, row, "PRODUCT", firstLong(row, "productTotalAmount"),
            row.getJSONObject("productStockDtl"), null, firstLong(row, "productTotalAmount"));
        appendIntegratedInventoryStock(stocks, row, "BOX", firstLong(row, "boxTotalAmount"),
            row.getJSONObject("boxStockDtl"), firstLong(row, "boxTotalAmount"), null);
        appendIntegratedInventoryStock(stocks, row, "RETURN", firstLong(row, "fbaReturnTotalAmount"),
            row.getJSONObject("fbaReturnStockDtl"), null, null);
    }

    private JSONObject combinedStockDetail(JSONObject row)
    {
        JSONObject detail = new JSONObject();
        detail.put("availableAmount", sumNullableOrNull(
            stockDetailLong(row.getJSONObject("productStockDtl"), "availableAmount"),
            stockDetailLong(row.getJSONObject("boxStockDtl"), "availableAmount"),
            stockDetailLong(row.getJSONObject("fbaReturnStockDtl"), "availableAmount")));
        detail.put("lockAmount", sumNullableOrNull(
            stockDetailLong(row.getJSONObject("productStockDtl"), "lockAmount"),
            stockDetailLong(row.getJSONObject("boxStockDtl"), "lockAmount"),
            stockDetailLong(row.getJSONObject("fbaReturnStockDtl"), "lockAmount")));
        detail.put("transportAmount", sumNullableOrNull(
            stockDetailLong(row.getJSONObject("productStockDtl"), "transportAmount"),
            stockDetailLong(row.getJSONObject("boxStockDtl"), "transportAmount"),
            stockDetailLong(row.getJSONObject("fbaReturnStockDtl"), "transportAmount")));
        return detail;
    }

    private void appendIntegratedInventoryStock(List<LingxingInventoryProductStock> stocks, JSONObject row, String scope,
        Long totalQuantity, JSONObject detail, Long boxedQuantity, Long unboxedQuantity)
    {
        String warehouseCode = firstString(row, "whCode", "warehouseCode", "warehouse_code", "wmsWarehouseCode");
        String sku = firstString(row, "sku", "masterSku", "productSku", "productSkuCode", "productCode");
        if (StringUtils.isBlank(warehouseCode) || StringUtils.isBlank(sku))
        {
            return;
        }
        if (totalQuantity == null && !hasStockDetail(detail))
        {
            return;
        }
        LingxingInventoryProductStock stock = new LingxingInventoryProductStock();
        stock.setWarehouseCode(warehouseCode);
        stock.setWarehouseName(firstString(row, "whNameCn", "warehouseName", "warehouse_name", "whName"));
        stock.setSku(sku);
        stock.setProductName(firstString(row, "productName", "productNameCn", "skuName", "name"));
        stock.setInventoryScope(scope);
        stock.setInventoryAttribute(firstString(row, "stockType"));
        stock.setBatchNo("");
        stock.setLocationCode("");
        stock.setAvailableQuantity(stockDetailLong(detail, "availableAmount"));
        stock.setLockedQuantity(stockDetailLong(detail, "lockAmount"));
        stock.setInTransitQuantity(stockDetailLong(detail, "transportAmount"));
        stock.setBoxedQuantity(boxedQuantity);
        stock.setUnboxedQuantity(unboxedQuantity);
        stock.setTotalQuantity(totalQuantity);
        if (stock.getTotalQuantity() == null)
        {
            stock.setTotalQuantity(sumNullable(stock.getAvailableQuantity(), stock.getLockedQuantity(),
                stock.getInTransitQuantity()));
        }
        String sourcePayload = JSON.toJSONString(row);
        stock.setSourcePayloadJson(sourcePayload);
        stock.setSourcePayloadHash(sha256Hex(sourcePayload));
        stocks.add(stock);
    }

    private boolean hasStockDetail(JSONObject detail)
    {
        return detail != null && (stockDetailLong(detail, "availableAmount") != null
            || stockDetailLong(detail, "lockAmount") != null
            || stockDetailLong(detail, "transportAmount") != null);
    }

    private Long stockDetailLong(JSONObject detail, String key)
    {
        return detail == null ? null : firstLong(detail, key);
    }

    private LingxingProductPage listProductSkuPage(Map<String, Object> dataRequest, int safeCurrent, int safeSize,
        String operation, String traceId)
    {
        Object data = post("/product/pagelist", dataRequest, operation, traceId);
        if (!(data instanceof JSONObject object))
        {
            throw new LingxingClientException("LINGXING_RESPONSE_ERROR", "领星SKU响应不是对象", false);
        }
        LingxingProductPage page = new LingxingProductPage();
        page.setCurrent(firstInt(object, safeCurrent, "page", "current"));
        page.setSize(firstInt(object, safeSize, "pageSize", "size"));
        JSONArray records = firstArray(object, "records", "list", "rows");
        page.setTotal(firstInt(object, records.size(), "total", "totalCount", "count"));
        List<LingxingProductSku> skus = new ArrayList<>();
        for (Object item : records)
        {
            if (item instanceof JSONObject row)
            {
                String sku = firstString(row, "sku", "productSku", "productSkuCode", "productCode");
                String productName = firstString(row, "productName", "productNameCn", "productAliasName", "name", "skuName");
                if (StringUtils.isNotBlank(sku) && StringUtils.isNotBlank(productName))
                {
                    LingxingProductSku productSku = new LingxingProductSku();
                    productSku.setSku(sku.trim());
                    productSku.setProductName(productName.trim());
                    productSku.setProductAliasName(firstString(row, "productAliasName"));
                    productSku.setApproveStatus(firstString(row, "approveStatus"));
                    productSku.setProductType(firstInteger(row, "type"));
                    productSku.setProductDescription(firstString(row, "productDescription"));
                    productSku.setImageUrl(firstString(row, "imageUrl"));
                    productSku.setMainCode(firstString(row, "mainCode"));
                    productSku.setOtherCode(firstString(row, "otherCode"));
                    productSku.setFnsku(firstString(row, "fnsku"));
                    productSku.setCountryOfOriginName(firstString(row, "countryOfOriginName"));
                    productSku.setCurrencyCode(firstString(row, "currencyCode"));
                    productSku.setCustomhouseCode(firstString(row, "customhouseCode"));
                    productSku.setDangerousCargo(firstInteger(row, "dangerousCargo"));
                    productSku.setDeclareNameCn(firstString(row, "declareNameCn"));
                    productSku.setDeclareNameEn(firstString(row, "declareNameEn"));
                    productSku.setDeclarePrice(firstBigDecimal(row, "declarePrice"));
                    productSku.setHeight(firstBigDecimal(row, "height"));
                    productSku.setHeightBs(firstBigDecimal(row, "heightBs"));
                    productSku.setLength(firstBigDecimal(row, "length"));
                    productSku.setLengthBs(firstBigDecimal(row, "lengthBs"));
                    productSku.setWeight(firstBigDecimal(row, "weight"));
                    productSku.setWeightBs(firstBigDecimal(row, "weightBs"));
                    productSku.setWidth(firstBigDecimal(row, "width"));
                    productSku.setWidthBs(firstBigDecimal(row, "widthBs"));
                    productSku.setWmsHeight(firstBigDecimal(row, "wmsHeight"));
                    productSku.setWmsHeightBs(firstBigDecimal(row, "wmsHeightBs"));
                    productSku.setWmsLength(firstBigDecimal(row, "wmsLength"));
                    productSku.setWmsLengthBs(firstBigDecimal(row, "wmsLengthBs"));
                    productSku.setWmsWeight(firstBigDecimal(row, "wmsWeight"));
                    productSku.setWmsWeightBs(firstBigDecimal(row, "wmsWeightBs"));
                    productSku.setWmsWidth(firstBigDecimal(row, "wmsWidth"));
                    productSku.setWmsWidthBs(firstBigDecimal(row, "wmsWidthBs"));
                    productSku.setCat1Name(firstString(row, "cat1Name"));
                    productSku.setCat2Name(firstString(row, "cat2Name"));
                    productSku.setCat3Name(firstString(row, "cat3Name"));
                    productSku.setPlatformSkuInfoJson(arrayJson(row, "platformSkuInfoList"));
                    productSku.setBrazilTaxInfoJson(arrayJson(row, "brazilTaxInfoList"));
                    String sourcePayload = JSON.toJSONString(row);
                    productSku.setSourcePayloadJson(sourcePayload);
                    productSku.setSourcePayloadHash(sha256Hex(sourcePayload));
                    skus.add(productSku);
                }
            }
        }
        page.setRecords(skus);
        return page;
    }

    private Object post(String path, Object data, String operation, String traceId)
    {
        return postToBase(baseUrl, path, data, operation, traceId);
    }

    private Object postToBase(String targetBaseUrl, String path, Object data, String operation, String traceId)
    {
        String safeTraceId = StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString());
        String reqTime = String.valueOf(System.currentTimeMillis() / 1000L);
        String authcode = sign(data, reqTime);
        String endpoint = targetBaseUrl + path;
        String requestUrl = endpoint + "?authcode=" + URLEncoder.encode(authcode, StandardCharsets.UTF_8);
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("appKey", credentials.getAppKey());
        requestBody.put("reqTime", reqTime);
        requestBody.put("data", data);
        String requestJson = JSON.toJSONString(requestBody);
        String requestLog = "{\"appKey\":\"" + UpstreamMaskUtils.mask(credentials.getAppKey()) + "\",\"authcode\":\""
            + UpstreamMaskUtils.mask(authcode) + "\",\"reqTime\":\"" + reqTime + "\",\"data\":" + JSON.toJSONString(data) + "}";

        for (int attempt = 1; attempt <= DEFAULT_MAX_RETRIES + 1; attempt++)
        {
            long started = System.currentTimeMillis();
            LingxingRequestLogEntry log = new LingxingRequestLogEntry();
            log.setTraceId(safeTraceId);
            log.setOperation(operation);
            log.setEndpoint(endpoint);
            log.setRequestTime(new Date(started));
            log.setRequestPayloadRedacted(requestLog);
            try
            {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("X-Trace-Id", safeTraceId)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                long finished = System.currentTimeMillis();
                log.setResponseTime(new Date(finished));
                log.setDurationMs(finished - started);
                log.setResponsePayloadRedacted(UpstreamMaskUtils.redactJson(response.body()));

                if (response.statusCode() == 429 || response.statusCode() >= 500)
                {
                    throw new LingxingClientException("LINGXING_HTTP_ERROR", "领星接口HTTP " + response.statusCode(), true);
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                {
                    throw new LingxingClientException("LINGXING_HTTP_ERROR", "领星接口HTTP " + response.statusCode(), false);
                }
                JSONObject payload = JSON.parseObject(response.body());
                if (payload.getIntValue("code") != 200)
                {
                    String message = StringUtils.defaultIfBlank(payload.getString("message"), payload.getString("msg"));
                    throw new LingxingClientException(String.valueOf(payload.get("code")),
                        StringUtils.defaultIfBlank(message, "领星接口返回业务错误"), false);
                }
                log.setStatus("SUCCESS");
                writeLog(log);
                return payload.get("data");
            }
            catch (LingxingClientException ex)
            {
                finishFailureLog(log, ex);
                writeLog(log);
                if (!ex.isRetryable() || attempt > DEFAULT_MAX_RETRIES)
                {
                    throw ex;
                }
                sleepBeforeRetry();
            }
            catch (Exception ex)
            {
                LingxingClientException clientException = new LingxingClientException("LINGXING_NETWORK_ERROR",
                    StringUtils.defaultIfBlank(ex.getMessage(), "领星网络请求失败"), true);
                finishFailureLog(log, clientException);
                writeLog(log);
                if (attempt > DEFAULT_MAX_RETRIES)
                {
                    throw clientException;
                }
                sleepBeforeRetry();
            }
        }
        throw new LingxingClientException("LINGXING_RETRY_ERROR", "领星请求重试失败", false);
    }

    private void finishFailureLog(LingxingRequestLogEntry log, LingxingClientException ex)
    {
        long finished = System.currentTimeMillis();
        log.setResponseTime(new Date(finished));
        log.setDurationMs(Math.max(0, finished - log.getRequestTime().getTime()));
        log.setExternalErrorCode(ex.getErrorCode());
        log.setExternalErrorMessage(ex.getMessage());
        log.setStatus("FAILURE");
    }

    private void writeLog(LingxingRequestLogEntry log)
    {
        if (requestLogger != null)
        {
            requestLogger.log(log);
        }
    }

    private void sleepBeforeRetry()
    {
        try
        {
            Thread.sleep(DEFAULT_RETRY_DELAY_MS);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    private String sign(Object data, String reqTime)
    {
        try
        {
            String payload = credentials.getAppKey() + normalizeForSigning(data) + reqTime;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(credentials.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes)
            {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
        catch (Exception ex)
        {
            throw new LingxingClientException("LINGXING_SIGN_ERROR", "领星请求签名失败", false);
        }
    }

    private String normalizeForSigning(Object value)
    {
        if (value == null)
        {
            return "null";
        }
        if (value instanceof Map<?, ?> map)
        {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet())
            {
                sorted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Object> entry : sorted.entrySet())
            {
                parts.add("\"" + entry.getKey() + "\":" + normalizeForSigning(entry.getValue()));
            }
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof Iterable<?> iterable)
        {
            List<String> parts = new ArrayList<>();
            for (Object item : iterable)
            {
                parts.add(normalizeForSigning(item));
            }
            return "[" + String.join(",", parts) + "]";
        }
        return JSON.toJSONString(value);
    }

    private static int firstInt(JSONObject object, int defaultValue, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value instanceof Number number)
            {
                return Math.max(0, number.intValue());
            }
            if (value instanceof String string && StringUtils.isNotBlank(string))
            {
                try
                {
                    return Math.max(0, Integer.parseInt(string));
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        return defaultValue;
    }

    private static JSONArray firstArray(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            JSONArray array = object.getJSONArray(key);
            if (array != null)
            {
                return array;
            }
        }
        return new JSONArray();
    }

    private static String firstString(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            String value = object.getString(key);
            if (StringUtils.isNotBlank(value))
            {
                return value;
            }
        }
        return "";
    }

    private static Integer firstInteger(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value instanceof Number number)
            {
                return number.intValue();
            }
            if (value instanceof String string && StringUtils.isNotBlank(string))
            {
                try
                {
                    return Integer.parseInt(string);
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        return null;
    }

    private static BigDecimal firstBigDecimal(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value instanceof BigDecimal decimal)
            {
                return decimal;
            }
            if (value instanceof Number number)
            {
                return BigDecimal.valueOf(number.doubleValue());
            }
            if (value instanceof String string && StringUtils.isNotBlank(string))
            {
                try
                {
                    return new BigDecimal(string.trim());
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        return null;
    }

    private static Long firstLong(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value instanceof Number number)
            {
                return number.longValue();
            }
            if (value instanceof String string && StringUtils.isNotBlank(string))
            {
                try
                {
                    return Long.parseLong(string.trim());
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        return null;
    }

    private static long sumNullable(Long... values)
    {
        long sum = 0L;
        for (Long value : values)
        {
            if (value != null)
            {
                sum += value;
            }
        }
        return sum;
    }

    private static Long sumNullableOrNull(Long... values)
    {
        long sum = 0L;
        boolean hasValue = false;
        for (Long value : values)
        {
            if (value != null)
            {
                sum += value;
                hasValue = true;
            }
        }
        return hasValue ? sum : null;
    }

    private static String arrayJson(JSONObject object, String key)
    {
        JSONArray array = object.getJSONArray(key);
        return array == null ? "[]" : JSON.toJSONString(array);
    }

    private static String sha256Hex(String value)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes)
            {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
        catch (Exception ex)
        {
            throw new LingxingClientException("LINGXING_HASH_ERROR", "领星产品快照哈希计算失败", false);
        }
    }
}
