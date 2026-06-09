package com.ruoyi.integration.domain;

/**
 * Official warehouse option derived from source SKU dimension pairing.
 */
public class SourceOfficialWarehouseOption
{
    private String sourceDimensionGroupKey;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseKind;
    private String settlementCurrency;

    public String getSourceDimensionGroupKey() { return sourceDimensionGroupKey; }
    public void setSourceDimensionGroupKey(String sourceDimensionGroupKey) { this.sourceDimensionGroupKey = sourceDimensionGroupKey; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }
    public String getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }
}
