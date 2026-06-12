package com.ruoyi.integration.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.finance.domain.FeeEstimateExternalRequest;
import com.ruoyi.finance.domain.FeeEstimateExternalResult;
import com.ruoyi.finance.domain.FeeEstimatePackageSummary;
import com.ruoyi.finance.domain.FeeEstimateRouteCandidate;
import com.ruoyi.finance.domain.request.FeeEstimateRequest;
import com.ruoyi.finance.service.FinanceFeeEstimateExternalService;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelPairing;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairingSnapshot;
import com.ruoyi.integration.lingxing.LingxingClientException;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.support.UpstreamSystemConstants;
import com.ruoyi.integration.sync.UpstreamLingxingClientFactory;

/**
 * Lingxing implementation of the finance external fee estimate port.
 */
@Service
public class LingxingFinanceFeeEstimateExternalServiceImpl implements FinanceFeeEstimateExternalService
{
    private static final String ROLE_QUOTE = UpstreamSystemConstants.PAIRING_ROLE_QUOTE;

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Autowired
    private UpstreamLingxingClientFactory lingxingClientFactory;

    @Value("${urili.integration.lingxing.fee-estimate-path:}")
    private String feeEstimatePath;

    @Override
    public FeeEstimateExternalResult estimate(FeeEstimateExternalRequest request)
    {
        String traceId = StringUtils.defaultIfBlank(request == null ? null : request.getTraceId(),
            UUID.randomUUID().toString());
        if (request == null || request.getRouteCandidate() == null)
        {
            return failed(traceId, "ROUTE_CANDIDATE_MISSING", "外部费用试算缺少可执行渠道");
        }
        FeeEstimateRouteCandidate route = request.getRouteCandidate();
        if (StringUtils.isBlank(route.getWarehouseCode()))
        {
            return failed(traceId, "QUOTE_WAREHOUSE_MISSING", "外部费用试算缺少系统仓库");
        }
        if (StringUtils.isBlank(route.getSystemChannelCode()))
        {
            return failed(traceId, "QUOTE_SYSTEM_CHANNEL_MISSING", "外部费用试算缺少系统渠道");
        }

        UpstreamWarehousePairingSnapshot warehousePairing = resolveQuoteWarehousePairing(route.getWarehouseCode());
        if (warehousePairing == null)
        {
            return failed(traceId, "QUOTE_WAREHOUSE_PAIRING_MISSING", "当前仓库未配置报价仓配对");
        }
        UpstreamLogisticsChannelPairing channelPairing = upstreamSystemMapper
            .selectLogisticsChannelPairingBySystemChannel(route.getSystemChannelCode(), ROLE_QUOTE);
        FeeEstimateExternalResult pairingFailure = validateChannelPairing(traceId, warehousePairing, channelPairing);
        if (pairingFailure != null)
        {
            return pairingFailure;
        }
        UpstreamSystemConnection connection = upstreamSystemMapper.selectConnectionByCode(
            warehousePairing.getConnectionCode());
        FeeEstimateExternalResult connectionFailure = validateConnection(traceId, connection);
        if (connectionFailure != null)
        {
            return connectionFailure;
        }
        if (StringUtils.isBlank(feeEstimatePath))
        {
            return failed(traceId, "LINGXING_ESTIMATE_ENDPOINT_UNCONFIGURED",
                "领星费用试算接口路径未配置：URILI_LINGXING_FEE_ESTIMATE_PATH");
        }

        try
        {
            LingxingOpenApiClient client = lingxingClientFactory.createClient(connection);
            Object data = client.estimateFee(feeEstimatePath,
                buildPayload(request, warehousePairing, channelPairing), traceId);
            return normalizeResponse(traceId, request.getCurrencyCode(), data);
        }
        catch (LingxingClientException ex)
        {
            return failed(traceId, ex.getErrorCode(), StringUtils.defaultIfBlank(ex.getMessage(), "领星费用试算失败"));
        }
        catch (RuntimeException ex)
        {
            return failed(traceId, "LINGXING_ESTIMATE_FAILED",
                StringUtils.defaultIfBlank(ex.getMessage(), "领星费用试算失败"));
        }
    }

    private UpstreamWarehousePairingSnapshot resolveQuoteWarehousePairing(String warehouseCode)
    {
        List<UpstreamWarehousePairingSnapshot> pairings = upstreamSystemMapper
            .selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(List.of(warehouseCode));
        return pairings.stream()
            .filter(pairing -> ROLE_QUOTE.equals(pairing.getPairingRole()))
            .findFirst()
            .orElse(null);
    }

