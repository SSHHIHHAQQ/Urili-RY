package com.ruoyi.integration.service;

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

    UpstreamSyncResult syncScheduled(String connectionCode, String syncType);

    UpstreamSyncResult syncWarehousesOnly(String connectionCode);

    UpstreamSyncResult syncLogisticsChannelsOnly(String connectionCode);

    UpstreamSyncResult syncSkuInfoOnly(String connectionCode);

    UpstreamSyncResult syncSkusOnly(String connectionCode);

    UpstreamSyncResult syncSkuDimensionsOnly(String connectionCode);

    UpstreamSyncResult syncSkuDimensionsBySkuList(String connectionCode, SkuDimensionSelectedSyncRequest request);

    UpstreamSyncResult syncWarehouseStocksOnly(String connectionCode);
}
