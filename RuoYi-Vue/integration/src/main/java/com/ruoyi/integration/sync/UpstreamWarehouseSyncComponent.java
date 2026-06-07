package com.ruoyi.integration.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.lingxing.LingxingWarehouse;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.support.UpstreamSystemConstants;

@Component
public class UpstreamWarehouseSyncComponent
{
    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    public UpstreamSyncItemResult sync(LingxingOpenApiClient client, String connectionCode, String syncBatchId)
    {
        List<LingxingWarehouse> warehouses = client.listWarehouses(UUID.randomUUID().toString());
        List<UpstreamWarehouseSyncItem> items = new ArrayList<>();
        for (LingxingWarehouse warehouse : warehouses)
        {
            UpstreamWarehouseSyncItem item = new UpstreamWarehouseSyncItem();
            item.setConnectionCode(connectionCode);
            item.setWarehouseCode(warehouse.getWarehouseCode());
            item.setWarehouseName(warehouse.getWarehouseName());
            item.setCountryCode(warehouse.getCountryCode());
            item.setSourcePayloadJson(warehouse.getSourcePayloadJson());
            item.setSourcePayloadHash(warehouse.getSourcePayloadHash());
            item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
            item.setSyncBatchId(syncBatchId);
            items.add(item);
        }
        if (!items.isEmpty())
        {
            upstreamSystemMapper.batchInsertWarehouseStage(items);
        }
        int inserted = upstreamSystemMapper.insertNewWarehousesFromStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedWarehousesFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedWarehousesFromStage(connectionCode, syncBatchId);
        int disabled = upstreamSystemMapper.disableMissingWarehousesFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupWarehouseStage(connectionCode, syncBatchId);
        return UpstreamSyncItemResults.diff(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE, items.size(), inserted,
            changed, unchanged, disabled);
    }
}