    private FeeEstimateExternalResult validateChannelPairing(String traceId,
        UpstreamWarehousePairingSnapshot warehousePairing, UpstreamLogisticsChannelPairing channelPairing)
    {
        if (channelPairing == null)
        {
            return failed(traceId, "QUOTE_CHANNEL_PAIRING_MISSING", "当前系统渠道未配置报价仓渠道配对");
        }
        if (!UpstreamSystemConstants.STATUS_ACTIVE.equals(channelPairing.getStatus()))
        {
            return failed(traceId, "QUOTE_CHANNEL_PAIRING_INACTIVE", "当前系统渠道报价仓渠道配对未启用");
        }
        if (!StringUtils.equals(warehousePairing.getConnectionCode(), channelPairing.getConnectionCode()))
        {
            return failed(traceId, "QUOTE_PAIRING_CONNECTION_MISMATCH", "报价仓仓库配对和渠道配对不属于同一个上游连接");
        }
        String channelWarehouseCode = StringUtils.trimToEmpty(channelPairing.getUpstreamWarehouseCode());
        if (StringUtils.isNotBlank(channelWarehouseCode)
            && !StringUtils.equals(channelWarehouseCode, warehousePairing.getUpstreamWarehouseCode()))
        {
            return failed(traceId, "QUOTE_PAIRING_WAREHOUSE_MISMATCH", "报价仓渠道配对的上游仓库和仓库配对不一致");
        }
        return null;
    }

