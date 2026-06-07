package com.ruoyi.integration.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.lingxing.LingxingProductPage;
import com.ruoyi.integration.lingxing.LingxingProductSku;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.support.UpstreamSystemConstants;

@Component
public class UpstreamSkuInfoSyncComponent
{
    private static final int SKU_PAGE_SIZE = 100;

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    public UpstreamSyncItemResult sync(LingxingOpenApiClient client, String connectionCode, String syncBatchId)
    {
        int current = 1;
        int total = Integer.MAX_VALUE;
        int synced = 0;
        while ((current - 1) * SKU_PAGE_SIZE < total)
        {
            LingxingProductPage page = client.listProductSkuPage(current, SKU_PAGE_SIZE, UUID.randomUUID().toString());
            total = page.getTotal();
            List<LingxingProductSku> records = page.getRecords();
            if (records == null || records.isEmpty())
            {
                break;
            }
            List<UpstreamSkuSyncItem> items = new ArrayList<>();
            for (LingxingProductSku sku : records)
            {
                UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
                item.setConnectionCode(connectionCode);
                item.setMasterSku(sku.getSku());
                item.setMasterProductName(sku.getProductName());
                LingxingSkuSyncItemMapper.copyFields(item, sku);
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSearchText(LingxingSkuSyncItemMapper.buildSearchText(sku));
                item.setSyncBatchId(syncBatchId);
                items.add(item);
                synced++;
            }
            upstreamSystemMapper.batchInsertSkuStage(items);
            current++;
        }
        int inserted = upstreamSystemMapper.insertNewSkusFromStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedSkusFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedSkusFromStage(connectionCode, syncBatchId);
        int disabled = upstreamSystemMapper.disableMissingSkusFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupSkuStage(connectionCode, syncBatchId);
        return UpstreamSyncItemResults.diff(UpstreamSystemConstants.SYNC_TYPE_SKU, synced, inserted, changed,
            unchanged, disabled);
    }
}
