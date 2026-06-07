package com.ruoyi.integration.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSkuSyncState;
import com.ruoyi.integration.domain.UpstreamSyncBatch;
import com.ruoyi.integration.domain.UpstreamSyncState;
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
import com.ruoyi.integration.sync.UpstreamWarehouseSyncComponent;

/**
 * 上游系统同步执行服务实现。
 */
@Service
public class UpstreamSyncServiceImpl implements IUpstreamSyncService
{
    private static final int SKU_DIMENSION_FULL_RATE_LIMIT_MS = 2000;

    private static final int SKU_DIMENSION_SELECTED_LIMIT = 100;

    private static final long TEN_MINUTES_MS = 10 * 60 * 1000L;

    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000L;

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
            recordSyncState(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                UpstreamSystemConstants.SYNC_STATUS_SYNCING, syncBatchId, startedTime, null, null, null,
                0, 0, 0, "", "", UpstreamSystemConstants.SYNC_MODE_SELECTED, 0);
            insertSyncBatch(syncBatchId, connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                UpstreamSystemConstants.SYNC_MODE_SELECTED, startedTime);
            UpstreamSyncItemResult item = skuDimensionSyncComponent.syncSelected(client, connectionCode, skuList,
                syncBatchId);
            int dimensionCount = item.getCount();
            Date finishedTime = new Date();
            recordSyncState(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                UpstreamSystemConstants.SYNC_STATUS_FRESH, syncBatchId, startedTime, finishedTime, finishedTime,
                nextDailyTime(), dimensionCount, dimensionCount, 0, "", "",
                UpstreamSystemConstants.SYNC_MODE_SELECTED, 0);
            upstreamSystemMapper.updateConnectionSyncSummary(connectionCode);
            sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);

            UpstreamSyncResult result = new UpstreamSyncResult();
            result.setSyncBatchId(syncBatchId);
            result.setSkuDimensionCount(dimensionCount);
            result.getItems().add(item);
            updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FRESH, item, finishedTime, "");
            insertSyncExecutionLog(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                UpstreamSystemConstants.SYNC_MODE_SELECTED, syncBatchId, startedTime, finishedTime, "SUCCESS", item, "");
            return result;
        }
        catch (RuntimeException ex)
        {
            Date finishedTime = new Date();
            String message = StringUtils.left(ex.getMessage(), 500);
            if (syncBatchId != null && startedTime != null)
            {
                recordSyncState(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                    UpstreamSystemConstants.SYNC_STATUS_FAILED, syncBatchId, startedTime, finishedTime, null,
                    nextDailyTime(), 0, 0, 1, "", message, UpstreamSystemConstants.SYNC_MODE_SELECTED, 0);
                UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
                failed.setSyncType(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION);
                failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
                failed.setErrorMessage(message);
                updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FAILED, failed, finishedTime, message);
                insertSyncExecutionLog(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
                    UpstreamSystemConstants.SYNC_MODE_SELECTED, syncBatchId, startedTime, finishedTime,
                    "FAILURE", failed, message);
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
        recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_SYNCING, syncBatchId,
            startedTime, null, null, null, 0, 0, 0, "", "", mode, rateLimitMs);
        recordLegacySyncingState(connectionCode, syncType, syncBatchId, startedTime);
        insertSyncBatch(syncBatchId, connectionCode, syncType, mode, startedTime);

        try
        {
            UpstreamSyncItemResult item = doSyncItem(client, connectionCode, syncType, syncBatchId, startedTime,
                rateLimitMs);
            Date finishedTime = new Date();
            rebuildReadModel(connectionCode, syncType);
            recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_FRESH, syncBatchId,
                startedTime, finishedTime, finishedTime, nextSyncTime(syncType), item.getPulledCount(),
                item.getInsertedCount() + item.getChangedCount(), 0, "", "", mode, rateLimitMs);
            recordLegacySuccessState(connectionCode, syncType, syncBatchId, startedTime, finishedTime, item.getCount());
            updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FRESH, item, finishedTime, "");
            insertSyncExecutionLog(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
                "SUCCESS", item, "");
            return item;
        }
        catch (RuntimeException ex)
        {
            Date finishedTime = new Date();
            String message = StringUtils.left(ex.getMessage(), 500);
            recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_FAILED, syncBatchId,
                startedTime, finishedTime, null, nextSyncTime(syncType), 0, 0, 1, "", message, mode, rateLimitMs);
            recordLegacyFailedState(connectionCode, syncType, syncBatchId, startedTime, finishedTime, message);
            UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
            failed.setSyncType(syncType);
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setErrorMessage(message);
            updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FAILED, failed, finishedTime, message);
            insertSyncExecutionLog(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
                "FAILURE", failed, message);
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

    private void recordSyncState(String connectionCode, String syncType, String status, String syncBatchId,
        Date startedTime, Date finishedTime, Date successTime, Date nextSyncTime, int totalCount, int successCount,
        int failedCount, String errorCode, String errorMessage, String mode, int rateLimitMs)
    {
        UpstreamSyncState state = new UpstreamSyncState();
        state.setConnectionCode(connectionCode);
        state.setSyncType(syncType);
        state.setStatus(status);
        state.setSyncBatchId(syncBatchId);
        state.setLastStartedTime(startedTime);
        state.setLastFinishedTime(finishedTime);
        state.setLastSuccessTime(successTime);
        state.setNextSyncTime(nextSyncTime);
        state.setTotalCount(totalCount);
        state.setSuccessCount(successCount);
        state.setFailedCount(failedCount);
        state.setLastErrorCode(errorCode);
        state.setLastErrorMessage(StringUtils.left(StringUtils.defaultString(errorMessage), 500));
        state.setLastMode(mode);
        state.setRateLimitMs(rateLimitMs);
        upstreamSystemMapper.upsertSyncState(state);
    }

    private void recordLegacySyncingState(String connectionCode, String syncType, String syncBatchId, Date startedTime)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType)
            || UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            UpstreamSkuSyncState state = new UpstreamSkuSyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            upstreamSystemMapper.upsertSkuSyncState(state);
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            UpstreamInventorySyncState previous = upstreamSystemMapper.selectInventorySyncState(connectionCode);
            UpstreamInventorySyncState state = new UpstreamInventorySyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastSuccessTime(previous == null ? null : previous.getLastSuccessTime());
            state.setNextSyncTime(previous == null ? null : previous.getNextSyncTime());
            state.setTotalCount(previous == null ? 0 : previous.getTotalCount());
            state.setActiveCount(previous == null ? 0 : previous.getActiveCount());
            state.setMissingCount(previous == null ? 0 : previous.getMissingCount());
            state.setLastErrorMessage("");
            upstreamSystemMapper.upsertInventorySyncState(state);
        }
    }

    private void recordLegacySuccessState(String connectionCode, String syncType, String syncBatchId, Date startedTime,
        Date finishedTime, int count)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType)
            || UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            UpstreamSkuSyncState state = new UpstreamSkuSyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastSuccessTime(finishedTime);
            state.setNextSyncTime(nextSyncTime(syncType));
            state.setLastErrorMessage("");
            upstreamSystemMapper.upsertSkuSyncState(state);
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            UpstreamInventorySyncState state = new UpstreamInventorySyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastSuccessTime(finishedTime);
            state.setNextSyncTime(nextSyncTime(syncType));
            state.setTotalCount(count);
            state.setActiveCount(count);
            state.setMissingCount(0);
            state.setLastErrorMessage("");
            upstreamSystemMapper.upsertInventorySyncState(state);
        }
    }

    private void recordLegacyFailedState(String connectionCode, String syncType, String syncBatchId, Date startedTime,
        Date finishedTime, String message)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType)
            || UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            UpstreamSkuSyncState state = new UpstreamSkuSyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastErrorMessage(StringUtils.left(message, 500));
            upstreamSystemMapper.upsertSkuSyncState(state);
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            UpstreamInventorySyncState previous = upstreamSystemMapper.selectInventorySyncState(connectionCode);
            UpstreamInventorySyncState state = new UpstreamInventorySyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastSuccessTime(previous == null ? null : previous.getLastSuccessTime());
            state.setNextSyncTime(previous == null ? null : previous.getNextSyncTime());
            state.setTotalCount(previous == null ? 0 : previous.getTotalCount());
            state.setActiveCount(previous == null ? 0 : previous.getActiveCount());
            state.setMissingCount(previous == null ? 0 : previous.getMissingCount());
            state.setLastErrorMessage(StringUtils.left(message, 500));
            upstreamSystemMapper.upsertInventorySyncState(state);
        }
    }

    private void insertSyncBatch(String syncBatchId, String connectionCode, String syncType, String mode,
        Date startedTime)
    {
        UpstreamSyncBatch batch = new UpstreamSyncBatch();
        batch.setSyncBatchId(syncBatchId);
        batch.setConnectionCode(connectionCode);
        batch.setSyncType(syncType);
        batch.setMode(mode);
        batch.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
        batch.setStartedTime(startedTime);
        upstreamSystemMapper.insertSyncBatch(batch);
    }

    private void updateSyncBatch(String syncBatchId, String status, UpstreamSyncItemResult item, Date finishedTime,
        String errorMessage)
    {
        UpstreamSyncBatch batch = new UpstreamSyncBatch();
        batch.setSyncBatchId(syncBatchId);
        batch.setStatus(status);
        batch.setPulledCount(item.getPulledCount());
        batch.setInsertedCount(item.getInsertedCount());
        batch.setChangedCount(item.getChangedCount());
        batch.setUnchangedCount(item.getUnchangedCount());
        batch.setDisabledCount(item.getDisabledCount());
        batch.setFailedCount(UpstreamSystemConstants.SYNC_STATUS_FAILED.equals(status) ? 1 : 0);
        batch.setFinishedTime(finishedTime);
        batch.setErrorMessage(errorMessage);
        upstreamSystemMapper.updateSyncBatch(batch);
    }

    private void insertSyncExecutionLog(String connectionCode, String syncType, String mode, String syncBatchId,
        Date startedTime, Date finishedTime, String status, UpstreamSyncItemResult item, String errorMessage)
    {
        UpstreamRequestLog log = new UpstreamRequestLog();
        log.setConnectionCode(connectionCode);
        log.setTraceId(syncBatchId);
        log.setOperation(syncExecutionOperation(syncType));
        log.setEndpoint("upstream-sync://" + StringUtils.defaultString(mode).toLowerCase() + "/" + syncType);
        log.setRequestTime(startedTime);
        log.setResponseTime(finishedTime);
        log.setDurationMs(Math.max(0L, finishedTime.getTime() - startedTime.getTime()));
        log.setRequestPayloadRedacted(JSON.toJSONString(syncExecutionRequestPayload(syncBatchId, syncType, mode)));
        log.setResponsePayloadRedacted(JSON.toJSONString(syncExecutionResponsePayload(status, item, errorMessage)));
        log.setExternalErrorCode("SUCCESS".equals(status) ? "" : syncType + "_SYNC_FAILED");
        log.setExternalErrorMessage(StringUtils.left(StringUtils.defaultString(errorMessage), 500));
        log.setStatus(status);
        upstreamSystemMapper.insertRequestLog(log);
    }

    private Map<String, Object> syncExecutionRequestPayload(String syncBatchId, String syncType, String mode)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("syncBatchId", syncBatchId);
        payload.put("syncType", syncType);
        payload.put("mode", mode);
        return payload;
    }

    private Map<String, Object> syncExecutionResponsePayload(String status, UpstreamSyncItemResult item,
        String errorMessage)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        if (item != null)
        {
            payload.put("pulledCount", item.getPulledCount());
            payload.put("insertedCount", item.getInsertedCount());
            payload.put("changedCount", item.getChangedCount());
            payload.put("unchangedCount", item.getUnchangedCount());
            payload.put("disabledCount", item.getDisabledCount());
            payload.put("count", item.getCount());
        }
        if (StringUtils.isNotBlank(errorMessage))
        {
            payload.put("errorMessage", StringUtils.left(errorMessage, 500));
        }
        return payload;
    }

    private String syncExecutionOperation(String syncType)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_WAREHOUSE_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_LOGISTICS_CHANNEL_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_SKU_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_SKU_DIMENSION_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_INVENTORY_SYNC;
        }
        return syncType + "_SYNC_TASK";
    }

    private Date nextSyncTime(String syncType)
    {
        long now = System.currentTimeMillis();
        if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            return new Date(now + TEN_MINUTES_MS);
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            return nextDailyTime();
        }
        return new Date(now + ONE_DAY_MS);
    }

    private Date nextDailyTime()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis())
        {
            calendar.add(Calendar.DATE, 1);
        }
        return calendar.getTime();
    }
}
