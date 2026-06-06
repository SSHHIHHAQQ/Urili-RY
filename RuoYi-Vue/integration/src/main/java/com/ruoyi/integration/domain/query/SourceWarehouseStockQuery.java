package com.ruoyi.integration.domain.query;

/**
 * 上游 SKU 仓库库存快照查询条件。
 */
public class SourceWarehouseStockQuery
{
    private String connectionCode;
    private String keyword;
    private String status;
    private String warehousePairingStatus;
    private String skuPairingStatus;
    private String inventoryScope;
    private String inventoryAttribute;
    private String warehouseKeyword;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWarehousePairingStatus() { return warehousePairingStatus; }
    public void setWarehousePairingStatus(String warehousePairingStatus) { this.warehousePairingStatus = warehousePairingStatus; }
    public String getSkuPairingStatus() { return skuPairingStatus; }
    public void setSkuPairingStatus(String skuPairingStatus) { this.skuPairingStatus = skuPairingStatus; }
    public String getInventoryScope() { return inventoryScope; }
    public void setInventoryScope(String inventoryScope) { this.inventoryScope = inventoryScope; }
    public String getInventoryAttribute() { return inventoryAttribute; }
    public void setInventoryAttribute(String inventoryAttribute) { this.inventoryAttribute = inventoryAttribute; }
    public String getWarehouseKeyword() { return warehouseKeyword; }
    public void setWarehouseKeyword(String warehouseKeyword) { this.warehouseKeyword = warehouseKeyword; }
}
