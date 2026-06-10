package com.ruoyi.logistics.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.logistics.agg56.Agg56AuthResult;
import com.ruoyi.logistics.agg56.Agg56Credentials;
import com.ruoyi.logistics.agg56.Agg56OpenApiClient;
import com.ruoyi.logistics.agg56.Agg56RequestLogEntry;
import com.ruoyi.logistics.agg56.Agg56ShippingMethod;
import com.ruoyi.logistics.domain.LogisticsAgg56Connection;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelCandidate;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelMapping;
import com.ruoyi.logistics.domain.LogisticsCarrierConnection;
import com.ruoyi.logistics.domain.LogisticsCarrierRequestLog;
import com.ruoyi.logistics.domain.LogisticsLabelOrder;
import com.ruoyi.logistics.domain.LogisticsLabelPackage;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.request.Agg56CredentialRequest;
import com.ruoyi.logistics.domain.request.LogisticsAddressRequest;
import com.ruoyi.logistics.domain.request.LogisticsBoxRequest;
import com.ruoyi.logistics.domain.request.LogisticsChannelMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsConnectionRequest;
import com.ruoyi.logistics.domain.request.LogisticsCreateLabelRequest;
import com.ruoyi.logistics.domain.request.LogisticsLabelActionRequest;
import com.ruoyi.logistics.domain.request.LogisticsQuoteRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelRequest;
import com.ruoyi.logistics.domain.response.LogisticsQuoteResponse;
import com.ruoyi.logistics.mapper.LogisticsCarrierMapper;
import com.ruoyi.logistics.service.ILogisticsCarrierService;
import com.ruoyi.logistics.support.LogisticsConstants;
import com.ruoyi.logistics.support.LogisticsMaskUtils;
import com.ruoyi.system.service.support.SecretCipherSupport;

/**
 * 物流商管理服务实现。
 */
@Service
public class LogisticsCarrierServiceImpl implements ILogisticsCarrierService
{
    @Autowired
    private LogisticsCarrierMapper logisticsCarrierMapper;

    @Autowired
    private SecretCipherSupport secretCipherSupport;

    @Override
    public List<LogisticsCarrierConnection> selectConnectionList(LogisticsCarrierConnection query)
    {
        List<LogisticsCarrierConnection> list = logisticsCarrierMapper.selectConnectionList(query);
        for (LogisticsCarrierConnection connection : list)
        {
            fillProviderExtension(connection);
        }
        return list;
    }

    @Override
    public LogisticsCarrierConnection selectConnectionByAccountId(Long carrierAccountId)
    {
        LogisticsCarrierConnection connection = logisticsCarrierMapper.selectConnectionByAccountId(
            requireAccountId(carrierAccountId));
        if (connection == null)
        {
            throw new ServiceException("物流商账号不存在");
        }
        fillProviderExtension(connection);
        return connection;
    }

    @Override
    @Transactional
    public int insertConnection(LogisticsConnectionRequest request)
    {
        String providerKind = normalizeProviderKind(request.getProviderKind());
        String carrierName = trimRequired(request.getCarrierName(), "物流商名称不能为空");
        String connectionCode = generateConnectionCode(providerKind, carrierName);
        if (logisticsCarrierMapper.selectConnectionByCode(connectionCode) != null)
        {
            throw new ServiceException("物流商账号内部编号已存在，请重试");
        }

        LogisticsCarrierConnection connection = new LogisticsCarrierConnection();
        connection.setConnectionCode(connectionCode);
        connection.setProviderKind(providerKind);
        connection.setCarrierName(carrierName);
        connection.setApiBaseUrl(defaultApiBaseUrl(providerKind, request.getApiBaseUrl()));
        connection.setStatus(LogisticsConstants.STATUS_ENABLED);
        connection.setCredentialStatus(LogisticsConstants.CREDENTIAL_UNCONFIGURED);
        Integer maxOrder = logisticsCarrierMapper.selectMaxConnectionDisplayOrder();
        connection.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        connection.setCreateBy(SecurityUtils.getUsername());
        connection.setRemark(trimOptional(request.getRemark()));
        int rows = logisticsCarrierMapper.insertConnection(connection);
        if (connection.getCarrierAccountId() == null)
        {
            LogisticsCarrierConnection saved = logisticsCarrierMapper.selectConnectionByCode(connectionCode);
            connection.setCarrierAccountId(saved == null ? null : saved.getCarrierAccountId());
        }
        if (LogisticsConstants.PROVIDER_AGG56.equals(providerKind))
        {
            updateAgg56Credentials(connection.getCarrierAccountId(), buildAgg56CredentialRequest(request));
        }
        return rows;
    }

