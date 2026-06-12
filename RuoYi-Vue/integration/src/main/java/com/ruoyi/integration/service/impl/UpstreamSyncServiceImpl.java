package com.ruoyi.integration.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.inventory.service.IInventoryOverviewService;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSyncRequestRecord;
import com.ruoyi.integration.domain.UpstreamSyncTask;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.query.UpstreamSyncTaskQuery;
import com.ruoyi.integration.domain.request.SkuDimensionSelectedSyncRequest;
import com.ruoyi.integration.domain.request.UpstreamSyncRequest;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.domain.response.UpstreamSyncResult;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.mapper.UpstreamSyncTaskMapper;
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
    private static final int SKU_DIMENSION_SELECTED_LIMIT = 100;

    private static final int DISPATCH_LIMIT = 5;

    private final Set<String> syncingConnectionCodes = ConcurrentHashMap.newKeySet();

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Autowired
    private UpstreamSyncTaskMapper upstreamSyncTaskMapper;

    @Autowired
    private SourceProductReadModelService sourceProductReadModelService;

    @Autowired
    private SourceWarehouseStockReadModelService sourceWarehouseStockReadModelService;

    @Autowired
    private ObjectProvider<IInventoryOverviewService> inventoryOverviewService;

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
    public UpstreamSyncResult submitSelected(String connectionCode, UpstreamSyncRequest request)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        List<String> syncTypes = normalizeSyncTypes(request == null ? null : request.getSyncTypes());
        if (syncTypes.isEmpty())
        {
            throw new ServiceException("请选择需要同步的内容");
        }
        return enqueueSyncTasks(connection, syncTypes, UpstreamSystemConstants.SYNC_MODE_MANUAL,
            UpstreamSystemConstants.SYNC_TRIGGER_MANUAL, currentUsername(), null);
    }

    @Override
    public UpstreamSyncResult syncScheduled(String connectionCode, String syncType)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        return enqueueSyncTasks(connection, List.of(normalizeSingleSyncType(syncType)),
            UpstreamSystemConstants.SYNC_MODE_SCHEDULED, UpstreamSystemConstants.SYNC_TRIGGER_SCHEDULED,
            "system", null);
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
    public UpstreamSyncResult submitSkusOnly(String connectionCode)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        return enqueueSyncTasks(connection, List.of(UpstreamSystemConstants.SYNC_TYPE_SKU),
            UpstreamSystemConstants.SYNC_MODE_MANUAL, UpstreamSystemConstants.SYNC_TRIGGER_MANUAL,
            currentUsername(), null);
    }

    @Override
    public UpstreamSyncResult syncSkuDimensionsOnly(String connectionCode)
    {
        return syncSingleType(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION,
            UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    @Override
    public UpstreamSyncResult submitSkuDimensionsOnly(String connectionCode)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        return enqueueSyncTasks(connection, List.of(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION),
            UpstreamSystemConstants.SYNC_MODE_MANUAL, UpstreamSystemConstants.SYNC_TRIGGER_MANUAL,
            currentUsername(), null);
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
    public UpstreamSyncResult submitSkuDimensionsBySkuList(String connectionCode, SkuDimensionSelectedSyncRequest request)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        List<String> skuList = normalizeSelectedSkuList(request == null ? null : request.getSkuList());
        return enqueueSyncTasks(connection, List.of(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION),
            UpstreamSystemConstants.SYNC_MODE_SELECTED, UpstreamSystemConstants.SYNC_TRIGGER_MANUAL,
            currentUsername(), skuList);
    }

    @Override
    public UpstreamSyncResult syncWarehouseStocksOnly(String connectionCode)
    {
        return syncSingleType(connectionCode, UpstreamSystemConstants.SYNC_TYPE_INVENTORY,
            UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    @Override
    public UpstreamSyncResult submitWarehouseStocksOnly(String connectionCode)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        return enqueueSyncTasks(connection, List.of(UpstreamSystemConstants.SYNC_TYPE_INVENTORY),
            UpstreamSystemConstants.SYNC_MODE_MANUAL, UpstreamSystemConstants.SYNC_TRIGGER_MANUAL,
            currentUsername(), null);
    }

    @Override
    public List<UpstreamSyncRequestRecord> selectSyncRequestList(UpstreamSyncTaskQuery query)
    {
        return upstreamSyncTaskMapper.selectSyncRequestList(query);
    }

    @Override
    public List<UpstreamSyncTask> selectSyncTaskList(UpstreamSyncTaskQuery query)
    {
        return upstreamSyncTaskMapper.selectSyncTaskList(query);
    }

    @Override
    public UpstreamSyncResult retrySyncTask(String connectionCode, Long taskId)
    {
        UpstreamSyncTask task = selectSyncTaskForAction(connectionCode, taskId);
        if (!isRetriableTaskStatus(task.getStatus()))
        {
            throw new ServiceException("Only failed, timed out, skipped, or canceled sync tasks can be retried");
        }
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        List<String> selectedSkus = UpstreamSystemConstants.SYNC_MODE_SELECTED.equals(task.getMode())
            ? selectedSkuListFromPayload(task.getPayloadRedacted()) : null;
        return enqueueSyncTasks(connection, List.of(task.getSyncType()), task.getMode(),
            UpstreamSystemConstants.SYNC_TRIGGER_MANUAL, currentUsername(), selectedSkus);
    }

    @Override
    public int cancelSyncTask(String connectionCode, Long taskId)
    {
        UpstreamSyncTask task = selectSyncTaskForAction(connectionCode, taskId);
        int affected = upstreamSyncTaskMapper.cancelSyncTask(connectionCode, taskId, "管理端取消同步任务");
        if (affected > 0)
        {
            upstreamSyncTaskMapper.updateRequestSummary(task.getRequestNo());
        }
        return affected;
    }

    @Override
    public int dispatchPendingTasks()
    {
        int handled = recoverExpiredLeaseTasks();
        List<UpstreamSyncTask> tasks = upstreamSyncTaskMapper.selectClaimableTasks(DISPATCH_LIMIT);
        for (UpstreamSyncTask task : tasks)
        {
            Date leaseUntil = minutesFromNow(leaseMinutes(task.getSyncType()));
            if (upstreamSyncTaskMapper.claimSyncTask(task.getTaskId(), dispatcherOwner(), leaseUntil) == 0)
            {
                continue;
            }
            task.setLeaseUntil(leaseUntil);
            executeClaimedTask(task);
            handled++;
        }
        return handled;
    }

    private UpstreamSyncResult enqueueSyncTasks(UpstreamSystemConnection connection, List<String> syncTypes,
        String mode, String triggerSource, String submittedBy, List<String> selectedSkuList)
    {
        String requestNo = UUID.randomUUID().toString();
        Date submittedTime = new Date();
        UpstreamSyncRequestRecord request = new UpstreamSyncRequestRecord();
        request.setRequestNo(requestNo);
        request.setConnectionCode(connection.getConnectionCode());
        request.setTriggerSource(triggerSource);
        request.setMode(mode);
        request.setRequestedSyncTypes(StringUtils.join(syncTypes, ","));
        request.setStatus(UpstreamSystemConstants.SYNC_TASK_STATUS_PENDING);
        request.setSubmittedBy(StringUtils.defaultIfBlank(submittedBy, "system"));
        request.setSubmittedTime(submittedTime);
        request.setTaskCount(syncTypes.size());
        request.setRemark("RuoYi dispatcher accepted upstream sync request");
        upstreamSyncTaskMapper.insertSyncRequest(request);

        UpstreamSyncResult result = new UpstreamSyncResult();
        result.setRequestId(request.getRequestId());
        result.setRequestNo(requestNo);
        result.setTaskCount(syncTypes.size());
        for (String syncType : syncTypes)
        {
            UpstreamSyncTask task = new UpstreamSyncTask();
            task.setRequestNo(requestNo);
            task.setSyncBatchId(UUID.randomUUID().toString());
            task.setConnectionCode(connection.getConnectionCode());
            task.setSyncType(syncType);
            task.setMode(mode);
            task.setTriggerSource(triggerSource);
            task.setStatus(UpstreamSystemConstants.SYNC_TASK_STATUS_PENDING);
            task.setPriority(priorityForSyncType(syncType));
            task.setPayloadRedacted(payloadFor(syncType, mode, selectedSkuList));
            task.setAttemptCount(0);
            task.setMaxAttempts(1);
            task.setDeadlineAt(minutesFrom(submittedTime, deadlineMinutes(syncType)));
            task.setTraceId(UUID.randomUUID().toString());
            task.setSysJobInvokeTarget("upstreamSyncDispatchTask.dispatch");
            task.setRemark("accepted");
            upstreamSyncTaskMapper.insertSyncTask(task);
            mergeAcceptedTask(result, task);
        }
        upstreamSyncTaskMapper.updateRequestSummary(requestNo);
        return result;
    }

    private int recoverExpiredLeaseTasks()
    {
        int handled = 0;
        List<UpstreamSyncTask> expiredTasks = upstreamSyncTaskMapper.selectExpiredLeaseTasks(new Date());
        for (UpstreamSyncTask task : expiredTasks)
        {
            Date finishedTime = new Date();
            boolean wasRunning = UpstreamSystemConstants.SYNC_TASK_STATUS_RUNNING.equals(task.getStatus());
            UpstreamSyncItemResult failed = failedItem(task, "任务租约超时，已由恢复器收口");
            task.setStatus(UpstreamSystemConstants.SYNC_TASK_STATUS_TIMEOUT);
            task.setFinishedTime(finishedTime);
            task.setPulledCount(0);
            task.setInsertedCount(0);
            task.setChangedCount(0);
            task.setUnchangedCount(0);
            task.setDisabledCount(0);
            task.setFailedCount(1);
            task.setErrorCode("SYNC_TASK_TIMEOUT");
            task.setErrorMessage("任务租约超时，已由恢复器收口");
            upstreamSyncTaskMapper.completeSyncTask(task);
            if (wasRunning)
            {
                syncStateRecorder.recordFailure(task.getConnectionCode(), task.getSyncType(), task.getMode(),
                    task.getSyncBatchId(), task.getStartedTime(), finishedTime, failed, task.getErrorMessage(),
                    rateLimitMs(task.getSyncType()));
            }
            upstreamSyncTaskMapper.updateRequestSummary(task.getRequestNo());
            upstreamSystemMapper.updateConnectionSyncSummary(task.getConnectionCode());
            handled++;
        }
        return handled;
    }

    private void executeClaimedTask(UpstreamSyncTask task)
    {
        Date startedTime = new Date();
        task.setStartedTime(startedTime);
        task.setTraceId(StringUtils.defaultIfBlank(task.getTraceId(), UUID.randomUUID().toString()));
        task.setLeaseUntil(minutesFrom(startedTime, leaseMinutes(task.getSyncType())));
        if (upstreamSyncTaskMapper.markSyncTaskRunning(task) == 0)
        {
            return;
        }

        boolean syncBatchRecorded = false;
        try
        {
            UpstreamSystemConnection connection = selectEnabledConnection(task.getConnectionCode());
            acquireSyncLock(task.getConnectionCode());
            try
            {
                int rateLimitMs = rateLimitMs(task.getSyncType());
                LingxingOpenApiClient client = lingxingClientFactory.createClient(connection);
                syncStateRecorder.recordSyncing(task.getConnectionCode(), task.getSyncType(), task.getSyncBatchId(),
                    startedTime, task.getMode(), rateLimitMs);
                syncBatchRecorded = true;
                UpstreamSyncItemResult item = executeTaskItem(client, task, startedTime, rateLimitMs);
                Date finishedTime = new Date();
                rebuildReadModel(task.getConnectionCode(), task.getSyncType());
                syncStateRecorder.recordSuccess(task.getConnectionCode(), task.getSyncType(), task.getMode(),
                    task.getSyncBatchId(), startedTime, finishedTime, item, rateLimitMs);
                completeTask(task, UpstreamSystemConstants.SYNC_TASK_STATUS_SUCCESS, finishedTime, item, "", "");
            }
            finally
            {
                releaseSyncLock(task.getConnectionCode());
            }
        }
        catch (RuntimeException ex)
        {
            Date finishedTime = new Date();
            String message = StringUtils.left(StringUtils.defaultIfBlank(ex.getMessage(), "上游同步任务执行失败"), 500);
            String taskStatus = message.contains("正在同步")
                ? UpstreamSystemConstants.SYNC_TASK_STATUS_SKIPPED : UpstreamSystemConstants.SYNC_TASK_STATUS_FAILED;
            UpstreamSyncItemResult failed = failedItem(task, message);
            if (syncBatchRecorded)
            {
                syncStateRecorder.recordFailure(task.getConnectionCode(), task.getSyncType(), task.getMode(),
                    task.getSyncBatchId(), startedTime, finishedTime, failed, message, rateLimitMs(task.getSyncType()));
            }
            completeTask(task, taskStatus, finishedTime, failed,
                UpstreamSystemConstants.SYNC_TASK_STATUS_SKIPPED.equals(taskStatus) ? "SYNC_TASK_SKIPPED" : "SYNC_TASK_FAILED",
                message);
        }
        finally
        {
            upstreamSyncTaskMapper.updateRequestSummary(task.getRequestNo());
            upstreamSystemMapper.updateConnectionSyncSummary(task.getConnectionCode());
        }
    }

    private UpstreamSyncTask selectSyncTaskForAction(String connectionCode, Long taskId)
    {
        UpstreamSyncTask task = upstreamSyncTaskMapper.selectSyncTaskById(connectionCode, taskId);
        if (task == null)
        {
            throw new ServiceException("同步任务不存在");
        }
        return task;
    }

    private boolean isRetriableTaskStatus(String status)
    {
        return Set.of(UpstreamSystemConstants.SYNC_TASK_STATUS_FAILED, UpstreamSystemConstants.SYNC_TASK_STATUS_TIMEOUT,
            UpstreamSystemConstants.SYNC_TASK_STATUS_SKIPPED, UpstreamSystemConstants.SYNC_TASK_STATUS_CANCELED)
            .contains(status);
    }

    private UpstreamSyncItemResult executeTaskItem(LingxingOpenApiClient client, UpstreamSyncTask task,
        Date startedTime, int rateLimitMs)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(task.getSyncType())
            && UpstreamSystemConstants.SYNC_MODE_SELECTED.equals(task.getMode()))
        {
            UpstreamSyncItemResult item = skuDimensionSyncComponent.syncSelected(client, task.getConnectionCode(),
                selectedSkuListFromPayload(task.getPayloadRedacted()), task.getSyncBatchId());
            item.setTaskId(task.getTaskId());
            item.setSyncBatchId(task.getSyncBatchId());
            return item;
        }
        UpstreamSyncItemResult item = doSyncItem(client, task.getConnectionCode(), task.getSyncType(),
            task.getSyncBatchId(), startedTime, rateLimitMs);
        item.setTaskId(task.getTaskId());
        item.setSyncBatchId(task.getSyncBatchId());
        return item;
    }

    private void completeTask(UpstreamSyncTask task, String status, Date finishedTime, UpstreamSyncItemResult item,
        String errorCode, String errorMessage)
    {
        task.setStatus(status);
        task.setFinishedTime(finishedTime);
        task.setPulledCount(item == null ? 0 : item.getPulledCount());
        task.setInsertedCount(item == null ? 0 : item.getInsertedCount());
        task.setChangedCount(item == null ? 0 : item.getChangedCount());
        task.setUnchangedCount(item == null ? 0 : item.getUnchangedCount());
        task.setDisabledCount(item == null ? 0 : item.getDisabledCount());
        task.setFailedCount(UpstreamSystemConstants.SYNC_TASK_STATUS_SUCCESS.equals(status) ? 0 : 1);
        task.setErrorCode(errorCode);
        task.setErrorMessage(StringUtils.left(StringUtils.defaultString(errorMessage), 500));
        upstreamSyncTaskMapper.completeSyncTask(task);
    }

    private UpstreamSyncItemResult failedItem(UpstreamSyncTask task, String message)
    {
        UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
        failed.setTaskId(task.getTaskId());
        failed.setSyncBatchId(task.getSyncBatchId());
        failed.setSyncType(task.getSyncType());
        failed.setStatus(UpstreamSystemConstants.SYNC_TASK_STATUS_FAILED);
        failed.setErrorMessage(StringUtils.left(StringUtils.defaultString(message), 500));
        return failed;
    }

    private void mergeAcceptedTask(UpstreamSyncResult result, UpstreamSyncTask task)
    {
        if (StringUtils.isBlank(result.getSyncBatchId()))
        {
            result.setSyncBatchId(task.getSyncBatchId());
        }
        UpstreamSyncItemResult item = new UpstreamSyncItemResult();
        item.setTaskId(task.getTaskId());
        item.setSyncBatchId(task.getSyncBatchId());
        item.setSyncType(task.getSyncType());
        item.setStatus(UpstreamSystemConstants.SYNC_TASK_STATUS_PENDING);
        result.getItems().add(item);
    }

    private String payloadFor(String syncType, String mode, List<String> selectedSkuList)
    {
        if (!UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType)
            || !UpstreamSystemConstants.SYNC_MODE_SELECTED.equals(mode))
        {
            return "";
        }
        JSONObject payload = new JSONObject();
        payload.put("skuList", selectedSkuList == null ? List.of() : selectedSkuList);
        return JSON.toJSONString(payload);
    }

    private List<String> selectedSkuListFromPayload(String payloadRedacted)
    {
        if (StringUtils.isBlank(payloadRedacted))
        {
            throw new ServiceException("指定SKU任务缺少SKU列表");
        }
        JSONObject payload = JSON.parseObject(payloadRedacted);
        if (payload == null || payload.getJSONArray("skuList") == null)
        {
            throw new ServiceException("指定SKU任务缺少SKU列表");
        }
        return normalizeSelectedSkuList(payload.getJSONArray("skuList").toJavaList(String.class));
    }

    private int priorityForSyncType(String syncType)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            return 30;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(syncType))
        {
            return 40;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(syncType))
        {
            return 50;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
        {
            return 60;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            return 90;
        }
        return 100;
    }

    private int deadlineMinutes(String syncType)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            return 180;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
        {
            return 60;
        }
        return 30;
    }

    private int leaseMinutes(String syncType)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            return 180;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
        {
            return 60;
        }
        return 30;
    }

    private int rateLimitMs(String syncType)
    {
        return UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType)
            ? UpstreamSystemConstants.SKU_DIMENSION_FULL_RATE_LIMIT_MS : 0;
    }

    private Date minutesFromNow(int minutes)
    {
        return minutesFrom(new Date(), minutes);
    }

    private Date minutesFrom(Date date, int minutes)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    private String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException ex)
        {
            return "system";
        }
    }

    private String dispatcherOwner()
    {
        String host = "unknown";
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ignored)
        {
        }
        return host + ":" + ProcessHandle.current().pid();
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
            ? UpstreamSystemConstants.SKU_DIMENSION_FULL_RATE_LIMIT_MS : 0;
        syncStateRecorder.recordSyncing(connectionCode, syncType, syncBatchId, startedTime, mode, rateLimitMs);

        try
        {
            UpstreamSyncItemResult item = doSyncItem(client, connectionCode, syncType, syncBatchId, startedTime,
                rateLimitMs);
            item.setSyncBatchId(syncBatchId);
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
            failed.setSyncBatchId(syncBatchId);
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
            List<Long> affectedSpuIds = selectSourceInventoryOverviewSpuIds(connectionCode);
            sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode);
            refreshSourceInventoryOverview(connectionCode, affectedSpuIds);
        }
    }

    private void refreshSourceInventoryOverview(String connectionCode)
    {
        refreshSourceInventoryOverview(connectionCode, null);
    }

    private void refreshSourceInventoryOverview(String connectionCode, List<Long> affectedSpuIds)
    {
        IInventoryOverviewService overviewService = inventoryOverviewService.getIfAvailable();
        if (overviewService != null)
        {
            overviewService.refreshSourceInventoryOverviewByConnection(connectionCode, affectedSpuIds);
        }
    }

    private List<Long> selectSourceInventoryOverviewSpuIds(String connectionCode)
    {
        IInventoryOverviewService overviewService = inventoryOverviewService.getIfAvailable();
        if (overviewService == null)
        {
            return new ArrayList<>();
        }
        return overviewService.selectSourceInventoryOverviewSpuIdsByConnection(connectionCode);
    }

    private UpstreamSystemConnection selectEnabledConnection(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步");
        }
        if (!UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED.equals(connection.getCredentialStatus()))
        {
            throw new ServiceException("主仓授权未通过，请先校验授权");
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
        if (StringUtils.isBlank(result.getSyncBatchId()))
        {
            result.setSyncBatchId(item.getSyncBatchId());
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
