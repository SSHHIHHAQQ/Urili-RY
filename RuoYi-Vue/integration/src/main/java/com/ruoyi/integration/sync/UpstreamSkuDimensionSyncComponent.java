package com.ruoyi.integration.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.lingxing.LingxingProductPage;
import com.ruoyi.integration.lingxing.LingxingProductSku;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.support.UpstreamSystemConstants;

@Component
public class UpstreamSkuDimensionSyncComponent
{
    private static final int SKU_DIMENSION_BATCH_SIZE = 50;

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    public UpstreamSyncItemResult syncFull(LingxingOpenApiClient client, String connectionCode,
        List<UpstreamSkuSyncItem> skus, String syncBatchId, int rateLimitMs)
    {
        return sync(client, connectionCode, skus, syncBatchId,
            UpstreamSystemConstants.OP_SKU_DIMENSION_FULL_SYNC, rateLimitMs);
    }

    public UpstreamSyncItemResult syncSelected(LingxingOpenApiClient client, String connectionCode,
        List<String> skuCodes, String syncBatchId)
    {
        List<UpstreamSkuSyncItem> items = new ArrayList<>();
        for (String skuCode : skuCodes)
        {
            UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
            item.setConnectionCode(connectionCode);
            item.setMasterSku(skuCode);
            items.add(item);
        }
        return sync(client, connectionCode, items, syncBatchId,
            UpstreamSystemConstants.OP_SKU_DIMENSION_SELECTED_SYNC, 0);
    }

    private UpstreamSyncItemResult sync(LingxingOpenApiClient client, String connectionCode,
        List<UpstreamSkuSyncItem> skus, String syncBatchId, String operation, int rateLimitMs)
    {
        if (skus == null || skus.isEmpty())
        {
            return UpstreamSyncItemResults.diff(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, 0, 0, 0, 0, 0);
        }
        int pulled = 0;
        int start = 0;
        while (start < skus.size())
        {
            int end = Math.min(start + SKU_DIMENSION_BATCH_SIZE, skus.size());
            List<String> skuCodes = new ArrayList<>();
            for (int i = start; i < end; i++)
            {
                String masterSku = skus.get(i).getMasterSku();
                if (StringUtils.isNotBlank(masterSku))
                {
                    skuCodes.add(masterSku);
                }
            }
            if (!skuCodes.isEmpty())
            {
                pulled += syncBatch(client, connectionCode, syncBatchId, skuCodes, operation);
                sleepRateLimit(rateLimitMs);
            }
            start = end;
        }
        int inserted = upstreamSystemMapper.insertNewSkusFromDimensionStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedSkuDimensionsFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedSkuDimensionsFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupSkuDimensionStage(connectionCode, syncBatchId);
        return UpstreamSyncItemResults.diff(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, pulled, inserted,
            changed, unchanged, 0);
    }

    private int syncBatch(LingxingOpenApiClient client, String connectionCode, String syncBatchId,
        List<String> skuCodes, String operation)
    {
        List<UpstreamSkuSyncItem> items = new ArrayList<>();
        LingxingProductPage page = client.listProductSkuPageBySkuList(skuCodes, operation, UUID.randomUUID().toString());
        List<LingxingProductSku> records = page.getRecords();
        if (records == null)
        {
            return 0;
        }
        for (LingxingProductSku sku : records)
        {
            if (!LingxingSkuSyncItemMapper.hasAnyWmsDimension(sku))
            {
                continue;
            }
            UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
            item.setConnectionCode(connectionCode);
            item.setMasterSku(sku.getSku());
            item.setMasterProductName(sku.getProductName());
            LingxingSkuSyncItemMapper.copyFields(item, sku);
            item.setSyncBatchId(syncBatchId);
            items.add(item);
        }
        if (!items.isEmpty())
        {
            upstreamSystemMapper.batchInsertSkuDimensionStage(items);
        }
        return items.size();
    }

    private void sleepRateLimit(int rateLimitMs)
    {
        if (rateLimitMs <= 0)
        {
            return;
        }
        try
        {
            Thread.sleep(rateLimitMs);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("同步限速等待被中断");
        }
    }
}
