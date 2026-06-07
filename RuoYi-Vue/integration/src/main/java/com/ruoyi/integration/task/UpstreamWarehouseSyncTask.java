package com.ruoyi.integration.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.service.IUpstreamSyncService;
import com.ruoyi.integration.support.UpstreamSystemConstants;

/**
 * 领星仓库清单定时同步入口。
 */
@Component("upstreamWarehouseSyncTask")
public class UpstreamWarehouseSyncTask
{
    @Autowired
    private IUpstreamSyncService upstreamSyncService;

    @Autowired
    private UpstreamScheduledSyncExecutor executor;

    public void sync()
    {
        executor.runForLingxingConnections("warehouse", connectionCode ->
            upstreamSyncService.syncScheduled(connectionCode, UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE)
                .getWarehouseCount());
    }
}