    @Override
    public int updateConnectionInfo(Long carrierAccountId, LogisticsConnectionRequest request)
    {
        LogisticsCarrierConnection existing = selectConnectionByAccountId(carrierAccountId);
        LogisticsCarrierConnection connection = new LogisticsCarrierConnection();
        connection.setCarrierAccountId(existing.getCarrierAccountId());
        connection.setCarrierName(trimRequired(request.getCarrierName(), "物流商名称不能为空"));
        connection.setApiBaseUrl(defaultApiBaseUrl(existing.getProviderKind(), request.getApiBaseUrl()));
        connection.setUpdateBy(SecurityUtils.getUsername());
        connection.setRemark(trimOptional(request.getRemark()));
        return logisticsCarrierMapper.updateConnectionInfo(connection);
    }

    @Override
    @Transactional
    public int updateAgg56Credentials(Long carrierAccountId, Agg56CredentialRequest request)
    {
        LogisticsCarrierConnection connection = requireProviderConnection(carrierAccountId, LogisticsConstants.PROVIDER_AGG56);
        String appToken = trimRequired(request.getAppToken(), "app_token不能为空");
        String appKey = trimRequired(request.getAppKey(), "app_key不能为空");
        Agg56AuthResult authResult = createAgg56Client(connection).getToken(new Agg56Credentials(appToken, appKey),
            UUID.randomUUID().toString());

        LogisticsAgg56Connection agg56 = new LogisticsAgg56Connection();
        agg56.setCarrierAccountId(connection.getCarrierAccountId());
        agg56.setConnectionCode(connection.getConnectionCode());
        agg56.setAppTokenMask(LogisticsMaskUtils.mask(appToken));
        agg56.setAppKeyMask(LogisticsMaskUtils.mask(appKey));
        agg56.setAppTokenCiphertext(secretCipherSupport.encrypt(appToken));
        agg56.setAppKeyCiphertext(secretCipherSupport.encrypt(appKey));
        agg56.setCredentialKeyId(secretCipherSupport.getEncryptionKeyId());
        fillAgg56AuthInfo(agg56, authResult);
        logisticsCarrierMapper.upsertAgg56Connection(agg56);
        return logisticsCarrierMapper.updateConnectionAuthorizeSummary(connection.getCarrierAccountId(),
            LogisticsConstants.CREDENTIAL_CONFIGURED, SecurityUtils.getUsername());
    }

