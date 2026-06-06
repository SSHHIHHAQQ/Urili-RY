package com.ruoyi.integration.domain.request;

import java.util.List;

/**
 * 上游系统分项同步请求。
 */
public class UpstreamSyncRequest
{
    private List<String> syncTypes;

    public List<String> getSyncTypes()
    {
        return syncTypes;
    }

    public void setSyncTypes(List<String> syncTypes)
    {
        this.syncTypes = syncTypes;
    }
}
