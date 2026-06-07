package com.ruoyi.inventory.domain.request;

/**
 * 库存总览调整请求。
 */
public class InventoryOverviewAdjustRequest
{
    private Long stockId;
    private String adjustField;
    private Long targetQty;
    private Boolean confirmed;
    private String reason;

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getAdjustField() { return adjustField; }
    public void setAdjustField(String adjustField) { this.adjustField = adjustField; }
    public Long getTargetQty() { return targetQty; }
    public void setTargetQty(Long targetQty) { this.targetQty = targetQty; }
    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
