package com.ruoyi.inventory.domain;

/**
 * 库存同步策略预览明细行。
 */
public class InventoryStockSyncPolicyPreviewRow
{
    private Long stockId;
    private Long spuId;
    private Long skuId;
    private Long sellerId;
    private String sellerName;
    private String systemSkuCode;
    private String productName;
    private String skuName;
    private String warehouseName;
    private String warehouseKind;
    private String warehouseRefType;
    private Boolean eligible;
    private Boolean changed;
    private String message;
    private String beforeSyncMode;
    private String afterSyncMode;
    private String beforePolicyScope;
    private String afterPolicyScope;
    private Long sourceAvailableQty;
    private Long pendingSourceDeductionQty;
    private Long beforePlatformTotalQty;
    private Long afterPlatformTotalQty;
    private Long platformTotalDeltaQty;
    private Long beforeAvailableQty;
    private Long afterAvailableQty;
    private Long reservedQty;
    private String syncStatus;

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }
    public String getWarehouseRefType() { return warehouseRefType; }
    public void setWarehouseRefType(String warehouseRefType) { this.warehouseRefType = warehouseRefType; }
    public Boolean getEligible() { return eligible; }
    public void setEligible(Boolean eligible) { this.eligible = eligible; }
    public Boolean getChanged() { return changed; }
    public void setChanged(Boolean changed) { this.changed = changed; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getBeforeSyncMode() { return beforeSyncMode; }
    public void setBeforeSyncMode(String beforeSyncMode) { this.beforeSyncMode = beforeSyncMode; }
    public String getAfterSyncMode() { return afterSyncMode; }
    public void setAfterSyncMode(String afterSyncMode) { this.afterSyncMode = afterSyncMode; }
    public String getBeforePolicyScope() { return beforePolicyScope; }
    public void setBeforePolicyScope(String beforePolicyScope) { this.beforePolicyScope = beforePolicyScope; }
    public String getAfterPolicyScope() { return afterPolicyScope; }
    public void setAfterPolicyScope(String afterPolicyScope) { this.afterPolicyScope = afterPolicyScope; }
    public Long getSourceAvailableQty() { return sourceAvailableQty; }
    public void setSourceAvailableQty(Long sourceAvailableQty) { this.sourceAvailableQty = sourceAvailableQty; }
    public Long getPendingSourceDeductionQty() { return pendingSourceDeductionQty; }
    public void setPendingSourceDeductionQty(Long pendingSourceDeductionQty) { this.pendingSourceDeductionQty = pendingSourceDeductionQty; }
    public Long getBeforePlatformTotalQty() { return beforePlatformTotalQty; }
    public void setBeforePlatformTotalQty(Long beforePlatformTotalQty) { this.beforePlatformTotalQty = beforePlatformTotalQty; }
    public Long getAfterPlatformTotalQty() { return afterPlatformTotalQty; }
    public void setAfterPlatformTotalQty(Long afterPlatformTotalQty) { this.afterPlatformTotalQty = afterPlatformTotalQty; }
    public Long getPlatformTotalDeltaQty() { return platformTotalDeltaQty; }
    public void setPlatformTotalDeltaQty(Long platformTotalDeltaQty) { this.platformTotalDeltaQty = platformTotalDeltaQty; }
    public Long getBeforeAvailableQty() { return beforeAvailableQty; }
    public void setBeforeAvailableQty(Long beforeAvailableQty) { this.beforeAvailableQty = beforeAvailableQty; }
    public Long getAfterAvailableQty() { return afterAvailableQty; }
    public void setAfterAvailableQty(Long afterAvailableQty) { this.afterAvailableQty = afterAvailableQty; }
    public Long getReservedQty() { return reservedQty; }
    public void setReservedQty(Long reservedQty) { this.reservedQty = reservedQty; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
