package com.ruoyi.integration.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.integration.domain.SourceProductItem;
import com.ruoyi.integration.domain.SourceWarehouseStockItem;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelPairing;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelSyncItem;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSkuPairing;
import com.ruoyi.integration.domain.UpstreamSkuPairingAuditEvent;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSkuSyncState;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.query.SourceProductQuery;
import com.ruoyi.integration.domain.query.SourceWarehouseStockQuery;
import com.ruoyi.integration.domain.request.LogisticsChannelPairingRequest;
import com.ruoyi.integration.domain.request.SkuPairingRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionInfoRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionRequest;
import com.ruoyi.integration.domain.request.UpstreamCredentialRequest;
import com.ruoyi.integration.domain.request.WarehousePairingRequest;
import com.ruoyi.integration.domain.response.SourceProductGroupDetail;
import com.ruoyi.integration.domain.response.UpstreamSyncResult;
import com.ruoyi.integration.lingxing.LingxingClientException;
import com.ruoyi.integration.lingxing.LingxingCredentials;
import com.ruoyi.integration.lingxing.LingxingInventoryProductPage;
import com.ruoyi.integration.lingxing.LingxingInventoryProductStock;
import com.ruoyi.integration.lingxing.LingxingLogisticsChannel;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.lingxing.LingxingProductPage;
import com.ruoyi.integration.lingxing.LingxingProductSku;
import com.ruoyi.integration.lingxing.LingxingRequestLogEntry;
import com.ruoyi.integration.lingxing.LingxingWarehouse;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.service.IUpstreamSystemService;
import com.ruoyi.integration.support.UpstreamMaskUtils;
import com.ruoyi.integration.support.UpstreamSystemConstants;
import com.ruoyi.system.service.support.SecretCipherSupport;

/**
 * 上游系统管理服务实现。
 */
@Service
public class UpstreamSystemServiceImpl implements IUpstreamSystemService
{
    private static final int SKU_PAGE_SIZE = 100;

    private static final long INVENTORY_SYNC_OVERLAP_MS = 5 * 60 * 1000L;

    private final Set<String> syncingSkuConnectionCodes = ConcurrentHashMap.newKeySet();
    private final Set<String> syncingInventoryConnectionCodes = ConcurrentHashMap.newKeySet();

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Autowired
    private SecretCipherSupport secretCipherSupport;

    @Override
    public List<UpstreamSystemConnection> selectConnectionList(UpstreamSystemConnection query)
    {
        return upstreamSystemMapper.selectConnectionList(query);
    }

    @Override
    public UpstreamSystemConnection selectConnectionByCode(String connectionCode)
    {
        UpstreamSystemConnection connection = upstreamSystemMapper.selectConnectionByCode(connectionCode);
        if (connection == null)
        {
            throw new ServiceException("主仓接入不存在");
        }
        return connection;
    }

    @Override
    @Transactional
    public int insertConnection(UpstreamConnectionRequest request)
    {
        String systemKind = normalizeSystemKind(request.getSystemKind());
        String connectionCode = StringUtils.defaultIfBlank(trimOptional(request.getConnectionCode()),
            generateConnectionCode(request.getMasterWarehouseName()));
        if (upstreamSystemMapper.selectConnectionByCode(connectionCode) != null)
        {
            throw new ServiceException("主仓接入编号已存在");
        }
        String appKey = trimRequired(request.getAppKey(), "appKey不能为空");
        String appSecret = trimRequired(request.getAppSecret(), "appSecret不能为空");
        checkCredentials(connectionCode, appKey, appSecret);

        UpstreamSystemConnection connection = new UpstreamSystemConnection();
        connection.setConnectionCode(connectionCode);
        connection.setSystemKind(systemKind);
        connection.setMasterWarehouseName(trimRequired(request.getMasterWarehouseName(), "主仓名称不能为空"));
        connection.setSettlementType(normalizeSettlementType(request.getSettlementType()));
        connection.setAppKeyMask(UpstreamMaskUtils.mask(appKey));
        connection.setAppSecretMask(UpstreamMaskUtils.mask(appSecret));
        connection.setAppKeyCiphertext(secretCipherSupport.encrypt(appKey));
        connection.setAppSecretCiphertext(secretCipherSupport.encrypt(appSecret));
        connection.setCredentialKeyId(secretCipherSupport.getEncryptionKeyId());
        connection.setStatus(UpstreamSystemConstants.STATUS_ENABLED);
        connection.setCredentialStatus(UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED);
        connection.setEnabledCapabilities(UpstreamSystemConstants.DEFAULT_CAPABILITIES);
        Integer maxOrder = upstreamSystemMapper.selectMaxDisplayOrder();
        connection.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        connection.setLastAuthorizedTime(new Date());
        connection.setRequestLogCount(0);
        connection.setCreateBy(SecurityUtils.getUsername());
        connection.setRemark(trimOptional(request.getRemark()));
        return upstreamSystemMapper.insertConnection(connection);
    }

    @Override
    public int updateConnectionInfo(String connectionCode, UpstreamConnectionInfoRequest request)
    {
        selectConnectionByCode(connectionCode);
        UpstreamSystemConnection connection = new UpstreamSystemConnection();
        connection.setConnectionCode(connectionCode);
        connection.setMasterWarehouseName(trimRequired(request.getMasterWarehouseName(), "主仓名称不能为空"));
        connection.setSettlementType(normalizeSettlementType(request.getSettlementType()));
        connection.setUpdateBy(SecurityUtils.getUsername());
        connection.setRemark(trimOptional(request.getRemark()));
        return upstreamSystemMapper.updateConnectionInfo(connection);
    }

