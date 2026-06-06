package com.ruoyi.integration.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.service.IUpstreamSystemService;

/**
 * 领星物流渠道清单定时同步入口。
 */
@Component("upstreamLogisticsChannelSyncTask")
public class UpstreamLogisticsChannelSyncTask
{
    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @Autowired
    private UpstreamScheduledSyncExecutor executor;

    public void sync()
    {
        executor.runForLingxingConnections("logistics channel", connectionCode ->
            upstreamSystemService.syncLogisticsChannelsOnly(connectionCode).getLogisticsChannelCount());
    }
}