    @Override
    public int updateConnectionStatus(Long carrierAccountId, String status)
    {
        selectConnectionByAccountId(carrierAccountId);
        String normalizedStatus = trimRequired(status, "状态不能为空").toUpperCase(Locale.ROOT);
        if (!LogisticsConstants.STATUS_ENABLED.equals(normalizedStatus)
            && !LogisticsConstants.STATUS_DISABLED.equals(normalizedStatus))
        {
            throw new ServiceException("接入状态只能是 ENABLED 或 DISABLED");
        }
        return logisticsCarrierMapper.updateConnectionStatus(carrierAccountId, normalizedStatus, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int updateConnectionOrder(List<Long> carrierAccountIds)
    {
        if (carrierAccountIds == null || carrierAccountIds.isEmpty())
        {
            throw new ServiceException("排序物流商账号不能为空");
        }
        Set<Long> seen = new LinkedHashSet<>();
        int rows = 0;
        int displayOrder = 1;
        for (Long rawId : carrierAccountIds)
        {
            Long carrierAccountId = requireAccountId(rawId);
            if (!seen.add(carrierAccountId))
            {
                throw new ServiceException("排序物流商账号不能重复：" + carrierAccountId);
            }
            selectConnectionByAccountId(carrierAccountId);
            rows += logisticsCarrierMapper.updateConnectionDisplayOrder(carrierAccountId, displayOrder++,
                SecurityUtils.getUsername());
        }
        return rows;
    }

    @Override
    @Transactional
    public int authorize(Long carrierAccountId)
    {
        LogisticsCarrierConnection connection = requireProviderConnection(carrierAccountId, LogisticsConstants.PROVIDER_AGG56);
        LogisticsAgg56Connection agg56 = requireAgg56Credentials(connection);
        try
        {
            Agg56AuthResult authResult = createAgg56Client(connection).getToken(decryptAgg56Credentials(agg56),
                UUID.randomUUID().toString());
            fillAgg56AuthInfo(agg56, authResult);
            logisticsCarrierMapper.upsertAgg56Connection(agg56);
            return logisticsCarrierMapper.updateConnectionAuthorizeSummary(connection.getCarrierAccountId(),
                LogisticsConstants.CREDENTIAL_CONFIGURED, SecurityUtils.getUsername());
        }
        catch (RuntimeException ex)
        {
            logisticsCarrierMapper.updateConnectionAuthorizeSummary(connection.getCarrierAccountId(),
                LogisticsConstants.CREDENTIAL_INVALID, SecurityUtils.getUsername());
            throw toServiceException(ex);
        }
    }

    @Override
    @Transactional
    public int syncChannels(Long carrierAccountId)
    {
        LogisticsCarrierConnection connection = requireProviderConnection(carrierAccountId, LogisticsConstants.PROVIDER_AGG56);
        String accessToken = requestAgg56AccessToken(connection);
        String syncBatchId = UUID.randomUUID().toString();
        List<Agg56ShippingMethod> methods = createAgg56Client(connection).listShippingMethods(accessToken, syncBatchId);
        int rows = 0;
        for (Agg56ShippingMethod method : methods)
        {
            LogisticsCarrierChannelCandidate item = new LogisticsCarrierChannelCandidate();
            item.setCarrierAccountId(connection.getCarrierAccountId());
            item.setConnectionCode(connection.getConnectionCode());
            item.setExternalChannelCode(method.getCode());
            item.setExternalChannelName(method.getName());
            item.setRawFinalCarrierText(method.getName());
            item.setStatus(LogisticsConstants.ITEM_ACTIVE);
            item.setSyncBatchId(syncBatchId);
            item.setSourcePayloadJson(method.getSourcePayloadJson());
            item.setSourcePayloadHash(method.getSourcePayloadHash());
            rows += logisticsCarrierMapper.upsertChannelCandidate(item);
        }
        logisticsCarrierMapper.markMissingChannelCandidates(connection.getCarrierAccountId(), syncBatchId);
        logisticsCarrierMapper.updateConnectionChannelSyncTime(connection.getCarrierAccountId(), SecurityUtils.getUsername());
        return rows;
    }

    @Override
    public List<LogisticsCarrierChannelCandidate> selectChannelCandidateList(Long carrierAccountId, String status)
    {
        selectConnectionByAccountId(carrierAccountId);
        return logisticsCarrierMapper.selectChannelCandidateList(carrierAccountId, trimOptional(status));
    }

    @Override
    public List<LogisticsSystemChannel> selectSystemChannelList(LogisticsSystemChannel query)
    {
        return logisticsCarrierMapper.selectSystemChannelList(query);
    }

    @Override
    public int insertSystemChannel(LogisticsSystemChannelRequest request)
    {
        String systemChannelCode = trimRequired(request.getSystemChannelCode(), "系统渠道代码不能为空");
        if (logisticsCarrierMapper.selectSystemChannelByCode(systemChannelCode) != null)
        {
            throw new ServiceException("系统渠道代码已存在");
        }
        LogisticsSystemChannel channel = buildSystemChannel(systemChannelCode, request);
        channel.setStatus(LogisticsConstants.STATUS_ENABLED);
        channel.setDisplayOrder(0);
        channel.setCreateBy(SecurityUtils.getUsername());
        return logisticsCarrierMapper.insertSystemChannel(channel);
    }

    @Override
    public int updateSystemChannel(String systemChannelCode, LogisticsSystemChannelRequest request, String status)
    {
        String normalizedCode = trimRequired(systemChannelCode, "系统渠道代码不能为空");
        if (logisticsCarrierMapper.selectSystemChannelByCode(normalizedCode) == null)
        {
            throw new ServiceException("系统渠道不存在");
        }
        LogisticsSystemChannel channel = buildSystemChannel(normalizedCode, request);
        channel.setStatus(normalizeOptionalStatus(status));
        channel.setUpdateBy(SecurityUtils.getUsername());
        return logisticsCarrierMapper.updateSystemChannel(channel);
    }

    @Override
    public List<LogisticsCarrierChannelMapping> selectChannelMappingList(Long carrierAccountId)
    {
        selectConnectionByAccountId(carrierAccountId);
        return logisticsCarrierMapper.selectChannelMappingList(carrierAccountId);
    }

    @Override
    @Transactional
    public int insertChannelMapping(Long carrierAccountId, LogisticsChannelMappingRequest request)
    {
        LogisticsCarrierConnection connection = selectConnectionByAccountId(carrierAccountId);
        String externalChannelCode = trimRequired(request.getExternalChannelCode(), "物流商渠道代码不能为空");
        LogisticsCarrierChannelCandidate candidate = logisticsCarrierMapper.selectChannelCandidate(carrierAccountId,
            externalChannelCode);
        if (candidate == null)
        {
            throw new ServiceException("物流商渠道不存在，请先同步物流产品");
        }
        if (!LogisticsConstants.ITEM_ACTIVE.equals(candidate.getStatus()))
        {
            throw new ServiceException("物流商渠道不是可映射状态");
        }
        String systemChannelCode = trimRequired(request.getSystemChannelCode(), "系统渠道代码不能为空");
        LogisticsSystemChannel systemChannel = logisticsCarrierMapper.selectSystemChannelByCode(systemChannelCode);
        if (systemChannel == null)
        {
            throw new ServiceException("系统渠道不存在");
        }

        LogisticsCarrierChannelMapping mapping = new LogisticsCarrierChannelMapping();
        mapping.setCarrierAccountId(connection.getCarrierAccountId());
        mapping.setConnectionCode(connection.getConnectionCode());
        mapping.setExternalChannelCode(externalChannelCode);
        mapping.setExternalChannelNameSnapshot(candidate.getExternalChannelName());
        mapping.setSystemChannelCode(systemChannelCode);
        mapping.setSystemChannelNameSnapshot(systemChannel.getSystemChannelName());
        mapping.setStandardCarrierCode(trimRequired(request.getStandardCarrierCode(), "标准最终承运商不能为空"));
        mapping.setStatus(LogisticsConstants.STATUS_ENABLED);
        mapping.setCreateBy(SecurityUtils.getUsername());
        mapping.setRemark(trimOptional(request.getRemark()));
        try
        {
            return logisticsCarrierMapper.insertChannelMapping(mapping);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("渠道映射已存在");
        }
    }

    @Override
    public int deleteChannelMapping(Long carrierAccountId, Long mappingId)
    {
        selectConnectionByAccountId(carrierAccountId);
        if (mappingId == null)
        {
            throw new ServiceException("映射ID不能为空");
        }
        return logisticsCarrierMapper.deleteChannelMapping(carrierAccountId, mappingId);
    }

    @Override
    public LogisticsQuoteResponse quote(LogisticsQuoteRequest request)
    {
        Routing routing = resolveRouting(request.getCarrierAccountId(), request.getSystemChannelCode());
        Map<String, Object> payload = buildAgg56ShipmentPayload(request, routing.mapping.getExternalChannelCode(), null);
        JSONObject object = createAgg56Client(routing.connection).rates(requestAgg56AccessToken(routing.connection),
            payload, UUID.randomUUID().toString(), null);

        LogisticsQuoteResponse response = new LogisticsQuoteResponse();
        response.setCarrierAccountId(routing.connection.getCarrierAccountId());
        response.setProviderKind(routing.connection.getProviderKind());
        response.setSystemChannelCode(routing.mapping.getSystemChannelCode());
        response.setExternalChannelCode(routing.mapping.getExternalChannelCode());
        response.setProviderResultJson(JSON.toJSONString(object.get("result")));
        return response;
    }

    @Override
    @Transactional
    public LogisticsLabelOrder createLabel(LogisticsCreateLabelRequest request)
    {
        String businessOrderNo = trimRequired(request.getBusinessOrderNo(), "业务单号不能为空");
        LogisticsLabelOrder existing = logisticsCarrierMapper.selectLabelOrderByBusinessNo(businessOrderNo);
        if (existing != null && !LogisticsConstants.LABEL_STATUS_FAILED.equals(existing.getStatus()))
        {
            return fillLabelPackages(existing);
        }

        Routing routing = resolveRouting(request.getCarrierAccountId(), request.getSystemChannelCode());
        Map<String, Object> payload = buildAgg56ShipmentPayload(request, routing.mapping.getExternalChannelCode(),
            businessOrderNo);
        JSONObject object;
        try
        {
            object = createAgg56Client(routing.connection).createOrder(requestAgg56AccessToken(routing.connection),
                payload, UUID.randomUUID().toString(), businessOrderNo);
        }
        catch (RuntimeException ex)
        {
            insertFailedLabelOrderIfAbsent(request, routing, payload, ex);
            throw toServiceException(ex);
        }

        LogisticsLabelOrder order = existing == null ? new LogisticsLabelOrder() : existing;
        order.setBusinessOrderNo(businessOrderNo);
        order.setCarrierAccountId(routing.connection.getCarrierAccountId());
        order.setConnectionCode(routing.connection.getConnectionCode());
        order.setProviderKind(routing.connection.getProviderKind());
        order.setSystemChannelCode(routing.mapping.getSystemChannelCode());
        order.setExternalChannelCode(routing.mapping.getExternalChannelCode());
        order.setCreatePayloadJson(JSON.toJSONString(payload));
        order.setProviderResultJson(JSON.toJSONString(object.get("result")));
        order.setCreateBy(SecurityUtils.getUsername());
        order.setUpdateBy(SecurityUtils.getUsername());
        order.setRemark(trimOptional(request.getRemark()));

        if (existing == null)
        {
            logisticsCarrierMapper.insertLabelOrder(order);
        }
        applyProviderLabelResult(order, object);
        return fillLabelPackages(order);
    }

    @Override
    @Transactional
    public LogisticsLabelOrder getLabel(LogisticsLabelActionRequest request)
    {
        LogisticsLabelOrder order = resolveLabelOrder(request);
        LogisticsCarrierConnection connection = requireProviderConnection(order.getCarrierAccountId(),
            LogisticsConstants.PROVIDER_AGG56);
        String providerOrderNo = trimRequired(order.getProviderOrderNo(), "物流商订单号为空，不能获取面单");
        JSONObject object = createAgg56Client(connection).getLabel(requestAgg56AccessToken(connection),
            providerOrderNo, UUID.randomUUID().toString(), order.getBusinessOrderNo());
        applyProviderLabelResult(order, object);
        return fillLabelPackages(order);
    }

    @Override
    @Transactional
    public LogisticsLabelOrder cancelLabel(LogisticsLabelActionRequest request)
    {
        LogisticsLabelOrder order = resolveLabelOrder(request);
        LogisticsCarrierConnection connection = requireProviderConnection(order.getCarrierAccountId(),
            LogisticsConstants.PROVIDER_AGG56);
        String providerOrderNo = trimRequired(order.getProviderOrderNo(), "物流商订单号为空，不能取消面单");
        try
        {
            createAgg56Client(connection).cancelOrder(requestAgg56AccessToken(connection), providerOrderNo,
                UUID.randomUUID().toString(), order.getBusinessOrderNo());
            logisticsCarrierMapper.updateLabelOrderStatus(order.getLabelOrderId(),
                LogisticsConstants.LABEL_STATUS_CANCELLED, SecurityUtils.getUsername());
            order.setStatus(LogisticsConstants.LABEL_STATUS_CANCELLED);
        }
        catch (RuntimeException ex)
        {
            logisticsCarrierMapper.updateLabelOrderStatus(order.getLabelOrderId(),
                LogisticsConstants.LABEL_STATUS_CANCEL_FAILED, SecurityUtils.getUsername());
            throw toServiceException(ex);
        }
        return fillLabelPackages(order);
    }

    @Override
    public List<LogisticsLabelOrder> selectLabelOrderList(LogisticsLabelOrder query)
    {
        List<LogisticsLabelOrder> list = logisticsCarrierMapper.selectLabelOrderList(query);
        for (LogisticsLabelOrder order : list)
        {
            fillLabelPackages(order);
        }
        return list;
    }

    @Override
    public List<LogisticsCarrierRequestLog> selectRequestLogList(Long carrierAccountId)
    {
        selectConnectionByAccountId(carrierAccountId);
        return logisticsCarrierMapper.selectRequestLogList(carrierAccountId);
    }

    private void fillProviderExtension(LogisticsCarrierConnection connection)
    {
        if (connection != null && LogisticsConstants.PROVIDER_AGG56.equals(connection.getProviderKind()))
        {
            connection.setAgg56(logisticsCarrierMapper.selectAgg56ConnectionByAccountId(connection.getCarrierAccountId()));
        }
    }

    private LogisticsCarrierConnection requireProviderConnection(Long carrierAccountId, String providerKind)
    {
        LogisticsCarrierConnection connection = selectConnectionByAccountId(carrierAccountId);
        if (!providerKind.equals(connection.getProviderKind()))
        {
            throw new ServiceException("该物流商账号不是 " + providerKind);
        }
        return connection;
    }

    private LogisticsAgg56Connection requireAgg56Credentials(LogisticsCarrierConnection connection)
    {
        LogisticsAgg56Connection agg56 = logisticsCarrierMapper.selectAgg56ConnectionByAccountId(connection.getCarrierAccountId());
        if (agg56 == null || StringUtils.isBlank(agg56.getAppTokenCiphertext())
            || StringUtils.isBlank(agg56.getAppKeyCiphertext()))
        {
            throw new ServiceException("AGG56凭证未配置");
        }
        return agg56;
    }

    private Agg56Credentials decryptAgg56Credentials(LogisticsAgg56Connection agg56)
    {
        return new Agg56Credentials(secretCipherSupport.decrypt(agg56.getAppTokenCiphertext()),
            secretCipherSupport.decrypt(agg56.getAppKeyCiphertext()));
    }

    private String requestAgg56AccessToken(LogisticsCarrierConnection connection)
    {
        LogisticsAgg56Connection agg56 = requireAgg56Credentials(connection);
        return createAgg56Client(connection)
            .getToken(decryptAgg56Credentials(agg56), UUID.randomUUID().toString())
            .getAccessToken();
    }

    private Agg56OpenApiClient createAgg56Client(LogisticsCarrierConnection connection)
    {
        return new Agg56OpenApiClient(connection.getApiBaseUrl(), entry -> insertAgg56RequestLog(connection, entry));
    }

    private void insertAgg56RequestLog(LogisticsCarrierConnection connection, Agg56RequestLogEntry entry)
    {
        LogisticsCarrierRequestLog log = new LogisticsCarrierRequestLog();
        log.setCarrierAccountId(connection.getCarrierAccountId());
        log.setConnectionCode(connection.getConnectionCode());
        log.setProviderKind(connection.getProviderKind());
        log.setTraceId(entry.getTraceId());
        log.setOperation(entry.getOperation());
        log.setEndpoint(entry.getEndpoint());
        log.setHttpMethod(entry.getHttpMethod());
        log.setBusinessOrderNo(entry.getBusinessOrderNo());
        log.setProviderOrderNo(entry.getProviderOrderNo());
        log.setRequestTime(entry.getRequestTime());
        log.setResponseTime(entry.getResponseTime());
        log.setDurationMs(entry.getDurationMs());
        log.setHttpStatus(entry.getHttpStatus());
        log.setProviderCode(entry.getProviderCode());
        log.setProviderMessage(entry.getProviderMessage());
        log.setStatus(entry.getStatus());
        log.setRequestPayloadRedacted(entry.getRequestPayloadRedacted());
        log.setResponsePayloadRedacted(entry.getResponsePayloadRedacted());
        logisticsCarrierMapper.insertRequestLog(log);
    }

    private void fillAgg56AuthInfo(LogisticsAgg56Connection agg56, Agg56AuthResult authResult)
    {
        agg56.setAgg56UserId(authResult.getUserId());
        agg56.setAgg56UserAccountMask(LogisticsMaskUtils.maskEmail(authResult.getUserAccount()));
        agg56.setAgg56CustomerCode(authResult.getCustomerCode());
    }

    private Routing resolveRouting(Long carrierAccountId, String systemChannelCode)
    {
        LogisticsCarrierConnection connection = requireProviderConnection(carrierAccountId, LogisticsConstants.PROVIDER_AGG56);
        if (!LogisticsConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("物流商账号已停用");
        }
        LogisticsCarrierChannelMapping mapping = logisticsCarrierMapper.selectActiveChannelMapping(
            connection.getCarrierAccountId(), trimRequired(systemChannelCode, "系统渠道不能为空"));
        if (mapping == null)
        {
            throw new ServiceException("系统渠道未映射到该物流商渠道");
        }
        return new Routing(connection, mapping);
    }

    private Map<String, Object> buildAgg56ShipmentPayload(LogisticsQuoteRequest request, String externalChannelCode,
        String businessOrderNo)
    {
        LogisticsAddressRequest recipient = request.getRecipientAddress();
        if (recipient == null)
        {
            throw new ServiceException("收件地址不能为空");
        }
        List<LogisticsBoxRequest> boxes = request.getBoxes();
        if (boxes == null || boxes.isEmpty())
        {
            throw new ServiceException("包裹不能为空");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(businessOrderNo))
        {
            payload.put("reference_no", businessOrderNo);
        }
        payload.put("sm_code", externalChannelCode);
        if (request instanceof LogisticsCreateLabelRequest createRequest)
        {
            payload.put("remark", trimOptional(createRequest.getRemark()));
            payload.put("po_code", trimOptional(createRequest.getPoCode()));
            payload.put("vat_code", trimOptional(createRequest.getVatCode()));
            payload.put("warehouse_code", trimOptional(createRequest.getWarehouseCode()));
            payload.put("delivery_time", trimOptional(createRequest.getDeliveryTime()));
        }
        payload.put("parcel_quantity", boxes.size());
        payload.put("parcel_declared_value", request.getDeclaredValue());
        payload.put("oa_firstname", trimRequired(recipient.getName(), "收件人姓名不能为空"));
        payload.put("oa_company", trimOptional(recipient.getCompany()));
        payload.put("oa_street_address1", trimRequired(recipient.getAddress1(), "收件地址1不能为空"));
        payload.put("oa_street_address2", trimOptional(recipient.getAddress2()));
        payload.put("oa_postcode", trimRequired(recipient.getPostcode(), "收件邮编不能为空"));
        payload.put("oa_state", trimRequired(recipient.getState(), "收件州/省不能为空"));
        payload.put("oa_city", trimRequired(recipient.getCity(), "收件城市不能为空"));
        payload.put("oa_country", StringUtils.defaultIfBlank(trimOptional(recipient.getCountry()), "US"));
        payload.put("oa_doorplate", trimOptional(recipient.getDoorplate()));
        payload.put("oa_telphone", trimRequired(recipient.getTelephone(), "收件电话不能为空"));
        payload.put("signature_service", trimOptional(request.getSignatureService()));
        payload.put("weight_unit_type", request.getWeightUnitType());
        payload.put("box_list", buildAgg56BoxList(boxes));

        LogisticsAddressRequest shipper = request.getShipperAddress();
        if (shipper != null)
        {
            payload.put("shipper_address", buildAgg56ShipperAddress(shipper));
        }
        return payload;
    }

    private List<Map<String, Object>> buildAgg56BoxList(List<LogisticsBoxRequest> boxes)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (LogisticsBoxRequest box : boxes)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("box_length", box.getLength());
            item.put("box_width", box.getWidth());
            item.put("box_height", box.getHeight());
            item.put("box_actual_weight", box.getActualWeight());
            item.put("box_remark", trimOptional(box.getRemark()));
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> buildAgg56ShipperAddress(LogisticsAddressRequest shipper)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("shipper_name", trimOptional(shipper.getName()));
        item.put("shipper_company", trimOptional(shipper.getCompany()));
        item.put("shipper_telphone", trimOptional(shipper.getTelephone()));
        item.put("shipper_country", StringUtils.defaultIfBlank(trimOptional(shipper.getCountry()), "US"));
        item.put("shipper_state_province", trimOptional(shipper.getState()));
        item.put("shipper_city", trimOptional(shipper.getCity()));
        item.put("shipper_address1", trimOptional(shipper.getAddress1()));
        item.put("shipper_address2", trimOptional(shipper.getAddress2()));
        item.put("shipper_postal_code", trimOptional(shipper.getPostcode()));
        item.put("shipper_doorplate", trimOptional(shipper.getDoorplate()));
        item.put("shipper_code", trimOptional(shipper.getShipperCode()));
        return item;
    }

    private void applyProviderLabelResult(LogisticsLabelOrder order, JSONObject object)
    {
        JSONObject result = object.getJSONObject("result");
        if (result == null)
        {
            order.setStatus(LogisticsConstants.LABEL_STATUS_FAILED);
            order.setProviderResultJson(JSON.toJSONString(object.get("result")));
            logisticsCarrierMapper.updateLabelOrderFromProvider(order);
            return;
        }

        order.setProviderOrderNo(StringUtils.defaultIfBlank(result.getString("order_code"), order.getProviderOrderNo()));
        order.setZoneCode(result.getString("zone"));
        order.setChargeWeight(result.getString("charge_weight"));
        order.setLogisticsError(result.getString("logistics_err"));
        order.setProviderResultJson(JSON.toJSONString(result));
        JSONArray labels = result.getJSONArray("labels");
        order.setLabelFileTypes(extractLabelFileTypes(labels));
        Integer providerCode = object.getInteger("code");
        if (StringUtils.isNotBlank(order.getLogisticsError()))
        {
            order.setStatus(LogisticsConstants.LABEL_STATUS_FAILED);
        }
        else if (labels != null && !labels.isEmpty())
        {
            order.setStatus(LogisticsConstants.LABEL_STATUS_CREATED);
        }
        else if (providerCode != null && providerCode.intValue() == 202)
        {
            order.setStatus(LogisticsConstants.LABEL_STATUS_PENDING_LABEL);
        }
        else
        {
            order.setStatus(LogisticsConstants.LABEL_STATUS_PENDING_LABEL);
        }
        order.setUpdateBy(SecurityUtils.getUsername());
        logisticsCarrierMapper.updateLabelOrderFromProvider(order);
        replaceLabelPackages(order.getLabelOrderId(), labels);
    }

    private String extractLabelFileTypes(JSONArray labels)
    {
        if (labels == null || labels.isEmpty())
        {
            return "";
        }
        Set<String> fileTypes = new LinkedHashSet<>();
        for (Object item : labels)
        {
            if (item instanceof JSONObject row)
            {
                String fileType = StringUtils.trimToEmpty(row.getString("file_type")).toUpperCase(Locale.ROOT);
                if (StringUtils.isNotBlank(fileType))
                {
                    fileTypes.add(fileType);
                }
            }
        }
        return String.join(",", fileTypes);
    }

    private void replaceLabelPackages(Long labelOrderId, JSONArray labels)
    {
        logisticsCarrierMapper.deleteLabelPackages(labelOrderId);
        if (labels == null)
        {
            return;
        }
        for (Object item : labels)
        {
            if (item instanceof JSONObject row)
            {
                LogisticsLabelPackage labelPackage = new LogisticsLabelPackage();
                labelPackage.setLabelOrderId(labelOrderId);
                labelPackage.setProviderPackageNo(StringUtils.defaultString(row.getString("box_code")));
                labelPackage.setTrackingNumber(StringUtils.defaultString(row.getString("tracking_number")));
                labelPackage.setLabelUrl(StringUtils.defaultString(row.getString("label_url")));
                labelPackage.setFileType(StringUtils.trimToEmpty(row.getString("file_type")).toUpperCase(Locale.ROOT));
                labelPackage.setStatus(LogisticsConstants.LABEL_STATUS_CREATED);
                labelPackage.setSourcePayloadJson(JSON.toJSONString(row));
                logisticsCarrierMapper.insertLabelPackage(labelPackage);
            }
        }
    }

    private void insertFailedLabelOrderIfAbsent(LogisticsCreateLabelRequest request, Routing routing,
        Map<String, Object> payload, RuntimeException ex)
    {
        if (logisticsCarrierMapper.selectLabelOrderByBusinessNo(request.getBusinessOrderNo()) != null)
        {
            return;
        }
        LogisticsLabelOrder order = new LogisticsLabelOrder();
        order.setBusinessOrderNo(request.getBusinessOrderNo());
        order.setCarrierAccountId(routing.connection.getCarrierAccountId());
        order.setConnectionCode(routing.connection.getConnectionCode());
        order.setProviderKind(routing.connection.getProviderKind());
        order.setSystemChannelCode(routing.mapping.getSystemChannelCode());
        order.setExternalChannelCode(routing.mapping.getExternalChannelCode());
        order.setStatus(LogisticsConstants.LABEL_STATUS_FAILED);
        order.setCreatePayloadJson(JSON.toJSONString(payload));
        order.setProviderResultJson("");
        order.setLogisticsError(StringUtils.left(ex.getMessage(), 500));
        order.setCreateBy(SecurityUtils.getUsername());
        order.setRemark(trimOptional(request.getRemark()));
        try
        {
            logisticsCarrierMapper.insertLabelOrder(order);
            logisticsCarrierMapper.updateLabelOrderFromProvider(order);
        }
        catch (DuplicateKeyException ignored)
        {
            // 并发请求已写入同一业务单号时，保留全局唯一约束结果。
        }
    }

    private LogisticsLabelOrder resolveLabelOrder(LogisticsLabelActionRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("面单请求不能为空");
        }
        String businessOrderNo = trimOptional(request.getBusinessOrderNo());
        if (StringUtils.isNotBlank(businessOrderNo))
        {
            LogisticsLabelOrder order = logisticsCarrierMapper.selectLabelOrderByBusinessNo(businessOrderNo);
            if (order != null)
            {
                return order;
            }
        }
        Long carrierAccountId = request.getCarrierAccountId();
        String providerOrderNo = trimOptional(request.getProviderOrderNo());
        if (carrierAccountId != null && StringUtils.isNotBlank(providerOrderNo))
        {
            LogisticsLabelOrder order = logisticsCarrierMapper.selectLabelOrderByProviderOrderNo(carrierAccountId,
                providerOrderNo);
            if (order != null)
            {
                return order;
            }
        }
        throw new ServiceException("面单订单不存在");
    }

