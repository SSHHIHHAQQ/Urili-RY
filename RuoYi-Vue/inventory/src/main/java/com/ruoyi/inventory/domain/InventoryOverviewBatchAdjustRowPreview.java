package com.ruoyi.inventory.domain;

/**
 * 库存总览批量调整行预览。
 */
public class InventoryOverviewBatchAdjustRowPreview
{
    private Long stockId;

    private String systemSkuCode;

    private String warehouseName;

    private Boolean allowed;

    private String message;

    private Long beforePlatformTotalQty;

    private Long afterPlatformTotalQty;

    private Long platformTotalDeltaQty;

    private Long beforePlatformInTransitQty;

    private Long afterPlatformInTransitQty;

    private Long platformInTransitDeltaQty;

    private Long beforeAvailableQty;

    private Long afterAvailableQty;

    private Boolean reviewRequired;

    private Long requestedAdjustQty;

    private Long immediateReturnableQty;

    public Long getStockId()
    {
        return stockId;
    }

    public void setStockId(Long stockId)
    {
        this.stockId = stockId;
    }

    public String getSystemSkuCode()
    {
        return systemSkuCode;
    }

    public void setSystemSkuCode(String systemSkuCode)
    {
        this.systemSkuCode = systemSkuCode;
    }

    public String getWarehouseName()
    {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName)
    {
        this.warehouseName = warehouseName;
    }

    public Boolean getAllowed()
    {
        return allowed;
    }

    public void setAllowed(Boolean allowed)
    {
        this.allowed = allowed;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Long getBeforePlatformTotalQty()
    {
        return beforePlatformTotalQty;
    }

    public void setBeforePlatformTotalQty(Long beforePlatformTotalQty)
    {
        this.beforePlatformTotalQty = beforePlatformTotalQty;
    }

    public Long getAfterPlatformTotalQty()
    {
        return afterPlatformTotalQty;
    }

    public void setAfterPlatformTotalQty(Long afterPlatformTotalQty)
    {
        this.afterPlatformTotalQty = afterPlatformTotalQty;
    }

    public Long getPlatformTotalDeltaQty()
    {
        return platformTotalDeltaQty;
    }

    public void setPlatformTotalDeltaQty(Long platformTotalDeltaQty)
    {
        this.platformTotalDeltaQty = platformTotalDeltaQty;
    }

    public Long getBeforePlatformInTransitQty()
    {
        return beforePlatformInTransitQty;
    }

    public void setBeforePlatformInTransitQty(Long beforePlatformInTransitQty)
    {
        this.beforePlatformInTransitQty = beforePlatformInTransitQty;
    }

    public Long getAfterPlatformInTransitQty()
    {
        return afterPlatformInTransitQty;
    }

    public void setAfterPlatformInTransitQty(Long afterPlatformInTransitQty)
    {
        this.afterPlatformInTransitQty = afterPlatformInTransitQty;
    }

    public Long getPlatformInTransitDeltaQty()
    {
        return platformInTransitDeltaQty;
    }

    public void setPlatformInTransitDeltaQty(Long platformInTransitDeltaQty)
    {
        this.platformInTransitDeltaQty = platformInTransitDeltaQty;
    }

    public Long getBeforeAvailableQty()
    {
        return beforeAvailableQty;
    }

    public void setBeforeAvailableQty(Long beforeAvailableQty)
    {
        this.beforeAvailableQty = beforeAvailableQty;
    }

    public Long getAfterAvailableQty()
    {
        return afterAvailableQty;
    }

    public void setAfterAvailableQty(Long afterAvailableQty)
    {
        this.afterAvailableQty = afterAvailableQty;
    }

    public Boolean getReviewRequired()
    {
        return reviewRequired;
    }

    public void setReviewRequired(Boolean reviewRequired)
    {
        this.reviewRequired = reviewRequired;
    }

    public Long getRequestedAdjustQty()
    {
        return requestedAdjustQty;
    }

    public void setRequestedAdjustQty(Long requestedAdjustQty)
    {
        this.requestedAdjustQty = requestedAdjustQty;
    }

    public Long getImmediateReturnableQty()
    {
        return immediateReturnableQty;
    }

    public void setImmediateReturnableQty(Long immediateReturnableQty)
    {
        this.immediateReturnableQty = immediateReturnableQty;
    }
}
