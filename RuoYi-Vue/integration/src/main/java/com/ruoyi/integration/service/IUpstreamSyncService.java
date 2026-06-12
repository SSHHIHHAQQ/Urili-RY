package com.ruoyi.integration.service;

import java.util.List;
import com.ruoyi.integration.domain.UpstreamSyncRequestRecord;
import com.ruoyi.integration.domain.UpstreamSyncTask;
import com.ruoyi.integration.domain.query.UpstreamSyncTaskQuery;
import com.ruoyi.integration.domain.request.SkuDimensionSelectedSyncRequest;
import com.ruoyi.integration.domain.request.UpstreamSyncRequest;
import com.ruoyi.integration.domain.response.UpstreamSyncResult;

/**
 * 上游系统同步执行服务。
 */
public interface IUpstreamSyncService
{
    UpstreamSyncResult syncAll(String connectionCode);

    UpstreamSyncResult syncSelected(String connectionCode, UpstreamSyncRequest request);

    UpstreamSyncResult submitSelected(String connectionCode, UpstreamSyncRequest request);

    UpstreamSyncResult syncScheduled(String connectionCode, String syncType);

    UpstreamSyncResult syncWarehousesOnly(String connectionCode);

    UpstreamSyncResult syncLogisticsChannelsOnly(String connectionCode);

    UpstreamSyncResult syncSkuInfoOnly(String connectionCode);

    UpstreamSyncResult syncSkusOnly(String connectionCode);

    UpstreamSyncResult submitSkusOnly(String connectionCode);

    UpstreamSyncResult syncSkuDimensionsOnly(String connectionCode);

    UpstreamSyncResult submitSkuDimensionsOnly(String connectionCode);

    UpstreamSyncResult syncSkuDimensionsBySkuList(String connectionCode, SkuDimensionSelectedSyncRequest request);

    UpstreamSyncResult submitSkuDimensionsBySkuList(String connectionCode, SkuDimensionSelectedSyncRequest request);

    UpstreamSyncResult syncWarehouseStocksOnly(String connectionCode);

    UpstreamSyncResult submitWarehouseStocksOnly(String connectionCode);

    List<UpstreamSyncRequestRecord> selectSyncRequestList(UpstreamSyncTaskQuery query);

    List<UpstreamSyncTask> selectSyncTaskList(UpstreamSyncTaskQuery query);

    UpstreamSyncResult retrySyncTask(String connectionCode, Long taskId);

    int cancelSyncTask(String connectionCode, Long taskId);

    int dispatchPendingTasks();
}
