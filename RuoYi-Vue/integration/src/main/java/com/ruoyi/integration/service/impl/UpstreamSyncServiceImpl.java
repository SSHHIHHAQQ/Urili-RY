package com.ruoyi.integration.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.request.SkuDimensionSelectedSyncRequest;
import com.ruoyi.integration.domain.request.UpstreamSyncRequest;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.domain.response.UpstreamSyncResult;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.service.IUpstreamSyncService;
import com.ruoyi.integration.support.UpstreamSystemConstants;
import com.ruoyi.integration.sync.UpstreamInventorySyncComponent;
import com.ruoyi.integration.sync.UpstreamLingxingClientFactory;
import com.ruoyi.integration.sync.UpstreamLogisticsChannelSyncComponent;
import com.ruoyi.integration.sync.UpstreamSkuDimensionSyncComponent;
import com.ruoyi.integration.sync.UpstreamSkuInfoSyncComponent;
import com.ruoyi.integration.sync.UpstreamSyncStateRecorder;
import com.ruoyi.integration.sync.UpstreamWarehouseSyncComponent;

/**
 * 上游系统同步执行服务实现。
 */
@Service
public class UpstreamSyncServiceImpl implements IUpstreamSyncService
{
    private static final int SKU_DIMENSION_FULL_RATE_LIMIT_MS = 2000;

    private static final int SKU_DIMENSION_SELECTED_LIMIT = 100;

    private final Set<String> syncingConnectionCodes = ConcurrentHashMap.newKeySet();

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Autowired
    private SourceProductReadModelService sourceProductReadModelService;

    @Autowired
    private SourceWarehouseStockReadModelService sourceWarehouseStockReadModelService;

    @Autowired
    private UpstreamLingxingClientFactory lingxingClientFactory;

    @Autowired
    private UpstreamWarehouseSyncComponent warehouseSyncComponent;

    @Autowired
    private UpstreamLogisticsChannelSyncComponent logisticsChannelSyncComponent;

    @Autowired
    private UpstreamSkuInfoSyncComponent skuInfoSyncComponent;

    @Autowired
    private UpstreamSkuDimensionSyncComponent skuDimensionSyncComponent;

    @Autowired
    private UpstreamInventorySyncComponent inventorySyncComponent;

    @Autowired
    private UpstreamSyncStateRecorder syncStateRecorder;

    @Override
    public UpstreamSyncResult syncAll(String connectionCode)
    {
        UpstreamSyncRequest request = new UpstreamSyncRequest();
        request.setSyncTypes(Arrays.asList(
            UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL,
            UpstreamSystemConstants.SYNC_TYPE_SKU));
        return syncSelected(connectionCode, request);
    }

