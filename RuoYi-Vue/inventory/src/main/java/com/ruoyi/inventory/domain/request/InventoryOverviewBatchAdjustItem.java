package com.ruoyi.inventory.domain.request;

/**
 * 库存总览批量调整明细。
 */
public class InventoryOverviewBatchAdjustItem
{
    private Long stockId;

    private Long targetPlatformTotalQty;

    private Long targetPlatformInTransitQty;

    public Long getStockId()
    {
        return stockId;
    }

    public void setStockId(Long stockId)
    {
        this.stockId = stockId;
    }

    public Long getTargetPlatformTotalQty()
    {
        return targetPlatformTotalQty;
    }

    public void setTargetPlatformTotalQty(Long targetPlatformTotalQty)
    {
        this.targetPlatformTotalQty = targetPlatformTotalQty;
    }

    public Long getTargetPlatformInTransitQty()
    {
        return targetPlatformInTransitQty;
    }

    public void setTargetPlatformInTransitQty(Long targetPlatformInTransitQty)
    {
        this.targetPlatformInTransitQty = targetPlatformInTransitQty;
    }
}