    private LogisticsLabelOrder fillLabelPackages(LogisticsLabelOrder order)
    {
        order.setPackages(logisticsCarrierMapper.selectLabelPackages(order.getLabelOrderId()));
        return order;
    }

    private LogisticsSystemChannel buildSystemChannel(String systemChannelCode, LogisticsSystemChannelRequest request)
    {
        LogisticsSystemChannel channel = new LogisticsSystemChannel();
        channel.setSystemChannelCode(systemChannelCode);
        channel.setSystemChannelName(trimRequired(request.getSystemChannelName(), "系统渠道名称不能为空"));
        channel.setStandardCarrierCode(trimRequired(request.getStandardCarrierCode(), "标准最终承运商不能为空"));
        channel.setRemark(trimOptional(request.getRemark()));
        return channel;
    }

    private String normalizeProviderKind(String value)
    {
        String providerKind = trimRequired(value, "物流商系统不能为空").toUpperCase(Locale.ROOT);
        if (LogisticsConstants.PROVIDER_AGG56.equals(providerKind))
        {
            return providerKind;
        }
        throw new ServiceException("暂不支持的物流商系统：" + providerKind);
    }

    private String normalizeOptionalStatus(String value)
    {
        String status = StringUtils.defaultIfBlank(trimOptional(value), LogisticsConstants.STATUS_ENABLED).toUpperCase(Locale.ROOT);
        if (!LogisticsConstants.STATUS_ENABLED.equals(status) && !LogisticsConstants.STATUS_DISABLED.equals(status))
        {
            throw new ServiceException("状态只能是 ENABLED 或 DISABLED");
        }
        return status;
    }

