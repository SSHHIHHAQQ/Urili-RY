package com.ruoyi.integration.domain.request;

import java.util.List;

/**
 * 指定SKU仓库尺寸重量同步请求。
 */
public class SkuDimensionSelectedSyncRequest
{
    private List<String> skuList;

    public List<String> getSkuList()
    {
        return skuList;
    }

    public void setSkuList(List<String> skuList)
    {
        this.skuList = skuList;
    }
}
