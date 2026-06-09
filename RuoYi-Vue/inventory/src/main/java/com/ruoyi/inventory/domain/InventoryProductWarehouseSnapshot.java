package com.ruoyi.inventory.domain;

/**
 * Product-owned SPU warehouse binding snapshot consumed by inventory refresh.
 */
public class InventoryProductWarehouseSnapshot
{
    private Long spuId;
    private Long warehouseId;
    private String warehouseKind;
    private String warehouseCode;
    private String warehouseName;

    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
}
