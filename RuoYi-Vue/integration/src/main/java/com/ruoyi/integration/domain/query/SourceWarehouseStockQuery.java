package com.ruoyi.integration.domain.query;

/**
 * 上游 SKU 仓库库存快照查询条件。
 */
public class SourceWarehouseStockQuery
{
    private String sourceStockGroupKey;
    private String repositoryScope;
    private String connectionCode;
    private String keyword;
    private String status;
    private String warehousePairingStatus;
    private String skuPairingStatus;
    private String inventoryScope;
    private String inventoryAttribute;
    private String masterWarehouseName;
    private String upstreamWarehouseCode;
    private String masterWarehouseKeyword;
    private String warehouseKeyword;
    private Long totalQuantityMin;
    private Long totalQuantityMax;
    private Long availableQuantityMin;
    private Long availableQuantityMax;
    private Long lockedQuantityMin;
    private Long lockedQuantityMax;
    private Long inTransitQuantityMin;
    private Long inTransitQuantityMax;

    public String getSourceStockGroupKey() { return sourceStockGroupKey; }
    public void setSourceStockGroupKey(String sourceStockGroupKey) { this.sourceStockGroupKey = sourceStockGroupKey; }
    public String getRepositoryScope() { return repositoryScope; }
    public void setRepositoryScope(String repositoryScope) { this.repositoryScope = repositoryScope; }
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
    public String getMasterWarehouseName() { return masterWarehouseName; }
    public void setMasterWarehouseName(String masterWarehouseName) { this.masterWarehouseName = masterWarehouseName; }
    public String getUpstreamWarehouseCode() { return upstreamWarehouseCode; }
    public void setUpstreamWarehouseCode(String upstreamWarehouseCode) { this.upstreamWarehouseCode = upstreamWarehouseCode; }
    public String getMasterWarehouseKeyword() { return masterWarehouseKeyword; }
    public void setMasterWarehouseKeyword(String masterWarehouseKeyword) { this.masterWarehouseKeyword = masterWarehouseKeyword; }
    public String getWarehouseKeyword() { return warehouseKeyword; }
    public void setWarehouseKeyword(String warehouseKeyword) { this.warehouseKeyword = warehouseKeyword; }
    public Long getTotalQuantityMin() { return totalQuantityMin; }
    public void setTotalQuantityMin(Long totalQuantityMin) { this.totalQuantityMin = totalQuantityMin; }
    public Long getTotalQuantityMax() { return totalQuantityMax; }
    public void setTotalQuantityMax(Long totalQuantityMax) { this.totalQuantityMax = totalQuantityMax; }
    public Long getAvailableQuantityMin() { return availableQuantityMin; }
    public void setAvailableQuantityMin(Long availableQuantityMin) { this.availableQuantityMin = availableQuantityMin; }
    public Long getAvailableQuantityMax() { return availableQuantityMax; }
    public void setAvailableQuantityMax(Long availableQuantityMax) { this.availableQuantityMax = availableQuantityMax; }
    public Long getLockedQuantityMin() { return lockedQuantityMin; }
    public void setLockedQuantityMin(Long lockedQuantityMin) { this.lockedQuantityMin = lockedQuantityMin; }
    public Long getLockedQuantityMax() { return lockedQuantityMax; }
    public void setLockedQuantityMax(Long lockedQuantityMax) { this.lockedQuantityMax = lockedQuantityMax; }
    public Long getInTransitQuantityMin() { return inTransitQuantityMin; }
    public void setInTransitQuantityMin(Long inTransitQuantityMin) { this.inTransitQuantityMin = inTransitQuantityMin; }
    public Long getInTransitQuantityMax() { return inTransitQuantityMax; }
    public void setInTransitQuantityMax(Long inTransitQuantityMax) { this.inTransitQuantityMax = inTransitQuantityMax; }
}
