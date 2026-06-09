package com.ruoyi.inventory.domain.request;

import java.util.List;

/**
 * 库存总览批量调整请求。
 */
public class InventoryOverviewBatchAdjustRequest
{
    private List<InventoryOverviewBatchAdjustItem> items;

    private Boolean confirmed;

    private String reason;

    public List<InventoryOverviewBatchAdjustItem> getItems()
    {
        return items;
    }

    public void setItems(List<InventoryOverviewBatchAdjustItem> items)
    {
        this.items = items;
    }

    public Boolean getConfirmed()
    {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed)
    {
        this.confirmed = confirmed;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }
}
