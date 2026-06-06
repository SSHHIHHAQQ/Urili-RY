package com.ruoyi.integration.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 上游系统历史定时任务入口。
 *
 * @deprecated 新任务请使用独立组件：
 * upstreamWarehouseSyncTask / upstreamLogisticsChannelSyncTask /
 * upstreamSkuInfoSyncTask / upstreamSkuDimensionSyncTask / upstreamInventorySyncTask。
 */
@Deprecated
@Component("upstreamSystemTask")
public class UpstreamSystemTask
{
    @Autowired
    private UpstreamWarehouseSyncTask warehouseSyncTask;

    @Autowired
    private UpstreamLogisticsChannelSyncTask logisticsChannelSyncTask;

    @Autowired
    private UpstreamSkuInfoSyncTask skuInfoSyncTask;

    @Autowired
    private UpstreamSkuDimensionSyncTask skuDimensionSyncTask;

    @Autowired
    private UpstreamInventorySyncTask inventorySyncTask;

    public void syncWarehouses()
    {
        warehouseSyncTask.sync();
    }

    public void syncLogisticsChannels()
    {
        logisticsChannelSyncTask.sync();
    }

    public void syncSkuInfo()
    {
        skuInfoSyncTask.sync();
    }

    public void syncSkuDimensions()
    {
        skuDimensionSyncTask.sync();
    }

    public void syncSkus()
    {
        syncSkuInfo();
    }

    public void syncInventory()
    {
        inventorySyncTask.sync();
    }
}