    @Override
    @Transactional
    public int updateConnectionCredentials(String connectionCode, UpstreamCredentialRequest request)
    {
        selectConnectionByCode(connectionCode);
        String appKey = trimRequired(request.getAppKey(), "appKey不能为空");
        String appSecret = trimRequired(request.getAppSecret(), "appSecret不能为空");
        checkCredentials(connectionCode, appKey, appSecret);

        UpstreamSystemConnection connection = new UpstreamSystemConnection();
        connection.setConnectionCode(connectionCode);
        connection.setAppKeyMask(UpstreamMaskUtils.mask(appKey));
        connection.setAppSecretMask(UpstreamMaskUtils.mask(appSecret));
        connection.setAppKeyCiphertext(secretCipherSupport.encrypt(appKey));
        connection.setAppSecretCiphertext(secretCipherSupport.encrypt(appSecret));
        connection.setCredentialKeyId(secretCipherSupport.getEncryptionKeyId());
        connection.setCredentialStatus(UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED);
        connection.setLastAuthorizedTime(new Date());
        connection.setUpdateBy(SecurityUtils.getUsername());
        return upstreamSystemMapper.updateConnectionCredentials(connection);
    }

    @Override
    public int updateConnectionStatus(String connectionCode, String status)
    {
        selectConnectionByCode(connectionCode);
        String normalizedStatus = trimRequired(status, "状态不能为空").toUpperCase();
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(normalizedStatus)
            && !UpstreamSystemConstants.STATUS_DISABLED.equals(normalizedStatus))
        {
            throw new ServiceException("接入状态只能是 ENABLED 或 DISABLED");
        }
        return upstreamSystemMapper.updateConnectionStatus(connectionCode, normalizedStatus, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int updateConnectionOrder(List<String> connectionCodes)
    {
        if (connectionCodes == null || connectionCodes.isEmpty())
        {
            throw new ServiceException("排序主仓不能为空");
        }
        Set<String> seenCodes = new HashSet<>();
        int rows = 0;
        int displayOrder = 1;
        for (String rawCode : connectionCodes)
        {
            String connectionCode = trimRequired(rawCode, "主仓接入编号不能为空");
            if (!seenCodes.add(connectionCode))
            {
                throw new ServiceException("排序主仓不能重复：" + connectionCode);
            }
            selectConnectionByCode(connectionCode);
            rows += upstreamSystemMapper.updateConnectionDisplayOrder(connectionCode, displayOrder++, SecurityUtils.getUsername());
        }
        return rows;
    }

    @Override
    public int authorize(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        LingxingOpenApiClient client = createClient(connection);
        try
        {
            client.listWorkOrderTypes(UUID.randomUUID().toString());
            connection.setCredentialStatus(UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED);
            connection.setLastAuthorizedTime(new Date());
            connection.setUpdateBy(SecurityUtils.getUsername());
            upstreamSystemMapper.updateConnectionCredentials(connection);
            return 1;
        }
        catch (RuntimeException ex)
        {
            upstreamSystemMapper.updateConnectionStatus(connectionCode, connection.getStatus(), SecurityUtils.getUsername());
            throw toServiceException(ex);
        }
    }

    @Override
    public UpstreamSyncResult syncAll(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步");
        }
        if (!syncingSkuConnectionCodes.add(connectionCode))
        {
            throw new ServiceException("该主仓SKU正在同步，请稍后再试");
        }
        String syncBatchId = UUID.randomUUID().toString();
        UpstreamSkuSyncState syncing = new UpstreamSkuSyncState();
        syncing.setConnectionCode(connectionCode);
        syncing.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
        syncing.setSyncBatchId(syncBatchId);
        syncing.setLastStartedTime(new Date());
        upstreamSystemMapper.upsertSkuSyncState(syncing);

        try
        {
            LingxingOpenApiClient client = createClient(connection);
            client.listWorkOrderTypes(UUID.randomUUID().toString());
            List<LingxingWarehouse> warehouses = client.listWarehouses(UUID.randomUUID().toString());
            for (LingxingWarehouse warehouse : warehouses)
            {
                UpstreamWarehouseSyncItem item = new UpstreamWarehouseSyncItem();
                item.setConnectionCode(connectionCode);
                item.setWarehouseCode(warehouse.getWarehouseCode());
                item.setWarehouseName(warehouse.getWarehouseName());
                item.setCountryCode(warehouse.getCountryCode());
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSyncBatchId(syncBatchId);
                upstreamSystemMapper.upsertWarehouseSyncItem(item);
            }
            upstreamSystemMapper.markMissingWarehouses(connectionCode, syncBatchId);

            int channelCount = 0;
            for (LingxingWarehouse warehouse : warehouses)
            {
                List<LingxingLogisticsChannel> channels = client.listLogisticsChannels(warehouse.getWarehouseCode(), UUID.randomUUID().toString());
                for (LingxingLogisticsChannel channel : channels)
                {
                    UpstreamLogisticsChannelSyncItem item = new UpstreamLogisticsChannelSyncItem();
                    item.setConnectionCode(connectionCode);
                    item.setWarehouseCode(channel.getWarehouseCode());
                    item.setChannelCode(channel.getChannelCode());
                    item.setChannelName(channel.getChannelName());
                    item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                    item.setSyncBatchId(syncBatchId);
                    upstreamSystemMapper.upsertLogisticsChannelSyncItem(item);
                    channelCount++;
                }
            }
            upstreamSystemMapper.markMissingLogisticsChannels(connectionCode, syncBatchId);

            UpstreamSyncResult skuResult = syncSkus(client, connectionCode, syncBatchId);
            UpstreamSkuSyncState fresh = new UpstreamSkuSyncState();
            fresh.setConnectionCode(connectionCode);
            fresh.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
            fresh.setSyncBatchId(syncBatchId);
            fresh.setLastStartedTime(syncing.getLastStartedTime());
            fresh.setLastFinishedTime(new Date());
            fresh.setLastSuccessTime(new Date());
            fresh.setNextSyncTime(new Date(System.currentTimeMillis() + 10 * 60 * 1000L));
            upstreamSystemMapper.upsertSkuSyncState(fresh);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);

            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(syncBatchId);
            result.setWarehouseCount(warehouses.size());
            result.setLogisticsChannelCount(channelCount);
            result.setSkuCount(skuResult.getSkuCount());
            result.setSkuDimensionCount(skuResult.getSkuDimensionCount());
            return result;
        }
        catch (RuntimeException ex)
        {
            UpstreamSkuSyncState failed = new UpstreamSkuSyncState();
            failed.setConnectionCode(connectionCode);
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setSyncBatchId(syncBatchId);
            failed.setLastStartedTime(syncing.getLastStartedTime());
            failed.setLastFinishedTime(new Date());
            failed.setLastErrorMessage(StringUtils.left(ex.getMessage(), 500));
            upstreamSystemMapper.upsertSkuSyncState(failed);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            throw toServiceException(ex);
        }
        finally
        {
            syncingSkuConnectionCodes.remove(connectionCode);
        }
    }

    @Override
    public UpstreamSyncResult syncSkusOnly(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步SKU");
        }
        if (!syncingSkuConnectionCodes.add(connectionCode))
        {
            throw new ServiceException("该主仓SKU正在同步，请稍后再试");
        }

        String syncBatchId = UUID.randomUUID().toString();
        Date startedTime = new Date();
        UpstreamSkuSyncState syncing = new UpstreamSkuSyncState();
        syncing.setConnectionCode(connectionCode);
        syncing.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
        syncing.setSyncBatchId(syncBatchId);
        syncing.setLastStartedTime(startedTime);
        upstreamSystemMapper.upsertSkuSyncState(syncing);

        try
        {
            LingxingOpenApiClient client = createClient(connection);
            UpstreamSyncResult skuResult = syncSkus(client, connectionCode, syncBatchId);
            Date finishedTime = new Date();
            UpstreamSkuSyncState fresh = new UpstreamSkuSyncState();
            fresh.setConnectionCode(connectionCode);
            fresh.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
            fresh.setSyncBatchId(syncBatchId);
            fresh.setLastStartedTime(startedTime);
            fresh.setLastFinishedTime(finishedTime);
            fresh.setLastSuccessTime(finishedTime);
            fresh.setNextSyncTime(new Date(System.currentTimeMillis() + 10 * 60 * 1000L));
            upstreamSystemMapper.upsertSkuSyncState(fresh);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);

            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(syncBatchId);
            result.setSkuCount(skuResult.getSkuCount());
            result.setSkuDimensionCount(skuResult.getSkuDimensionCount());
            return result;
        }
        catch (RuntimeException ex)
        {
            UpstreamSkuSyncState failed = new UpstreamSkuSyncState();
            failed.setConnectionCode(connectionCode);
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setSyncBatchId(syncBatchId);
            failed.setLastStartedTime(startedTime);
            failed.setLastFinishedTime(new Date());
            failed.setLastErrorMessage(StringUtils.left(ex.getMessage(), 500));
            upstreamSystemMapper.upsertSkuSyncState(failed);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            throw toServiceException(ex);
        }
        finally
        {
            syncingSkuConnectionCodes.remove(connectionCode);
        }
    }

    @Override
    public UpstreamSyncResult syncSkuDimensionsOnly(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步仓库尺寸重量");
        }
        if (!syncingSkuConnectionCodes.add(connectionCode))
        {
            throw new ServiceException("该主仓SKU正在同步，请稍后再试");
        }

        try
        {
            LingxingOpenApiClient client = createClient(connection);
            List<UpstreamSkuSyncItem> existingSkus = upstreamSystemMapper.selectSkuSyncList(connectionCode,
                UpstreamSystemConstants.STATUS_ACTIVE, null, null, null, null);
            int dimensionCount = syncSkuDimensions(client, connectionCode, existingSkus);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);

            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(UUID.randomUUID().toString());
            result.setSkuDimensionCount(dimensionCount);
            return result;
        }
        catch (RuntimeException ex)
        {
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            throw toServiceException(ex);
        }
        finally
        {
            syncingSkuConnectionCodes.remove(connectionCode);
        }
    }

    @Override
    public UpstreamSyncResult syncWarehouseStocksOnly(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步库存");
        }
        if (!syncingInventoryConnectionCodes.add(connectionCode))
        {
            throw new ServiceException("该主仓库存正在同步，请稍后再试");
        }

        UpstreamInventorySyncState previousState = upstreamSystemMapper.selectInventorySyncState(connectionCode);
        String syncBatchId = UUID.randomUUID().toString();
        Date startedTime = new Date();
        UpstreamInventorySyncState syncing = new UpstreamInventorySyncState();
        syncing.setConnectionCode(connectionCode);
        syncing.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
        syncing.setSyncBatchId(syncBatchId);
        syncing.setLastStartedTime(startedTime);
        syncing.setTotalCount(0);
        syncing.setActiveCount(0);
        syncing.setMissingCount(0);
        syncing.setLastErrorMessage("");
        upstreamSystemMapper.upsertInventorySyncState(syncing);

        try
        {
            LingxingOpenApiClient client = createClient(connection);
            int stockCount = syncWarehouseStocks(client, connectionCode, syncBatchId, previousState, startedTime);
            Date finishedTime = new Date();
            UpstreamInventorySyncState fresh = new UpstreamInventorySyncState();
            fresh.setConnectionCode(connectionCode);
            fresh.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
            fresh.setSyncBatchId(syncBatchId);
            fresh.setLastStartedTime(startedTime);
            fresh.setLastFinishedTime(finishedTime);
            fresh.setLastSuccessTime(finishedTime);
            fresh.setNextSyncTime(new Date(System.currentTimeMillis() + 10 * 60 * 1000L));
            fresh.setTotalCount(stockCount);
            fresh.setActiveCount(stockCount);
            fresh.setMissingCount(0);
            fresh.setLastErrorMessage("");
            upstreamSystemMapper.upsertInventorySyncState(fresh);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);

            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(syncBatchId);
            result.setWarehouseStockCount(stockCount);
            return result;
        }
        catch (RuntimeException ex)
        {
            UpstreamInventorySyncState failed = new UpstreamInventorySyncState();
            failed.setConnectionCode(connectionCode);
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setSyncBatchId(syncBatchId);
            failed.setLastStartedTime(startedTime);
            failed.setLastFinishedTime(new Date());
            failed.setTotalCount(0);
            failed.setActiveCount(0);
            failed.setMissingCount(0);
            failed.setLastErrorMessage(StringUtils.left(ex.getMessage(), 500));
            upstreamSystemMapper.upsertInventorySyncState(failed);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            throw toServiceException(ex);
        }
        finally
        {
            syncingInventoryConnectionCodes.remove(connectionCode);
        }
    }

    @Override
    public List<SourceProductItem> selectSourceProductList(SourceProductQuery query)
    {
        SourceProductQuery normalized = normalizeSourceProductQuery(query);
        List<SourceProductItem> list = upstreamSystemMapper.selectSourceProductList(normalized);
        fillSourceProductLabels(list);
        return list;
    }

    @Override
    public SourceProductGroupDetail selectSourceProductGroupDetail(SourceProductQuery query)
    {
        SourceProductQuery normalized = normalizeSourceProductQuery(query);
        normalized.setSourceSkuGroupKey(trimRequired(normalized.getSourceSkuGroupKey(), "来源SKU组不能为空"));
        SourceProductItem group = upstreamSystemMapper.selectSourceProductGroupSummary(normalized);
        if (group == null)
        {
            throw new ServiceException("来源SKU组不存在或已失效");
        }
        group.setSystemKindLabel(systemKindLabel(group.getSystemKind()));

        List<SourceProductItem> dimensionGroups = upstreamSystemMapper.selectSourceProductList(normalized);
        List<SourceProductItem> warehouses = upstreamSystemMapper.selectSourceProductWarehouseDetailList(normalized);
        fillSourceProductLabels(dimensionGroups);
        fillSourceProductLabels(warehouses);

        SourceProductGroupDetail detail = new SourceProductGroupDetail();
        detail.setGroup(group);
        detail.setDimensionGroups(dimensionGroups);
        detail.setWarehouses(warehouses);
        return detail;
    }

    @Override
    public List<SourceWarehouseStockItem> selectSourceWarehouseStockList(SourceWarehouseStockQuery query)
    {
        SourceWarehouseStockQuery normalized = normalizeSourceWarehouseStockQuery(query);
        if (StringUtils.isNotBlank(normalized.getConnectionCode()))
        {
            selectConnectionByCode(normalized.getConnectionCode());
        }
        List<SourceWarehouseStockItem> list = upstreamSystemMapper.selectSourceWarehouseStockList(normalized);
        fillSourceWarehouseStockLabels(list);
        return list;
    }

    @Override
    public UpstreamInventorySyncState selectInventorySyncState(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        UpstreamInventorySyncState state = upstreamSystemMapper.selectInventorySyncState(connectionCode);
        if (state != null)
        {
            return state;
        }
        UpstreamInventorySyncState never = new UpstreamInventorySyncState();
        never.setConnectionCode(connectionCode);
        never.setStatus(UpstreamSystemConstants.SYNC_STATUS_NEVER);
        never.setTotalCount(0);
        never.setActiveCount(0);
        never.setMissingCount(0);
        return never;
    }

    @Override
    public List<UpstreamWarehouseSyncItem> selectWarehouseSyncList(String connectionCode, String status)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectWarehouseSyncList(connectionCode, trimOptional(status));
    }

    @Override
    public List<UpstreamWarehousePairing> selectWarehousePairingList(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectWarehousePairingList(connectionCode);
    }

    @Override
    @Transactional
    public int insertWarehousePairing(String connectionCode, WarehousePairingRequest request)
    {
        selectConnectionByCode(connectionCode);
        String upstreamWarehouseCode = trimRequired(request.getUpstreamWarehouseCode(), "领星仓库代码不能为空");
        UpstreamWarehouseSyncItem candidate = upstreamSystemMapper.selectWarehouseSyncItem(connectionCode, upstreamWarehouseCode);
        if (candidate == null)
        {
            throw new ServiceException("领星仓库不在同步清单中，请先同步仓库");
        }
        UpstreamWarehousePairing pairing = new UpstreamWarehousePairing();
        pairing.setConnectionCode(connectionCode);
        pairing.setUpstreamWarehouseCode(upstreamWarehouseCode);
        pairing.setUpstreamWarehouseName(candidate.getWarehouseName());
        pairing.setSystemWarehouseCode(trimRequired(request.getSystemWarehouseCode(), "系统仓库代码不能为空"));
        pairing.setSystemWarehouseName(trimRequired(request.getSystemWarehouseName(), "系统仓库名称不能为空"));
        pairing.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
        pairing.setCreateBy(SecurityUtils.getUsername());
        pairing.setRemark(trimOptional(request.getRemark()));
        try
        {
            return upstreamSystemMapper.insertWarehousePairing(pairing);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("仓库配对重复：系统仓库或领星仓库已经配对");
        }
    }

    @Override
    public int deleteWarehousePairing(Long warehousePairingId)
    {
        return upstreamSystemMapper.deleteWarehousePairing(warehousePairingId);
    }

    @Override
    public List<UpstreamLogisticsChannelSyncItem> selectLogisticsChannelSyncList(String connectionCode, String status)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectLogisticsChannelSyncList(connectionCode, trimOptional(status));
    }

    @Override
    public List<UpstreamLogisticsChannelPairing> selectLogisticsChannelPairingList(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectLogisticsChannelPairingList(connectionCode);
    }

    @Override
    @Transactional
    public int insertLogisticsChannelPairing(String connectionCode, LogisticsChannelPairingRequest request)
    {
        selectConnectionByCode(connectionCode);
        String upstreamChannelCode = trimRequired(request.getUpstreamChannelCode(), "领星渠道代码不能为空");
        UpstreamLogisticsChannelSyncItem candidate = upstreamSystemMapper.selectLogisticsChannelSyncItem(connectionCode, upstreamChannelCode);
        if (candidate == null)
        {
            throw new ServiceException("领星物流渠道不在同步清单中，请先同步物流渠道");
        }
        UpstreamLogisticsChannelPairing pairing = new UpstreamLogisticsChannelPairing();
        pairing.setConnectionCode(connectionCode);
        pairing.setUpstreamChannelCode(upstreamChannelCode);
        pairing.setUpstreamChannelName(candidate.getChannelName());
        pairing.setSystemChannelCode(trimRequired(request.getSystemChannelCode(), "系统渠道代码不能为空"));
        pairing.setSystemChannelName(trimRequired(request.getSystemChannelName(), "系统渠道名称不能为空"));
        pairing.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
        pairing.setCreateBy(SecurityUtils.getUsername());
        pairing.setRemark(trimOptional(request.getRemark()));
        try
        {
            return upstreamSystemMapper.insertLogisticsChannelPairing(pairing);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("物流渠道配对重复：系统渠道已经配对");
        }
    }

    @Override
    public int deleteLogisticsChannelPairing(Long logisticsChannelPairingId)
    {
        return upstreamSystemMapper.deleteLogisticsChannelPairing(logisticsChannelPairingId);
    }

    @Override
    public List<UpstreamSkuSyncItem> selectSkuSyncList(String connectionCode, String status, String pairingStatus,
        String dimensionStatus, String field, String keyword)
    {
        return upstreamSystemMapper.selectSkuSyncList(connectionCode, trimOptional(status),
            trimOptional(pairingStatus), trimOptional(dimensionStatus), trimOptional(field), trimOptional(keyword));
    }

    @Override
    public List<UpstreamSkuPairing> selectSkuPairingList(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectSkuPairingList(connectionCode);
    }

    @Override
    @Transactional
    public int insertSkuPairing(String connectionCode, SkuPairingRequest request)
    {
        selectConnectionByCode(connectionCode);
        String masterSku = trimRequired(request.getMasterSku(), "领星masterSku不能为空");
        UpstreamSkuSyncItem candidate = upstreamSystemMapper.selectSkuSyncItem(connectionCode, masterSku);
        if (candidate == null)
        {
            throw new ServiceException("领星SKU不在同步清单中，请先同步SKU");
        }
        UpstreamSkuPairing pairing = new UpstreamSkuPairing();
        pairing.setConnectionCode(connectionCode);
        pairing.setMasterSku(masterSku);
        pairing.setSystemSku(trimRequired(request.getSystemSku(), "系统SKU不能为空"));
        pairing.setSystemSkuName(trimRequired(request.getSystemSkuName(), "系统SKU名称不能为空"));
        pairing.setCustomerName(trimOptional(request.getCustomerName()));
        pairing.setCreateBy(SecurityUtils.getUsername());
        pairing.setRemark(trimOptional(request.getRemark()));
        try
        {
            int rows = upstreamSystemMapper.insertSkuPairing(pairing);
            insertSkuAudit("PAIR", pairing, null, pairing, request.getRemark());
            return rows;
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("SKU配对重复：系统SKU或领星masterSku已经配对");
        }
    }

    @Override
    @Transactional
    public int deleteSkuPairing(Long skuPairingId)
    {
        UpstreamSkuPairing current = upstreamSystemMapper.selectSkuPairingById(skuPairingId);
        if (current == null)
        {
            return 0;
        }
        int rows = upstreamSystemMapper.deleteSkuPairing(skuPairingId);
        insertSkuAudit("UNPAIR", current, current, null, "解除SKU配对");
        return rows;
    }

    @Override
    public UpstreamSkuSyncState selectSkuSyncState(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectSkuSyncState(connectionCode);
    }

    @Override
    public List<UpstreamRequestLog> selectRequestLogList(String connectionCode)
    {
        return upstreamSystemMapper.selectRequestLogList(connectionCode);
    }

    private UpstreamSyncResult syncSkus(LingxingOpenApiClient client, String connectionCode, String syncBatchId)
    {
        int current = 1;
        int total = Integer.MAX_VALUE;
        int synced = 0;
        int dimensionSynced = 0;
        while ((current - 1) * SKU_PAGE_SIZE < total)
        {
            LingxingProductPage page = client.listProductSkuPage(current, SKU_PAGE_SIZE, UUID.randomUUID().toString());
            total = page.getTotal();
            List<LingxingProductSku> records = page.getRecords();
            if (records == null || records.isEmpty())
            {
                break;
            }
            List<UpstreamSkuSyncItem> items = new ArrayList<>();
            for (LingxingProductSku sku : records)
            {
                UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
                item.setConnectionCode(connectionCode);
                item.setMasterSku(sku.getSku());
                item.setMasterProductName(sku.getProductName());
                copyLingxingSkuFields(item, sku);
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSearchText(buildSkuSearchText(sku));
                item.setSyncBatchId(syncBatchId);
                items.add(item);
                synced++;
            }
            upstreamSystemMapper.batchUpsertSkuSyncItems(items);
            dimensionSynced += syncSkuDimensions(client, connectionCode, items);
            current++;
        }
        upstreamSystemMapper.markMissingSkus(connectionCode, syncBatchId);
        UpstreamSyncResult result = new UpstreamSyncResult();
        result.setSkuCount(synced);
        result.setSkuDimensionCount(dimensionSynced);
        return result;
    }

    private int syncSkuDimensions(LingxingOpenApiClient client, String connectionCode, List<UpstreamSkuSyncItem> skus)
    {
        if (skus == null || skus.isEmpty())
        {
            return 0;
        }
        int updated = 0;
        int start = 0;
        while (start < skus.size())
        {
            int end = Math.min(start + SKU_PAGE_SIZE, skus.size());
            List<String> skuCodes = new ArrayList<>();
            for (int i = start; i < end; i++)
            {
                String masterSku = skus.get(i).getMasterSku();
                if (StringUtils.isNotBlank(masterSku))
                {
                    skuCodes.add(masterSku);
                }
            }
            if (!skuCodes.isEmpty())
            {
                LingxingProductPage page = client.listProductSkuPageBySkuList(skuCodes, UUID.randomUUID().toString());
                List<LingxingProductSku> records = page.getRecords();
                if (records != null)
                {
                    for (LingxingProductSku sku : records)
                    {
                        if (!hasAnyWmsDimension(sku))
                        {
                            continue;
                        }
                        UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
                        item.setConnectionCode(connectionCode);
                        item.setMasterSku(sku.getSku());
                        copyLingxingSkuFields(item, sku);
                        updated += upstreamSystemMapper.updateSkuWmsDimensions(item);
                    }
                }
            }
            start = end;
        }
        return updated;
    }

    private int syncWarehouseStocks(LingxingOpenApiClient client, String connectionCode, String syncBatchId,
        UpstreamInventorySyncState previousState, Date startedTime)
    {
        Map<String, UpstreamWarehousePairing> warehousePairingMap = buildWarehousePairingMap(connectionCode);
        Map<String, UpstreamWarehouseSyncItem> warehouseSyncMap = buildWarehouseSyncMap(connectionCode);
        Map<String, UpstreamSkuPairing> skuPairingMap = buildSkuPairingMap(connectionCode);
        String startTime = inventorySyncStartTime(previousState);
        String endTime = StringUtils.isBlank(startTime) ? "" : formatLingxingDateTime(startedTime);
        int current = 1;
        int total = Integer.MAX_VALUE;
        int synced = 0;
        while ((current - 1) * SKU_PAGE_SIZE < total)
        {
            LingxingInventoryProductPage page = client.listInventoryProductPage(current, SKU_PAGE_SIZE,
                startTime, endTime, UUID.randomUUID().toString());
            total = page.getTotal();
            List<LingxingInventoryProductStock> records = page.getRecords();
            if (records == null || records.isEmpty())
            {
                break;
            }
            for (LingxingInventoryProductStock stock : records)
            {
                SourceWarehouseStockItem item = new SourceWarehouseStockItem();
                item.setConnectionCode(connectionCode);
                copyLingxingInventoryStockFields(item, stock);
                UpstreamWarehouseSyncItem warehouse = warehouseSyncMap.get(item.getUpstreamWarehouseCode());
                if (StringUtils.isBlank(item.getUpstreamWarehouseName()) && warehouse != null)
                {
                    item.setUpstreamWarehouseName(warehouse.getWarehouseName());
                }
                UpstreamWarehousePairing warehousePairing = warehousePairingMap.get(item.getUpstreamWarehouseCode());
                if (warehousePairing != null)
                {
                    item.setSystemWarehouseCode(warehousePairing.getSystemWarehouseCode());
                    item.setSystemWarehouseName(warehousePairing.getSystemWarehouseName());
                }
                UpstreamSkuPairing skuPairing = skuPairingMap.get(item.getMasterSku());
                if (skuPairing != null)
                {
                    item.setSystemSku(skuPairing.getSystemSku());
                    item.setSystemSkuName(skuPairing.getSystemSkuName());
                    item.setCustomerName(skuPairing.getCustomerName());
                }
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSyncBatchId(syncBatchId);
                upstreamSystemMapper.upsertSourceWarehouseStock(item);
                synced++;
            }
            current++;
        }
        return synced;
    }

    private String inventorySyncStartTime(UpstreamInventorySyncState previousState)
    {
        if (previousState == null || previousState.getLastSuccessTime() == null)
        {
            return "";
        }
        long startAt = Math.max(0L, previousState.getLastSuccessTime().getTime() - INVENTORY_SYNC_OVERLAP_MS);
        return formatLingxingDateTime(new Date(startAt));
    }

    private String formatLingxingDateTime(Date value)
    {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
    }

    private Map<String, UpstreamWarehousePairing> buildWarehousePairingMap(String connectionCode)
    {
        Map<String, UpstreamWarehousePairing> result = new HashMap<>();
        List<UpstreamWarehousePairing> pairings = upstreamSystemMapper.selectWarehousePairingList(connectionCode);
        for (UpstreamWarehousePairing pairing : pairings)
        {
            if (StringUtils.isNotBlank(pairing.getUpstreamWarehouseCode()))
            {
                result.put(pairing.getUpstreamWarehouseCode(), pairing);
            }
        }
        return result;
    }

    private Map<String, UpstreamWarehouseSyncItem> buildWarehouseSyncMap(String connectionCode)
    {
        Map<String, UpstreamWarehouseSyncItem> result = new HashMap<>();
        List<UpstreamWarehouseSyncItem> warehouses = upstreamSystemMapper.selectWarehouseSyncList(connectionCode, null);
        for (UpstreamWarehouseSyncItem warehouse : warehouses)
        {
            if (StringUtils.isNotBlank(warehouse.getWarehouseCode()))
            {
                result.put(warehouse.getWarehouseCode(), warehouse);
            }
        }
        return result;
    }

    private Map<String, UpstreamSkuPairing> buildSkuPairingMap(String connectionCode)
    {
        Map<String, UpstreamSkuPairing> result = new HashMap<>();
        List<UpstreamSkuPairing> pairings = upstreamSystemMapper.selectSkuPairingList(connectionCode);
        for (UpstreamSkuPairing pairing : pairings)
        {
            if (StringUtils.isNotBlank(pairing.getMasterSku()))
            {
                result.put(pairing.getMasterSku(), pairing);
            }
        }
        return result;
    }

    private void copyLingxingInventoryStockFields(SourceWarehouseStockItem item, LingxingInventoryProductStock stock)
    {
        item.setUpstreamWarehouseCode(StringUtils.trimToEmpty(stock.getWarehouseCode()));
        item.setUpstreamWarehouseName(StringUtils.trimToEmpty(stock.getWarehouseName()));
        item.setMasterSku(StringUtils.trimToEmpty(stock.getSku()));
        item.setMasterProductName(StringUtils.trimToEmpty(stock.getProductName()));
        item.setInventoryScope(StringUtils.defaultIfBlank(StringUtils.trimToEmpty(stock.getInventoryScope()),
            "COMPREHENSIVE"));
        item.setInventoryAttribute(StringUtils.trimToEmpty(stock.getInventoryAttribute()));
        item.setBatchNo(StringUtils.trimToEmpty(stock.getBatchNo()));
        item.setLocationCode(StringUtils.trimToEmpty(stock.getLocationCode()));
        item.setTotalQuantity(zeroIfNull(stock.getTotalQuantity()));
        item.setAvailableQuantity(zeroIfNull(stock.getAvailableQuantity()));
        item.setLockedQuantity(zeroIfNull(stock.getLockedQuantity()));
        item.setInTransitQuantity(zeroIfNull(stock.getInTransitQuantity()));
        item.setBoxedQuantity(stock.getBoxedQuantity());
        item.setUnboxedQuantity(stock.getUnboxedQuantity());
        item.setSourcePayloadJson(stock.getSourcePayloadJson());
        item.setSourcePayloadHash(stock.getSourcePayloadHash());
    }

    private SourceProductQuery normalizeSourceProductQuery(SourceProductQuery query)
    {
        SourceProductQuery normalized = query == null ? new SourceProductQuery() : query;
        normalized.setRepositoryScope(trimOptional(normalized.getRepositoryScope()));
        normalized.setSourceSkuGroupKey(trimOptional(normalized.getSourceSkuGroupKey()));
        normalized.setConnectionCode(trimOptional(normalized.getConnectionCode()));
        normalized.setSystemKind(normalizeSystemKindOptional(normalized.getSystemKind()));
        normalized.setMasterWarehouseName(trimOptional(normalized.getMasterWarehouseName()));
        normalized.setMasterSku(trimOptional(normalized.getMasterSku()));
        normalized.setProductName(trimOptional(normalized.getProductName()));
        normalized.setIdentifyCodeKeyword(trimOptional(normalized.getIdentifyCodeKeyword()));
        normalized.setCategoryKeyword(trimOptional(normalized.getCategoryKeyword()));
        normalized.setApproveStatus(trimOptional(normalized.getApproveStatus()));
        normalized.setStatus(trimOptional(normalized.getStatus()));
        normalized.setPairingStatus(trimOptional(normalized.getPairingStatus()));
        normalized.setKeyword(trimOptional(normalized.getKeyword()));
        return normalized;
    }

    private void fillSourceProductLabels(List<SourceProductItem> list)
    {
        for (SourceProductItem item : list)
        {
            item.setSystemKindLabel(systemKindLabel(item.getSystemKind()));
        }
    }

    private void fillSourceWarehouseStockLabels(List<SourceWarehouseStockItem> list)
    {
        for (SourceWarehouseStockItem item : list)
        {
            item.setSystemKindLabel(systemKindLabel(item.getSystemKind()));
        }
    }

    private SourceWarehouseStockQuery normalizeSourceWarehouseStockQuery(SourceWarehouseStockQuery query)
    {
        SourceWarehouseStockQuery normalized = query == null ? new SourceWarehouseStockQuery() : query;
        normalized.setConnectionCode(trimOptional(normalized.getConnectionCode()));
        normalized.setKeyword(trimOptional(normalized.getKeyword()));
        normalized.setStatus(trimOptional(normalized.getStatus()));
        normalized.setWarehousePairingStatus(trimOptional(normalized.getWarehousePairingStatus()));
        normalized.setSkuPairingStatus(trimOptional(normalized.getSkuPairingStatus()));
        normalized.setInventoryScope(trimOptional(normalized.getInventoryScope()));
        normalized.setInventoryAttribute(trimOptional(normalized.getInventoryAttribute()));
        normalized.setWarehouseKeyword(trimOptional(normalized.getWarehouseKeyword()));
        return normalized;
    }

    private String normalizeSystemKindOptional(String value)
    {
        String trimmed = trimOptional(value);
        if (StringUtils.isBlank(trimmed))
        {
            return null;
        }
        if (UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS_LEGACY.equals(trimmed))
        {
            return UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS;
        }
        return trimmed;
    }

    private String systemKindLabel(String systemKind)
    {
        String normalized = normalizeSystemKindOptional(systemKind);
        if (UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS.equals(normalized))
        {
            return "领星WMS";
        }
        return StringUtils.defaultIfBlank(systemKind, "未知来源");
    }

    private void copyLingxingSkuFields(UpstreamSkuSyncItem item, LingxingProductSku sku)
    {
        item.setProductAliasName(sku.getProductAliasName());
        item.setApproveStatus(sku.getApproveStatus());
        item.setProductType(sku.getProductType());
        item.setProductDescription(sku.getProductDescription());
        item.setImageUrl(sku.getImageUrl());
        item.setMainCode(sku.getMainCode());
        item.setOtherCode(sku.getOtherCode());
        item.setFnsku(sku.getFnsku());
        item.setCountryOfOriginName(sku.getCountryOfOriginName());
        item.setCurrencyCode(sku.getCurrencyCode());
        item.setCustomhouseCode(sku.getCustomhouseCode());
        item.setDangerousCargo(sku.getDangerousCargo());
        item.setDeclareNameCn(sku.getDeclareNameCn());
        item.setDeclareNameEn(sku.getDeclareNameEn());
        item.setDeclarePrice(sku.getDeclarePrice());
        item.setHeight(sku.getHeight());
        item.setHeightBs(sku.getHeightBs());
        item.setLength(sku.getLength());
        item.setLengthBs(sku.getLengthBs());
        item.setWeight(sku.getWeight());
        item.setWeightBs(sku.getWeightBs());
        item.setWidth(sku.getWidth());
        item.setWidthBs(sku.getWidthBs());
        item.setWmsHeight(sku.getWmsHeight());
        item.setWmsHeightBs(sku.getWmsHeightBs());
        item.setWmsLength(sku.getWmsLength());
        item.setWmsLengthBs(sku.getWmsLengthBs());
        item.setWmsWeight(sku.getWmsWeight());
        item.setWmsWeightBs(sku.getWmsWeightBs());
        item.setWmsWidth(sku.getWmsWidth());
        item.setWmsWidthBs(sku.getWmsWidthBs());
        item.setCat1Name(sku.getCat1Name());
        item.setCat2Name(sku.getCat2Name());
        item.setCat3Name(sku.getCat3Name());
        item.setPlatformSkuInfoJson(sku.getPlatformSkuInfoJson());
        item.setBrazilTaxInfoJson(sku.getBrazilTaxInfoJson());
        item.setSourcePayloadJson(sku.getSourcePayloadJson());
        item.setSourcePayloadHash(sku.getSourcePayloadHash());
    }

    private String buildSkuSearchText(LingxingProductSku sku)
    {
        List<String> parts = new ArrayList<>();
        addSearchPart(parts, sku.getSku());
        addSearchPart(parts, sku.getProductName());
        addSearchPart(parts, sku.getProductAliasName());
        addSearchPart(parts, sku.getMainCode());
        addSearchPart(parts, sku.getOtherCode());
        addSearchPart(parts, sku.getFnsku());
        addSearchPart(parts, sku.getDeclareNameCn());
        addSearchPart(parts, sku.getDeclareNameEn());
        addSearchPart(parts, sku.getCustomhouseCode());
        addSearchPart(parts, sku.getCountryOfOriginName());
        addSearchPart(parts, sku.getCat1Name());
        addSearchPart(parts, sku.getCat2Name());
        addSearchPart(parts, sku.getCat3Name());
        return String.join(" ", parts);
    }

    private boolean hasAnyWmsDimension(LingxingProductSku sku)
    {
        return sku.getWmsHeight() != null || sku.getWmsLength() != null || sku.getWmsWidth() != null
            || sku.getWmsWeight() != null || sku.getWmsHeightBs() != null || sku.getWmsLengthBs() != null
            || sku.getWmsWidthBs() != null || sku.getWmsWeightBs() != null;
    }

    private void addSearchPart(List<String> parts, String value)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isNotBlank(trimmed))
        {
            parts.add(trimmed);
        }
    }

    private void checkCredentials(String connectionCode, String appKey, String appSecret)
    {
        LingxingOpenApiClient client = new LingxingOpenApiClient(new LingxingCredentials(appKey, appSecret),
            entry -> insertRequestLog(connectionCode, entry));
        try
        {
            client.listWorkOrderTypes(UUID.randomUUID().toString());
        }
        catch (RuntimeException ex)
        {
            throw toServiceException(ex);
        }
    }

    private LingxingOpenApiClient createClient(UpstreamSystemConnection connection)
    {
        String appKey = secretCipherSupport.decrypt(connection.getAppKeyCiphertext());
        String appSecret = secretCipherSupport.decrypt(connection.getAppSecretCiphertext());
        return new LingxingOpenApiClient(new LingxingCredentials(appKey, appSecret),
            entry -> insertRequestLog(connection.getConnectionCode(), entry));
    }

    private void insertRequestLog(String connectionCode, LingxingRequestLogEntry entry)
    {
        UpstreamRequestLog log = new UpstreamRequestLog();
        log.setConnectionCode(connectionCode);
        log.setTraceId(entry.getTraceId());
        log.setOperation(entry.getOperation());
        log.setEndpoint(entry.getEndpoint());
        log.setRequestTime(entry.getRequestTime());
        log.setResponseTime(entry.getResponseTime());
        log.setDurationMs(entry.getDurationMs());
        log.setRequestPayloadRedacted(entry.getRequestPayloadRedacted());
        log.setResponsePayloadRedacted(entry.getResponsePayloadRedacted());
        log.setExternalErrorCode(entry.getExternalErrorCode());
        log.setExternalErrorMessage(entry.getExternalErrorMessage());
        log.setStatus(entry.getStatus());
        upstreamSystemMapper.insertRequestLog(log);
    }

    private void insertSkuAudit(String eventType, UpstreamSkuPairing reference, UpstreamSkuPairing before,
        UpstreamSkuPairing after, String remark)
    {
        UpstreamSkuPairingAuditEvent event = new UpstreamSkuPairingAuditEvent();
        event.setConnectionCode(reference.getConnectionCode());
        event.setMasterSku(reference.getMasterSku());
        event.setSystemSku(reference.getSystemSku());
        event.setEventType(eventType);
        event.setOperator(SecurityUtils.getUsername());
        event.setBeforeSnapshot(before == null ? "" : JSON.toJSONString(before));
        event.setAfterSnapshot(after == null ? "" : JSON.toJSONString(after));
        event.setRemark(trimOptional(remark));
        upstreamSystemMapper.insertSkuPairingAuditEvent(event);
    }

    private ServiceException toServiceException(RuntimeException ex)
    {
        if (ex instanceof ServiceException serviceException)
        {
            return serviceException;
        }
        if (ex instanceof LingxingClientException clientException)
        {
            return new ServiceException("领星接口调用失败：" + clientException.getMessage());
        }
        return new ServiceException(StringUtils.defaultIfBlank(ex.getMessage(), "上游系统处理失败"));
    }

    private String generateConnectionCode(String masterWarehouseName)
    {
        String seed = StringUtils.defaultIfBlank(masterWarehouseName, "MASTER")
            .trim()
            .replaceAll("\\s+", "-")
            .replaceAll("[^0-9A-Za-z\\u4e00-\\u9fa5-]", "");
        if (seed.length() > 24)
        {
            seed = seed.substring(0, 24);
        }
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "LX-" + StringUtils.defaultIfBlank(seed, "MASTER") + "-" + suffix;
    }

    private String normalizeSystemKind(String value)
    {
        String trimmed = StringUtils.defaultIfBlank(value, UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS).trim();
        if (UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS.equals(trimmed)
            || UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS_LEGACY.equals(trimmed))
        {
            return UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS;
        }
        throw new ServiceException("暂不支持的上游系统类型：" + trimmed);
    }

    private String normalizeSettlementType(String value)
    {
        String trimmed = trimRequired(value, "结算类型不能为空");
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE.equals(trimmed)
            || "UPSTREAM_PAYABLE".equals(trimmed))
        {
            return UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE;
        }
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equals(trimmed)
            || "PLATFORM_ADVANCE".equals(trimmed))
        {
            return UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE;
        }
        throw new ServiceException("暂不支持的结算类型：" + trimmed);
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

    private Long zeroIfNull(Long value)
    {
        return value == null ? 0L : value;
    }
}
