package com.ruoyi.integration.sync;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.domain.response.UpstreamSyncResult;
import com.ruoyi.integration.support.UpstreamSystemConstants;

/**
 * 手动同步的异步受理器：受理时写入同步中，后台串行执行，完成后写成功或失败。
 */
@Component
public class UpstreamManualSyncSubmitter
{
    private static final Logger log = LoggerFactory.getLogger(UpstreamManualSyncSubmitter.class);

    @Autowired
    @Qualifier("upstreamSyncTaskExecutor")
    private TaskExecutor syncTaskExecutor;

    @Autowired
    private UpstreamSyncStateRecorder syncStateRecorder;

    public UpstreamSyncResult submit(String connectionCode, List<String> syncTypes, String mode,
        Runnable acquireLock, Runnable releaseLock, SyncWorkRunner runner,
        Function<RuntimeException, ServiceException> exceptionMapper, Runnable onFinished)
    {
        List<SyncWorkItem> workItems = createSyncWorkItems(syncTypes);
        acquireLock.run();
        try
        {
            recordAcceptedItems(connectionCode, workItems, mode);
            syncTaskExecutor.execute(() -> execute(connectionCode, workItems, mode, releaseLock, runner,
                exceptionMapper, onFinished));
            return acceptedResult(workItems);
        }
        catch (RejectedExecutionException ex)
        {
            Date finishedTime = new Date();
            recordRejectedItems(connectionCode, workItems, mode, finishedTime, "同步队列繁忙，请稍后再试");
            releaseLock.run();
            throw new ServiceException("同步队列繁忙，请稍后再试");
        }
        catch (RuntimeException ex)
        {
            releaseLock.run();
            throw ex;
        }
    }

    private void execute(String connectionCode, List<SyncWorkItem> workItems, String mode, Runnable releaseLock,
        SyncWorkRunner runner, Function<RuntimeException, ServiceException> exceptionMapper, Runnable onFinished)
    {
        try
        {
            for (int i = 0; i < workItems.size(); i++)
            {
                SyncWorkItem workItem = workItems.get(i);
                try
                {
                    UpstreamSyncItemResult item = runner.run(workItem);
                    Date finishedTime = new Date();
                    syncStateRecorder.recordSuccess(connectionCode, workItem.getSyncType(), mode,
                        workItem.getSyncBatchId(), workItem.getStartedTime(), finishedTime, item,
                        workItem.getRateLimitMs());
                }
                catch (RuntimeException ex)
                {
                    Date finishedTime = new Date();
                    String message = StringUtils.left(ex.getMessage(), 500);
                    UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
                    failed.setSyncType(workItem.getSyncType());
                    failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
                    failed.setErrorMessage(message);
                    syncStateRecorder.recordFailure(connectionCode, workItem.getSyncType(), mode,
                        workItem.getSyncBatchId(), workItem.getStartedTime(), finishedTime, failed, message,
                        workItem.getRateLimitMs());
                    recordPendingFailures(connectionCode, workItems, i + 1, mode, finishedTime, message);
                    throw exceptionMapper.apply(ex);
                }
            }
            onFinished.run();
        }
        catch (RuntimeException ex)
        {
            try
            {
                onFinished.run();
            }
            catch (RuntimeException finishEx)
            {
                log.warn("Upstream async sync summary update failed, connectionCode={}", connectionCode, finishEx);
            }
            log.warn("Upstream async sync failed, connectionCode={}", connectionCode, ex);
        }
        finally
        {
            releaseLock.run();
        }
    }

    private List<SyncWorkItem> createSyncWorkItems(List<String> syncTypes)
    {
        Date startedTime = new Date();
        List<SyncWorkItem> workItems = new ArrayList<>();
        for (String syncType : syncTypes)
        {
            workItems.add(new SyncWorkItem(syncType, java.util.UUID.randomUUID().toString(), startedTime,
                rateLimitMs(syncType)));
        }
        return workItems;
    }

    private void recordAcceptedItems(String connectionCode, List<SyncWorkItem> workItems, String mode)
    {
        for (SyncWorkItem workItem : workItems)
        {
            syncStateRecorder.recordSyncing(connectionCode, workItem.getSyncType(), workItem.getSyncBatchId(),
                workItem.getStartedTime(), mode, workItem.getRateLimitMs());
        }
    }

    private void recordRejectedItems(String connectionCode, List<SyncWorkItem> workItems, String mode,
        Date finishedTime, String message)
    {
        for (SyncWorkItem workItem : workItems)
        {
            UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
            failed.setSyncType(workItem.getSyncType());
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setErrorMessage(message);
            syncStateRecorder.recordFailure(connectionCode, workItem.getSyncType(), mode, workItem.getSyncBatchId(),
                workItem.getStartedTime(), finishedTime, failed, message, workItem.getRateLimitMs());
        }
    }

    private void recordPendingFailures(String connectionCode, List<SyncWorkItem> workItems, int fromIndex, String mode,
        Date finishedTime, String sourceMessage)
    {
        if (fromIndex >= workItems.size())
        {
            return;
        }
        String message = StringUtils.left("前置同步失败，当前同步项未执行：" + StringUtils.defaultString(sourceMessage), 500);
        for (int i = fromIndex; i < workItems.size(); i++)
        {
            SyncWorkItem workItem = workItems.get(i);
            UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
            failed.setSyncType(workItem.getSyncType());
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setErrorMessage(message);
            syncStateRecorder.recordFailure(connectionCode, workItem.getSyncType(), mode, workItem.getSyncBatchId(),
                workItem.getStartedTime(), finishedTime, failed, message, workItem.getRateLimitMs());
        }
    }

    private UpstreamSyncResult acceptedResult(List<SyncWorkItem> workItems)
    {
        UpstreamSyncResult result = new UpstreamSyncResult();
        if (!workItems.isEmpty())
        {
            result.setSyncBatchId(workItems.get(0).getSyncBatchId());
        }
        for (SyncWorkItem workItem : workItems)
        {
            UpstreamSyncItemResult item = new UpstreamSyncItemResult();
            item.setSyncType(workItem.getSyncType());
            item.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
            result.getItems().add(item);
        }
        return result;
    }

    private int rateLimitMs(String syncType)
    {
        return UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType)
            ? UpstreamSystemConstants.SKU_DIMENSION_FULL_RATE_LIMIT_MS : 0;
    }

    @FunctionalInterface
    public interface SyncWorkRunner
    {
        UpstreamSyncItemResult run(SyncWorkItem workItem);
    }

    public static class SyncWorkItem
    {
        private final String syncType;
        private final String syncBatchId;
        private final Date startedTime;
        private final int rateLimitMs;

        private SyncWorkItem(String syncType, String syncBatchId, Date startedTime, int rateLimitMs)
        {
            this.syncType = syncType;
            this.syncBatchId = syncBatchId;
            this.startedTime = startedTime;
            this.rateLimitMs = rateLimitMs;
        }

        public String getSyncType()
        {
            return syncType;
        }

        public String getSyncBatchId()
        {
            return syncBatchId;
        }

        public Date getStartedTime()
        {
            return startedTime;
        }

        public int getRateLimitMs()
        {
            return rateLimitMs;
        }
    }
}
