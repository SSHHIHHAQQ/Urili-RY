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

    public void syncSkus()
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
                UpstreamSyncResult result = upstreamSystemService.syncSkusOnly(connectionCode);
                success++;
                log.info("Upstream SKU scheduled sync succeeded, connectionCode={}, skuCount={}, skuDimensionCount={}",
                    connectionCode, result.getSkuCount(), result.getSkuDimensionCount());
            }
            catch (RuntimeException ex)
            {
                failures.add(connectionCode);
                log.warn("Upstream SKU scheduled sync failed, connectionCode={}", connectionCode, ex);
            }
        }

        log.info("Upstream SKU scheduled sync completed, attempted={}, success={}, skipped={}, failed={}",
            attempted, success, skipped, failures.size());
        if (!failures.isEmpty())
        {
            throw new ServiceException("Upstream SKU scheduled sync failed for " + failures.size()
                + " connection(s): " + String.join(",", failures));
        }
    }

    public void syncInventory()
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
                UpstreamSyncResult result = upstreamSystemService.syncWarehouseStocksOnly(connectionCode);
                success++;
                log.info("Upstream inventory scheduled sync succeeded, connectionCode={}, warehouseStockCount={}",
                    connectionCode, result.getWarehouseStockCount());
            }
            catch (RuntimeException ex)
            {
                failures.add(connectionCode);
                log.warn("Upstream inventory scheduled sync failed, connectionCode={}", connectionCode, ex);
            }
        }

        log.info("Upstream inventory scheduled sync completed, attempted={}, success={}, skipped={}, failed={}",
            attempted, success, skipped, failures.size());
        if (!failures.isEmpty())
        {
            throw new ServiceException("Upstream inventory scheduled sync failed for " + failures.size()
                + " connection(s): " + String.join(",", failures));
        }
    }

    private boolean isLingxingWms(UpstreamSystemConnection connection)
    {
        String systemKind = connection.getSystemKind();
        return UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS.equals(systemKind)
            || UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS_LEGACY.equals(systemKind);
    }
}
