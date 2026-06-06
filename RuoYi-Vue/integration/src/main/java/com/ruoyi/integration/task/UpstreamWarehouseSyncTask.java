package com.ruoyi.integration.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.service.IUpstreamSystemService;

/**
 * 领星仓库清单定时同步入口。
 */
@Component("upstreamWarehouseSyncTask")
public class UpstreamWarehouseSyncTask
{
    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @Autowired
    private UpstreamScheduledSyncExecutor executor;

    public void sync()
    {
        executor.runForLingxingConnections("warehouse", connectionCode ->
            upstreamSystemService.syncWarehousesOnly(connectionCode).getWarehouseCount());
    }
}