    @Override
    public UpstreamSyncResult syncSelected(String connectionCode, UpstreamSyncRequest request)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        List<String> syncTypes = normalizeSyncTypes(request == null ? null : request.getSyncTypes());
        if (syncTypes.isEmpty())
        {
            throw new ServiceException("请选择需要同步的内容");
        }
        acquireSyncLock(connectionCode);
        try
        {
            LingxingOpenApiClient client = lingxingClientFactory.createClient(connection);
            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(UUID.randomUUID().toString());
            for (String syncType : syncTypes)
            {
                UpstreamSyncItemResult item = executeSyncItem(client, connectionCode, syncType,
                    UpstreamSystemConstants.SYNC_MODE_MANUAL);
                mergeSyncItemResult(result, item);
            }
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            return result;
        }
        finally
        {
            releaseSyncLock(connectionCode);
        }
    }

    @Override
    public UpstreamSyncResult syncScheduled(String connectionCode, String syncType)
    {
        return syncSingleType(connectionCode, normalizeSingleSyncType(syncType),
            UpstreamSystemConstants.SYNC_MODE_SCHEDULED);
    }

    @Override
    public UpstreamSyncResult syncWarehousesOnly(String connectionCode)
    {
        return syncSingleType(connectionCode, UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    @Override
    public UpstreamSyncResult syncLogisticsChannelsOnly(String connectionCode)
    {
        return syncSingleType(connectionCode, UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL,
            UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    @Override
    public UpstreamSyncResult syncSkuInfoOnly(String connectionCode)
    {
        return syncSingleType(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU,
            UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    @Override
    public UpstreamSyncResult syncSkusOnly(String connectionCode)
    {
        return syncSkuInfoOnly(connectionCode);
    }

    @Override
    public UpstreamSyncResult syncSkuDimensionsOnly(String connectionCode)
    {
        return syncSingleType(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
            UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    @Override
    public UpstreamSyncResult syncSkuDimensionsBySkuList(String connectionCode, SkuDimensionSelectedSyncRequest request)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        List<String> skuList = normalizeSelectedSkuList(request == null ? null : request.getSkuList());
        String syncBatchId = null;
        Date startedTime = null;
        acquireSyncLock(connectionCode);
        try
        {
            LingxingOpenApiClient client = lingxingClientFactory.createClient(connection);
            syncBatchId = UUID.randomUUID().toString();
            startedTime = new Date();
            syncStateRecorder.recordSyncing(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                syncBatchId, startedTime, UpstreamSystemConstants.SYNC_MODE_SELECTED, 0);
            UpstreamSyncItemResult item = skuDimensionSyncComponent.syncSelected(client, connectionCode, skuList,
                syncBatchId);
            int dimensionCount = item.getCount();
            Date finishedTime = new Date();
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);

            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(syncBatchId);
            result.setSkuDimensionCount(dimensionCount);
            result.getItems().add(item);
            syncStateRecorder.recordSuccess(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                UpstreamSystemConstants.SYNC_MODE_SELECTED, syncBatchId, startedTime, finishedTime, item, 0);
            return result;
        }
        catch (RuntimeException ex)
        {
            Date finishedTime = new Date();
            String message = StringUtils.left(ex.getMessage(), 500);
            if (syncBatchId != null && startedTime != null)
            {
                UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
                failed.setSyncType(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION);
                failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
                failed.setErrorMessage(message);
                syncStateRecorder.recordFailure(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                    UpstreamSystemConstants.SYNC_MODE_SELECTED, syncBatchId, startedTime, finishedTime,
                    failed, message, 0);
            }
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            throw lingxingClientFactory.toServiceException(ex);
        }
        finally
        {
            releaseSyncLock(connectionCode);
        }
    }

    @Override
    public UpstreamSyncResult syncWarehouseStocksOnly(String connectionCode)
    {
        return syncSingleType(connectionCode, UpstreamSystemConstants.SYNC_TYPE_INVENTORY,
            UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    private UpstreamSyncResult syncSingleType(String connectionCode, String syncType, String mode)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        acquireSyncLock(connectionCode);
        try
        {
            LingxingOpenApiClient client = lingxingClientFactory.createClient(connection);
            UpstreamSyncItemResult item = executeSyncItem(client, connectionCode, syncType, mode);
            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(UUID.randomUUID().toString());
            mergeSyncItemResult(result, item);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            return result;
        }
        finally
        {
            releaseSyncLock(connectionCode);
        }
    }

    private UpstreamSyncItemResult executeSyncItem(LingxingOpenApiClient client, String connectionCode,
        String syncType, String mode)
    {
        String syncBatchId = UUID.randomUUID().toString();
        Date startedTime = new Date();
        int rateLimitMs = UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType)
            ? SKU_DIMENSION_FULL_RATE_LIMIT_MS : 0;
        syncStateRecorder.recordSyncing(connectionCode, syncType, syncBatchId, startedTime, mode, rateLimitMs);

        try
        {
            UpstreamSyncItemResult item = doSyncItem(client, connectionCode, syncType, syncBatchId, startedTime,
                rateLimitMs);
            Date finishedTime = new Date();
            rebuildReadModel(connectionCode, syncType);
            syncStateRecorder.recordSuccess(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
                item, rateLimitMs);
            return item;
        }
        catch (RuntimeException ex)
        {
            Date finishedTime = new Date();
            String message = StringUtils.left(ex.getMessage(), 500);
            UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
            failed.setSyncType(syncType);
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setErrorMessage(message);
            syncStateRecorder.recordFailure(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
                failed, message, rateLimitMs);
            throw lingxingClientFactory.toServiceException(ex);
        }
    }

    private UpstreamSyncItemResult doSyncItem(LingxingOpenApiClient client, String connectionCode, String syncType,
        String syncBatchId, Date startedTime, int rateLimitMs)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(syncType))
        {
            return warehouseSyncComponent.sync(client, connectionCode, syncBatchId);
        }
        if (UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(syncType))
        {
            return logisticsChannelSyncComponent.sync(client, connectionCode, syncBatchId);
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
        {
            return skuInfoSyncComponent.sync(client, connectionCode, syncBatchId);
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            List<UpstreamSkuSyncItem> existingSkus = upstreamSystemMapper.selectSkuSyncList(connectionCode,
                UpstreamSystemConstants.STATUS_ACTIVE, null, null, null, null);
            return skuDimensionSyncComponent.syncFull(client, connectionCode, existingSkus, syncBatchId, rateLimitMs);
        }
        if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            UpstreamInventorySyncState previousState = upstreamSystemMapper.selectInventorySyncState(connectionCode);
            int count = inventorySyncComponent.sync(client, connectionCode, syncBatchId, previousState, startedTime);
            return simpleSyncItemResult(syncType, count);
        }
        throw new ServiceException("不支持的同步类型：" + syncType);
    }

    private void rebuildReadModel(String connectionCode, String syncType)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
        {
            sourceProductReadModelService.rebuildOfficialMaster();
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        }
    }

    private UpstreamSystemConnection selectEnabledConnection(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步");
        }
        return connection;
    }

    private UpstreamSystemConnection selectConnectionByCode(String connectionCode)
    {
        UpstreamSystemConnection connection = upstreamSystemMapper.selectConnectionByCode(connectionCode);
        if (connection == null)
        {
            throw new ServiceException("主仓接入不存在");
        }
        return connection;
    }

    private List<String> normalizeSyncTypes(List<String> syncTypes)
    {
        List<String> defaults = Arrays.asList(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, UpstreamSystemConstants.SYNC_TYPE_SKU);
        List<String> source = syncTypes == null || syncTypes.isEmpty() ? defaults : syncTypes;
        List<String> ordered = Arrays.asList(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, UpstreamSystemConstants.SYNC_TYPE_SKU,
            UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, UpstreamSystemConstants.SYNC_TYPE_INVENTORY);
        Set<String> requested = new HashSet<>();
        for (String syncType : source)
        {
            String normalized = StringUtils.trimToEmpty(syncType).toUpperCase();
            if (StringUtils.isNotBlank(normalized))
            {
                requested.add(normalized);
            }
        }
        List<String> result = new ArrayList<>();
        for (String syncType : ordered)
        {
            if (requested.contains(syncType))
            {
                result.add(syncType);
            }
        }
        return result;
    }

    private String normalizeSingleSyncType(String syncType)
    {
        String normalized = StringUtils.trimToEmpty(syncType).toUpperCase();
        List<String> allowed = Arrays.asList(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, UpstreamSystemConstants.SYNC_TYPE_SKU,
            UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, UpstreamSystemConstants.SYNC_TYPE_INVENTORY);
        if (!allowed.contains(normalized))
        {
            throw new ServiceException("不支持的同步类型：" + syncType);
        }
        return normalized;
    }

    private List<String> normalizeSelectedSkuList(List<String> skuList)
    {
        if (skuList == null || skuList.isEmpty())
        {
            throw new ServiceException("请选择或输入需要获取尺寸重量的SKU");
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String sku : skuList)
        {
            String normalized = StringUtils.trimToEmpty(sku);
            if (StringUtils.isBlank(normalized))
            {
                continue;
            }
            if (seen.add(normalized))
            {
                result.add(normalized);
            }
            if (result.size() > SKU_DIMENSION_SELECTED_LIMIT)
            {
                throw new ServiceException("指定SKU一次最多支持" + SKU_DIMENSION_SELECTED_LIMIT + "个");
            }
        }
        if (result.isEmpty())
        {
            throw new ServiceException("请选择或输入需要获取尺寸重量的SKU");
        }
        return result;
    }

    private void acquireSyncLock(String connectionCode)
    {
        if (!syncingConnectionCodes.add(connectionCode))
        {
            throw new ServiceException("该主仓正在同步，请稍后再试");
        }
    }

    private void releaseSyncLock(String connectionCode)
    {
        syncingConnectionCodes.remove(connectionCode);
    }

    private void mergeSyncItemResult(UpstreamSyncResult result, UpstreamSyncItemResult item)
    {
        if (item == null)
        {
            return;
        }
        result.getItems().add(item);
        if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(item.getSyncType()))
        {
            result.setWarehouseCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(item.getSyncType()))
        {
            result.setLogisticsChannelCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(item.getSyncType()))
        {
            result.setSkuCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(item.getSyncType()))
        {
            result.setSkuDimensionCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(item.getSyncType()))
        {
            result.setWarehouseStockCount(item.getCount());
        }
    }

    private UpstreamSyncItemResult simpleSyncItemResult(String syncType, int count)
    {
        UpstreamSyncItemResult item = new UpstreamSyncItemResult();
        item.setSyncType(syncType);
        item.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
        item.setPulledCount(count);
        item.setInsertedCount(count);
        item.setCount(count);
        return item;
    }

}
