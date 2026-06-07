package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 来源仓库库存组读模型。
 */
public class SourceWarehouseStockGroupItem
{
    private String sourceStockGroupKey;
    private String repositoryScope;
    private String inventoryScope;
    private String masterSku;
    private String masterProductName;
    private String inventoryAttributeCodes;
    private String inventoryAttributeLabels;
    private Integer inventoryAttributeCount;
    private String sourceConnectionCodes;
    private String masterWarehouseNames;
    private Integer masterWarehouseCount;
    private String upstreamWarehouseCodes;
    private String upstreamWarehouseNames;
    private Integer upstreamWarehouseCount;
    private Integer detailRowCount;
    private Integer activeDetailCount;
    private Integer missingDetailCount;
    private Long totalQuantity;
    private Long availableQuantity;
    private Long lockedQuantity;
    private Long inTransitQuantity;
    private Long boxedQuantity;
    private Long unboxedQuantity;
    private String systemWarehouseCodes;
    private String systemWarehouseNames;
    private String systemSkus;
    private String systemSkuNames;
    private String customerNames;
    private String warehousePairingStatus;
    private String skuPairingStatus;
    private String status;
    private String latestSyncBatchId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date latestUpdateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date rebuildTime;

    public String getSourceStockGroupKey() { return sourceStockGroupKey; }
    public void setSourceStockGroupKey(String sourceStockGroupKey) { this.sourceStockGroupKey = sourceStockGroupKey; }
    public String getRepositoryScope() { return repositoryScope; }
    public void setRepositoryScope(String repositoryScope) { this.repositoryScope = repositoryScope; }
    public String getInventoryScope() { return inventoryScope; }
    public void setInventoryScope(String inventoryScope) { this.inventoryScope = inventoryScope; }
    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getMasterProductName() { return masterProductName; }
    public void setMasterProductName(String masterProductName) { this.masterProductName = masterProductName; }
    public String getInventoryAttributeCodes() { return inventoryAttributeCodes; }
    public void setInventoryAttributeCodes(String inventoryAttributeCodes) { this.inventoryAttributeCodes = inventoryAttributeCodes; }
    public String getInventoryAttributeLabels() { return inventoryAttributeLabels; }
    public void setInventoryAttributeLabels(String inventoryAttributeLabels) { this.inventoryAttributeLabels = inventoryAttributeLabels; }
    public Integer getInventoryAttributeCount() { return inventoryAttributeCount; }
    public void setInventoryAttributeCount(Integer inventoryAttributeCount) { this.inventoryAttributeCount = inventoryAttributeCount; }
    public String getSourceConnectionCodes() { return sourceConnectionCodes; }
    public void setSourceConnectionCodes(String sourceConnectionCodes) { this.sourceConnectionCodes = sourceConnectionCodes; }
    public String getMasterWarehouseNames() { return masterWarehouseNames; }
    public void setMasterWarehouseNames(String masterWarehouseNames) { this.masterWarehouseNames = masterWarehouseNames; }
    public Integer getMasterWarehouseCount() { return masterWarehouseCount; }
    public void setMasterWarehouseCount(Integer masterWarehouseCount) { this.masterWarehouseCount = masterWarehouseCount; }
    public String getUpstreamWarehouseCodes() { return upstreamWarehouseCodes; }
    public void setUpstreamWarehouseCodes(String upstreamWarehouseCodes) { this.upstreamWarehouseCodes = upstreamWarehouseCodes; }
    public String getUpstreamWarehouseNames() { return upstreamWarehouseNames; }
    public void setUpstreamWarehouseNames(String upstreamWarehouseNames) { this.upstreamWarehouseNames = upstreamWarehouseNames; }
    public Integer getUpstreamWarehouseCount() { return upstreamWarehouseCount; }
    public void setUpstreamWarehouseCount(Integer upstreamWarehouseCount) { this.upstreamWarehouseCount = upstreamWarehouseCount; }
    public Integer getDetailRowCount() { return detailRowCount; }
    public void setDetailRowCount(Integer detailRowCount) { this.detailRowCount = detailRowCount; }
    public Integer getActiveDetailCount() { return activeDetailCount; }
    public void setActiveDetailCount(Integer activeDetailCount) { this.activeDetailCount = activeDetailCount; }
    public Integer getMissingDetailCount() { return missingDetailCount; }
    public void setMissingDetailCount(Integer missingDetailCount) { this.missingDetailCount = missingDetailCount; }
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
    public String getSystemWarehouseCodes() { return systemWarehouseCodes; }
    public void setSystemWarehouseCodes(String systemWarehouseCodes) { this.systemWarehouseCodes = systemWarehouseCodes; }
    public String getSystemWarehouseNames() { return systemWarehouseNames; }
    public void setSystemWarehouseNames(String systemWarehouseNames) { this.systemWarehouseNames = systemWarehouseNames; }
    public String getSystemSkus() { return systemSkus; }
    public void setSystemSkus(String systemSkus) { this.systemSkus = systemSkus; }
    public String getSystemSkuNames() { return systemSkuNames; }
    public void setSystemSkuNames(String systemSkuNames) { this.systemSkuNames = systemSkuNames; }
    public String getCustomerNames() { return customerNames; }
    public void setCustomerNames(String customerNames) { this.customerNames = customerNames; }
    public String getWarehousePairingStatus() { return warehousePairingStatus; }
    public void setWarehousePairingStatus(String warehousePairingStatus) { this.warehousePairingStatus = warehousePairingStatus; }
    public String getSkuPairingStatus() { return skuPairingStatus; }
    public void setSkuPairingStatus(String skuPairingStatus) { this.skuPairingStatus = skuPairingStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLatestSyncBatchId() { return latestSyncBatchId; }
    public void setLatestSyncBatchId(String latestSyncBatchId) { this.latestSyncBatchId = latestSyncBatchId; }
    public Date getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Date firstSeenTime) { this.firstSeenTime = firstSeenTime; }
    public Date getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Date lastSeenTime) { this.lastSeenTime = lastSeenTime; }
    public Date getLatestUpdateTime() { return latestUpdateTime; }
    public void setLatestUpdateTime(Date latestUpdateTime) { this.latestUpdateTime = latestUpdateTime; }
    public Date getRebuildTime() { return rebuildTime; }
    public void setRebuildTime(Date rebuildTime) { this.rebuildTime = rebuildTime; }
}
