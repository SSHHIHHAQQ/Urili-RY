package com.ruoyi.product.domain;

/**
 * Buyer-facing fulfillment warehouse display row.
 */
public class ProductCenterWarehouse
{
    private Long id;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseKind;
    private String warehouseKindLabel;
    private String settlementCurrency;
    private String deliveryText;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }
    public String getWarehouseKindLabel() { return warehouseKindLabel; }
    public void setWarehouseKindLabel(String warehouseKindLabel) { this.warehouseKindLabel = warehouseKindLabel; }
    public String getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }
    public String getDeliveryText() { return deliveryText; }
    public void setDeliveryText(String deliveryText) { this.deliveryText = deliveryText; }
}
