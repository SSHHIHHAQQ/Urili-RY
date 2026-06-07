package com.ruoyi.integration.sync;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSkuSyncState;
import com.ruoyi.integration.domain.UpstreamSyncBatch;
import com.ruoyi.integration.domain.UpstreamSyncState;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.support.UpstreamSystemConstants;

@Component
public class UpstreamSyncStateRecorder
{
    private static final long TEN_MINUTES_MS = 10 * 60 * 1000L;

    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000L;

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    public void recordSyncing(String connectionCode, String syncType, String syncBatchId, Date startedTime,
        String mode, int rateLimitMs)
    {
        recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_SYNCING, syncBatchId,
            startedTime, null, null, null, 0, 0, 0, "", "", mode, rateLimitMs);
        recordLegacySyncingState(connectionCode, syncType, syncBatchId, startedTime);
        insertSyncBatch(syncBatchId, connectionCode, syncType, mode, startedTime);
    }

    public void recordSuccess(String connectionCode, String syncType, String mode, String syncBatchId,
        Date startedTime, Date finishedTime, UpstreamSyncItemResult item, int rateLimitMs)
    {
        recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_FRESH, syncBatchId,
            startedTime, finishedTime, finishedTime, nextSyncTime(syncType), item.getPulledCount(),
            item.getInsertedCount() + item.getChangedCount(), 0, "", "", mode, rateLimitMs);
        recordLegacySuccessState(connectionCode, syncType, syncBatchId, startedTime, finishedTime, item.getCount());
        updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FRESH, item, finishedTime, "");
        insertSyncExecutionLog(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
            "SUCCESS", item, "");
    }

    public void recordFailure(String connectionCode, String syncType, String mode, String syncBatchId,
        Date startedTime, Date finishedTime, UpstreamSyncItemResult item, String message, int rateLimitMs)
    {
        recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_FAILED, syncBatchId,
            startedTime, finishedTime, null, nextSyncTime(syncType), 0, 0, 1, "", message, mode, rateLimitMs);
        recordLegacyFailedState(connectionCode, syncType, syncBatchId, startedTime, finishedTime, message);
        updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FAILED, item, finishedTime, message);
        insertSyncExecutionLog(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
            "FAILURE", item, message);
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
