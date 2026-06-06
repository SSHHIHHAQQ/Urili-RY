package com.ruoyi.integration.task;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.response.UpstreamSyncResult;
import com.ruoyi.integration.service.IUpstreamSystemService;
import com.ruoyi.integration.support.UpstreamSystemConstants;

/**
 * Quartz entrypoint for upstream system scheduled sync tasks.
 */
@Component("upstreamSystemTask")
public class UpstreamSystemTask
{
    private static final Logger log = LoggerFactory.getLogger(UpstreamSystemTask.class);

    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    public void syncWarehouses()
    {
        runForLingxingConnections("warehouse", connectionCode ->
            upstreamSystemService.syncWarehousesOnly(connectionCode).getWarehouseCount());
    }

    public void syncLogisticsChannels()
    {
        runForLingxingConnections("logistics channel", connectionCode ->
            upstreamSystemService.syncLogisticsChannelsOnly(connectionCode).getLogisticsChannelCount());
    }

    public void syncSkuInfo()
    {
        runForLingxingConnections("SKU info", connectionCode ->
            upstreamSystemService.syncSkuInfoOnly(connectionCode).getSkuCount());
    }

    public void syncSkuDimensions()
    {
        runForLingxingConnections("SKU dimension", connectionCode ->
            upstreamSystemService.syncSkuDimensionsOnly(connectionCode).getSkuDimensionCount());
    }

    public void syncSkus()
    {
        syncSkuInfo();
    }

    public void syncInventory()
    {
        runForLingxingConnections("inventory", connectionCode ->
            upstreamSystemService.syncWarehouseStocksOnly(connectionCode).getWarehouseStockCount());
    }

    /**
     * Keep scheduled upstream sync serial in this JVM. Quartz non-concurrent jobs are scoped by job key.
     */
    private synchronized void runForLingxingConnections(String taskName, SyncInvoker invoker)
    {
        UpstreamSystemConnection query = new UpstreamSystemConnection();
        query.setStatus(UpstreamSystemConstants.STATUS_ENABLED);
        List<UpstreamSystemConnection> connections = upstreamSystemService.selectConnectionList(query);

        int attempted = 0;
        int success = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        for (UpstreamSystemConnection connection : connections)
        {
            if (!isLingxingWms(connection))
            {
                skipped++;
                continue;
            }
            attempted++;
            String connectionCode = connection.getConnectionCode();
            try
            {
                int count = invoker.sync(connectionCode);
                success++;
                log.info("Upstream {} scheduled sync succeeded, connectionCode={}, count={}",
                    taskName, connectionCode, count);
            }
            catch (RuntimeException ex)
            {
                failures.add(connectionCode);
                log.warn("Upstream {} scheduled sync failed, connectionCode={}", taskName, connectionCode, ex);
            }
        }

        log.info("Upstream {} scheduled sync completed, attempted={}, success={}, skipped={}, failed={}",
            taskName, attempted, success, skipped, failures.size());
        if (!failures.isEmpty())
        {
            throw new ServiceException("Upstream " + taskName + " scheduled sync failed for " + failures.size()
                + " connection(s): " + String.join(",", failures));
        }
    }

    private boolean isLingxingWms(UpstreamSystemConnection connection)
    {
        String systemKind = connection.getSystemKind();
        return UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS.equals(systemKind)
            || UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS_LEGACY.equals(systemKind);
    }

    private interface SyncInvoker
    {
        int sync(String connectionCode);
    }
}
