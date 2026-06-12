package com.ruoyi.integration.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.integration.domain.UpstreamSyncRequestRecord;
import com.ruoyi.integration.domain.UpstreamSyncTask;
import com.ruoyi.integration.domain.query.UpstreamSyncTaskQuery;

/**
 * 上游同步任务 Mapper。
 */
public interface UpstreamSyncTaskMapper
{
    int insertSyncRequest(UpstreamSyncRequestRecord request);

    int insertSyncTask(UpstreamSyncTask task);

    List<UpstreamSyncRequestRecord> selectSyncRequestList(UpstreamSyncTaskQuery query);

    List<UpstreamSyncTask> selectSyncTaskList(UpstreamSyncTaskQuery query);

    UpstreamSyncTask selectSyncTaskById(@Param("connectionCode") String connectionCode, @Param("taskId") Long taskId);

    List<UpstreamSyncTask> selectClaimableTasks(@Param("limit") int limit);

    List<UpstreamSyncTask> selectExpiredLeaseTasks(@Param("now") Date now);

    int claimSyncTask(@Param("taskId") Long taskId, @Param("leaseOwner") String leaseOwner,
        @Param("leaseUntil") Date leaseUntil);

    int markSyncTaskRunning(UpstreamSyncTask task);

    int completeSyncTask(UpstreamSyncTask task);

    int cancelSyncTask(@Param("connectionCode") String connectionCode, @Param("taskId") Long taskId,
        @Param("message") String message);

    int updateRequestSummary(@Param("requestNo") String requestNo);
}
