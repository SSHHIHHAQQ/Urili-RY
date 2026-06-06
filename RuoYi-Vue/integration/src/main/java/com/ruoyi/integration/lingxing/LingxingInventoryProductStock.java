package com.ruoyi.integration.lingxing;

/**
 * 领星库存行。
 */
public class LingxingInventoryProductStock
{
    private String warehouseCode;
    private String warehouseName;
    private String sku;
    private String productName;
    private String inventoryScope;
    private String inventoryAttribute;
    private String batchNo;
    private String locationCode;
    private Long totalQuantity;
    private Long availableQuantity;
    private Long lockedQuantity;
    private Long inTransitQuantity;
    private Long boxedQuantity;
    private Long unboxedQuantity;
    private String sourcePayloadJson;
    private String sourcePayloadHash;

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getInventoryScope() { return inventoryScope; }
    public void setInventoryScope(String inventoryScope) { this.inventoryScope = inventoryScope; }
    public String getInventoryAttribute() { return inventoryAttribute; }
    public void setInventoryAttribute(String inventoryAttribute) { this.inventoryAttribute = inventoryAttribute; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public Long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Long totalQuantity) { this.totalQuantity = totalQuantity; }
    public Long getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Long availableQuantity) { this.availableQuantity = availableQuantity; }
    public Long getLockedQuantity() { return lockedQuantity; }
    public void setLockedQuantity(Long lockedQuantity) { this.lockedQuantity = lockedQuantity; }
    public Long getInTransitQuantity() { return inTransitQuantity; }
    public void setInTransitQuantity(Long inTransitQuantity) { this.inTransitQuantity = inTransitQuantity; }
    public Long getBoxedQuantity() { return boxedQuantity; }
    public void setBoxedQuantity(Long boxedQuantity) { this.boxedQuantity = boxedQuantity; }
    public Long getUnboxedQuantity() { return unboxedQuantity; }
    public void setUnboxedQuantity(Long unboxedQuantity) { this.unboxedQuantity = unboxedQuantity; }
    public String getSourcePayloadJson() { return sourcePayloadJson; }
    public void setSourcePayloadJson(String sourcePayloadJson) { this.sourcePayloadJson = sourcePayloadJson; }
    public String getSourcePayloadHash() { return sourcePayloadHash; }
    public void setSourcePayloadHash(String sourcePayloadHash) { this.sourcePayloadHash = sourcePayloadHash; }
}