    private FeeEstimateExternalResult validateConnection(String traceId, UpstreamSystemConnection connection)
    {
        if (connection == null)
        {
            return failed(traceId, "QUOTE_CONNECTION_MISSING", "报价仓上游连接不存在");
        }
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            return failed(traceId, "QUOTE_CONNECTION_DISABLED", "报价仓上游连接未启用");
        }
        if (!UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED.equals(connection.getCredentialStatus()))
        {
            return failed(traceId, "QUOTE_CONNECTION_CREDENTIAL_MISSING", "报价仓上游连接凭证未配置");
        }
        return null;
    }

    private Map<String, Object> buildPayload(FeeEstimateExternalRequest request,
        UpstreamWarehousePairingSnapshot warehousePairing, UpstreamLogisticsChannelPairing channelPairing)
    {
        FeeEstimateRequest original = request.getOriginalRequest();
        FeeEstimatePackageSummary summary = request.getPackageSummary();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("warehouseCode", warehousePairing.getUpstreamWarehouseCode());
        payload.put("whCode", warehousePairing.getUpstreamWarehouseCode());
        payload.put("channelCode", channelPairing.getUpstreamChannelCode());
        payload.put("logisticsChannelCode", channelPairing.getUpstreamChannelCode());
        putIfNotBlank(payload, "currencyCode", request.getCurrencyCode());
        if (original != null)
        {
            putIfNotBlank(payload, "countryCode", original.getDestinationCountryCode());
            putIfNotBlank(payload, "state", original.getDestinationState());
            putIfNotBlank(payload, "province", original.getDestinationState());
            putIfNotBlank(payload, "city", original.getDestinationCity());
            putIfNotBlank(payload, "postCode", original.getDestinationPostalCode());
            putIfNotBlank(payload, "zipCode", original.getDestinationPostalCode());
            putIfNotBlank(payload, "address1", original.getDestinationAddress1());
            putIfNotBlank(payload, "address2", original.getDestinationAddress2());
        }
        if (summary != null)
        {
            Map<String, Object> parcel = new LinkedHashMap<>();
            putIfNotNull(parcel, "length", summary.getEdge3Cm());
            putIfNotNull(parcel, "width", summary.getEdge2Cm());
            putIfNotNull(parcel, "height", summary.getEdge1Cm());
            putIfNotNull(parcel, "weight", summary.getActualWeightKg());
            putIfNotNull(parcel, "chargeableWeight", summary.getChargeableWeightKg());
            parcel.put("quantity", 1);
            payload.putAll(parcel);
            payload.put("packageList", List.of(parcel));
        }
        return payload;
    }

    private FeeEstimateExternalResult normalizeResponse(String traceId, String fallbackCurrencyCode, Object data)
    {
        JSONObject object = unwrapObject(data);
        if (object == null)
        {
            return failed(traceId, "LINGXING_ESTIMATE_RESPONSE_UNSUPPORTED", "领星费用试算响应结构不支持");
        }
        FeeEstimateExternalResult result = new FeeEstimateExternalResult();
        result.setTraceId(traceId);
        result.setCurrencyCode(StringUtils.defaultIfBlank(firstStringDeep(object, "currencyCode", "currency"),
            fallbackCurrencyCode));
        result.setBasicFreightAmount(firstBigDecimalDeep(object, "basicFreightAmount", "freightAmount",
            "baseFreight", "shippingFee"));
        result.setSurchargeAmount(firstBigDecimalDeep(object, "surchargeAmount", "surcharge", "additionalFee"));
        result.setOperationFeeAmount(firstBigDecimalDeep(object, "operationFeeAmount", "operationFee",
            "handlingFee"));
        result.setPackageMaterialFeeAmount(firstBigDecimalDeep(object, "packageMaterialFeeAmount", "materialFee",
            "packagingFee"));
        BigDecimal total = firstBigDecimalDeep(object, "totalAmount", "totalFee", "amount", "fee", "price",
            "freight", "freightFee");
        if (total == null)
        {
            total = sumNullable(result.getBasicFreightAmount(), result.getSurchargeAmount(),
                result.getOperationFeeAmount(), result.getPackageMaterialFeeAmount());
        }
        if (total == null)
        {
            return failed(traceId, "LINGXING_ESTIMATE_AMOUNT_MISSING", "领星费用试算响应未返回可识别金额");
        }
        result.setTotalAmount(total);
        result.setSuccess(true);
        return result;
    }

    private JSONObject unwrapObject(Object data)
    {
        if (data instanceof JSONObject object)
        {
            JSONObject nested = firstObject(object, "fee", "fees", "price", "priceInfo", "amountInfo", "estimate");
            return nested == null ? object : nested;
        }
        if (data instanceof JSONArray array && !array.isEmpty() && array.get(0) instanceof JSONObject object)
        {
            return object;
        }
        return null;
    }

    private JSONObject firstObject(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value instanceof JSONObject nested)
            {
                return nested;
            }
        }
        return null;
    }

    private BigDecimal firstBigDecimalDeep(JSONObject object, String... keys)
    {
        BigDecimal direct = firstBigDecimal(object, keys);
        if (direct != null)
        {
            return direct;
        }
        for (String key : new String[] {"fee", "fees", "price", "priceInfo", "amountInfo", "estimate"})
        {
            Object nestedObject = object.get(key);
            if (!(nestedObject instanceof JSONObject nested))
            {
                continue;
            }
            BigDecimal nestedValue = firstBigDecimal(nested, keys);
            if (nestedValue != null)
            {
                return nestedValue;
            }
        }
        return null;
    }

    private BigDecimal firstBigDecimal(JSONObject object, String... keys)
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
                return new BigDecimal(number.toString());
            }
            if (value instanceof String text && StringUtils.isNotBlank(text))
            {
                try
                {
                    return new BigDecimal(text.trim());
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        return null;
    }

    private String firstStringDeep(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            String value = object.getString(key);
            if (StringUtils.isNotBlank(value))
            {
                return value;
            }
        }
        for (String key : new String[] {"fee", "fees", "price", "priceInfo", "amountInfo", "estimate"})
        {
            Object nestedObject = object.get(key);
            if (!(nestedObject instanceof JSONObject nested))
            {
                continue;
            }
            for (String valueKey : keys)
            {
                String value = nested.getString(valueKey);
                if (StringUtils.isNotBlank(value))
                {
                    return value;
                }
            }
        }
        return null;
    }

    private BigDecimal sumNullable(BigDecimal... values)
    {
        BigDecimal total = null;
        for (BigDecimal value : values)
        {
            if (value == null)
            {
                continue;
            }
            total = total == null ? value : total.add(value);
        }
        return total;
    }

    private void putIfNotBlank(Map<String, Object> payload, String key, String value)
    {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed != null)
        {
            payload.put(key, trimmed);
        }
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value)
    {
        if (value != null)
        {
            payload.put(key, value);
        }
    }

    private FeeEstimateExternalResult failed(String traceId, String errorCode, String errorMessage)
    {
        FeeEstimateExternalResult result = new FeeEstimateExternalResult();
        result.setSuccess(false);
        result.setTraceId(traceId);
        result.setErrorCode(StringUtils.defaultIfBlank(errorCode, "EXTERNAL_ESTIMATE_FAILED"));
        result.setErrorMessage(StringUtils.defaultIfBlank(errorMessage, "外部费用试算失败"));
        return result;
    }
}