    private String defaultApiBaseUrl(String providerKind, String apiBaseUrl)
    {
        String baseUrl = trimOptional(apiBaseUrl);
        if (StringUtils.isNotBlank(baseUrl))
        {
            return baseUrl.replaceAll("/$", "");
        }
        if (LogisticsConstants.PROVIDER_AGG56.equals(providerKind))
        {
            return "https://www.agg56.com";
        }
        return baseUrl;
    }

    private Agg56CredentialRequest buildAgg56CredentialRequest(LogisticsConnectionRequest request)
    {
        Agg56CredentialRequest credentialRequest = new Agg56CredentialRequest();
        credentialRequest.setAppToken(trimRequired(request.getAppToken(), "APP Token不能为空"));
        credentialRequest.setAppKey(trimRequired(request.getAppKey(), "APP Key不能为空"));
        return credentialRequest;
    }

    private String generateConnectionCode(String providerKind, String connectionName)
    {
        String seed = StringUtils.defaultIfBlank(connectionName, providerKind)
            .trim()
            .replaceAll("\\s+", "-")
            .replaceAll("[^0-9A-Za-z\\u4e00-\\u9fa5-]", "");
        if (seed.length() > 24)
        {
            seed = seed.substring(0, 24);
        }
        return providerKind + "-" + StringUtils.defaultIfBlank(seed, "CARRIER") + "-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private Long requireAccountId(Long carrierAccountId)
    {
        if (carrierAccountId == null || carrierAccountId.longValue() <= 0)
        {
            throw new ServiceException("物流商账号不能为空");
        }
        return carrierAccountId;
    }

    private ServiceException toServiceException(RuntimeException ex)
    {
        if (ex instanceof ServiceException serviceException)
        {
            return serviceException;
        }
        return new ServiceException(StringUtils.defaultIfBlank(ex.getMessage(), "物流商接口调用失败"));
    }

    private String trimRequired(String value, String message)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(trimmed))
        {
            throw new ServiceException(message);
        }
        return trimmed;
    }

    private String trimOptional(String value)
    {
        return StringUtils.trimToEmpty(value);
    }

    private record Routing(LogisticsCarrierConnection connection, LogisticsCarrierChannelMapping mapping)
    {
    }
}
