package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 上游 SKU 仓库库存快照。
 */
public class SourceWarehouseStockItem
{
    private Long inventorySnapshotId;
    private String connectionCode;
    private String systemKind;
    private String systemKindLabel;
    private String masterWarehouseName;
    private String upstreamWarehouseCode;
    private String upstreamWarehouseName;
    private String masterSku;
    private String masterProductName;
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
    private String systemWarehouseCode;
    private String systemWarehouseName;
    private String systemSku;
    private String systemSkuName;
    private String customerName;
    private String warehousePairingStatus;
    private String skuPairingStatus;
    private String status;
    private String syncBatchId;
    private String sourcePayloadJson;
    private String sourcePayloadHash;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getInventorySnapshotId() { return inventorySnapshotId; }
    public void setInventorySnapshotId(Long inventorySnapshotId) { this.inventorySnapshotId = inventorySnapshotId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getSystemKind() { return systemKind; }
    public void setSystemKind(String systemKind) { this.systemKind = systemKind; }
    public String getSystemKindLabel() { return systemKindLabel; }
    public void setSystemKindLabel(String systemKindLabel) { this.systemKindLabel = systemKindLabel; }
    public String getMasterWarehouseName() { return masterWarehouseName; }
    public void setMasterWarehouseName(String masterWarehouseName) { this.masterWarehouseName = masterWarehouseName; }
    public String getUpstreamWarehouseCode() { return upstreamWarehouseCode; }
    public void setUpstreamWarehouseCode(String upstreamWarehouseCode) { this.upstreamWarehouseCode = upstreamWarehouseCode; }
    public String getUpstreamWarehouseName() { return upstreamWarehouseName; }
    public void setUpstreamWarehouseName(String upstreamWarehouseName) { this.upstreamWarehouseName = upstreamWarehouseName; }
    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getMasterProductName() { return masterProductName; }
    public void setMasterProductName(String masterProductName) { this.masterProductName = masterProductName; }
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
    public String getSystemWarehouseCode() { return systemWarehouseCode; }
    public void setSystemWarehouseCode(String systemWarehouseCode) { this.systemWarehouseCode = systemWarehouseCode; }
    public String getSystemWarehouseName() { return systemWarehouseName; }
    public void setSystemWarehouseName(String systemWarehouseName) { this.systemWarehouseName = systemWarehouseName; }
    public String getSystemSku() { return systemSku; }
    public void setSystemSku(String systemSku) { this.systemSku = systemSku; }
    public String getSystemSkuName() { return systemSkuName; }
    public void setSystemSkuName(String systemSkuName) { this.systemSkuName = systemSkuName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getWarehousePairingStatus() { return warehousePairingStatus; }
    public void setWarehousePairingStatus(String warehousePairingStatus) { this.warehousePairingStatus = warehousePairingStatus; }
    public String getSkuPairingStatus() { return skuPairingStatus; }
    public void setSkuPairingStatus(String skuPairingStatus) { this.skuPairingStatus = skuPairingStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public String getSourcePayloadJson() { return sourcePayloadJson; }
    public void setSourcePayloadJson(String sourcePayloadJson) { this.sourcePayloadJson = sourcePayloadJson; }
    public String getSourcePayloadHash() { return sourcePayloadHash; }
    public void setSourcePayloadHash(String sourcePayloadHash) { this.sourcePayloadHash = sourcePayloadHash; }
    public Date getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Date firstSeenTime) { this.firstSeenTime = firstSeenTime; }
    public Date getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Date lastSeenTime) { this.lastSeenTime = lastSeenTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
