package com.ruoyi.integration.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.service.IUpstreamSystemService;
import com.ruoyi.integration.support.UpstreamSystemConstants;

/**
 * 领星SKU仓库尺寸重量定时同步入口。
 */
@Component("upstreamSkuDimensionSyncTask")
public class UpstreamSkuDimensionSyncTask
{
    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @Autowired
    private UpstreamScheduledSyncExecutor executor;

    public void sync()
    {
        executor.runForLingxingConnections("SKU dimension", connectionCode ->
            upstreamSystemService.syncScheduled(connectionCode, UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION)
                .getSkuDimensionCount());
    }
}
