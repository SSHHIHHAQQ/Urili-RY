package com.ruoyi.integration.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelSyncItem;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.lingxing.LingxingLogisticsChannel;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.support.UpstreamSystemConstants;

@Component
public class UpstreamLogisticsChannelSyncComponent
{
    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    public UpstreamSyncItemResult sync(LingxingOpenApiClient client, String connectionCode, String syncBatchId)
    {
        List<UpstreamWarehouseSyncItem> warehouses = upstreamSystemMapper.selectWarehouseSyncList(connectionCode,
            UpstreamSystemConstants.STATUS_ACTIVE);
        if (warehouses == null || warehouses.isEmpty())
        {
            throw new ServiceException("请先同步仓库，再同步物流渠道");
        }
        List<UpstreamLogisticsChannelSyncItem> items = new ArrayList<>();
        for (UpstreamWarehouseSyncItem warehouse : warehouses)
        {
            List<LingxingLogisticsChannel> channels = client.listLogisticsChannels(warehouse.getWarehouseCode(),
                UUID.randomUUID().toString());
            for (LingxingLogisticsChannel channel : channels)
            {
                UpstreamLogisticsChannelSyncItem item = new UpstreamLogisticsChannelSyncItem();
                item.setConnectionCode(connectionCode);
                item.setWarehouseCode(channel.getWarehouseCode());
                item.setChannelCode(channel.getChannelCode());
                item.setChannelName(channel.getChannelName());
                item.setSourcePayloadJson(channel.getSourcePayloadJson());
                item.setSourcePayloadHash(channel.getSourcePayloadHash());
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSyncBatchId(syncBatchId);
                items.add(item);
            }
        }
        if (!items.isEmpty())
        {
            upstreamSystemMapper.batchInsertLogisticsChannelStage(items);
        }
        int inserted = upstreamSystemMapper.insertNewLogisticsChannelsFromStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedLogisticsChannelsFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedLogisticsChannelsFromStage(connectionCode, syncBatchId);
        int disabled = upstreamSystemMapper.disableMissingLogisticsChannelsFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupLogisticsChannelStage(connectionCode, syncBatchId);
        return UpstreamSyncItemResults.diff(UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, items.size(),
            inserted, changed, unchanged, disabled);
    }
}
