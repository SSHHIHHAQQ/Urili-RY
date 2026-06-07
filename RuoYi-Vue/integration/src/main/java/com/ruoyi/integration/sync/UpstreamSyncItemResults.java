package com.ruoyi.integration.sync;

import com.ruoyi.integration.domain.response.UpstreamSyncItemResult;
import com.ruoyi.integration.support.UpstreamSystemConstants;

final class UpstreamSyncItemResults
{
    private UpstreamSyncItemResults()
    {
    }

    static UpstreamSyncItemResult simple(String syncType, int count)
    {
        return diff(syncType, count, count, 0, 0, 0);
    }

    static UpstreamSyncItemResult diff(String syncType, int pulledCount, int insertedCount, int changedCount,
        int unchangedCount, int disabledCount)
    {
        UpstreamSyncItemResult item = new UpstreamSyncItemResult();
        item.setSyncType(syncType);
        item.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
        item.setPulledCount(pulledCount);
        item.setInsertedCount(insertedCount);
        item.setChangedCount(changedCount);
        item.setUnchangedCount(unchangedCount);
        item.setDisabledCount(disabledCount);
        item.setCount(insertedCount + changedCount);
        return item;
    }
}
