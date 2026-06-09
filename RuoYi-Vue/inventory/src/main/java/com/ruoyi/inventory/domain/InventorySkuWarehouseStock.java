package com.ruoyi.inventory.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * SKU + 仓库维度平台库存当前行。
 */
public class InventorySkuWarehouseStock extends BaseEntity
{
    private Long stockId;
    private String stockKey;
    private Long spuId;
    private Long skuId;
    private Long sellerId;
    private String sellerNo;
    private String sellerName;
    private String systemSkuCode;
    private String productName;
    private String skuName;
    private String skuImageUrl;
    private String warehouseKind;
    private String warehouseRefType;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String sourceScope;
    private String sourceMasterWarehouseName;
    private Long sourceTotalQty;
    private Long sourceAvailableQty;
    private Long sourceInTransitQty;
    private Date sourceSnapshotTime;
    private Long platformTotalQty;
    private Long platformReservedQty;
    private Long platformInTransitQty;
    private Long pendingAvailableInboundQty;
    private Long pendingSourceDeductionQty;
    private Long platformAvailableQty;
    private String effectiveStatus;
    private String syncMode;
    private Long syncPolicyId;
    private String syncPolicyScope;
    private String syncPolicyKey;
    private String syncStatus;
    private Date lastAutoSyncTime;
    private Integer version;
    private Date calcTime;
    private String keyword;
    private String pairingStatus;
    private String warehouseKey;
    private Long platformTotalQtyMin;
    private Long platformTotalQtyMax;
    private Long platformAvailableQtyMin;
    private Long platformAvailableQtyMax;
    private Long platformReservedQtyMin;
    private Long platformReservedQtyMax;
    private Long platformInTransitQtyMin;
    private Long platformInTransitQtyMax;
    private Long sourceAvailableQtyMin;
    private Long sourceAvailableQtyMax;
    private Long sourceInTransitQtyMin;
    private Long sourceInTransitQtyMax;
    private String latestSourceSnapshotTimeStart;
    private String latestSourceSnapshotTimeEnd;
    private String latestStockUpdateTimeStart;
    private String latestStockUpdateTimeEnd;

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getStockKey() { return stockKey; }
    public void setStockKey(String stockKey) { this.stockKey = stockKey; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerNo() { return sellerNo; }
    public void setSellerNo(String sellerNo) { this.sellerNo = sellerNo; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getSkuImageUrl() { return skuImageUrl; }
    public void setSkuImageUrl(String skuImageUrl) { this.skuImageUrl = skuImageUrl; }
    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }
    public String getWarehouseRefType() { return warehouseRefType; }
    public void setWarehouseRefType(String warehouseRefType) { this.warehouseRefType = warehouseRefType; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getSourceScope() { return sourceScope; }
    public void setSourceScope(String sourceScope) { this.sourceScope = sourceScope; }
    public String getSourceMasterWarehouseName() { return sourceMasterWarehouseName; }
    public void setSourceMasterWarehouseName(String sourceMasterWarehouseName) { this.sourceMasterWarehouseName = sourceMasterWarehouseName; }
    public Long getSourceTotalQty() { return sourceTotalQty; }
    public void setSourceTotalQty(Long sourceTotalQty) { this.sourceTotalQty = sourceTotalQty; }
    public Long getSourceAvailableQty() { return sourceAvailableQty; }
    public void setSourceAvailableQty(Long sourceAvailableQty) { this.sourceAvailableQty = sourceAvailableQty; }
    public Long getSourceInTransitQty() { return sourceInTransitQty; }
    public void setSourceInTransitQty(Long sourceInTransitQty) { this.sourceInTransitQty = sourceInTransitQty; }
    public Date getSourceSnapshotTime() { return sourceSnapshotTime; }
    public void setSourceSnapshotTime(Date sourceSnapshotTime) { this.sourceSnapshotTime = sourceSnapshotTime; }
    public Long getPlatformTotalQty() { return platformTotalQty; }
    public void setPlatformTotalQty(Long platformTotalQty) { this.platformTotalQty = platformTotalQty; }
    public Long getPlatformReservedQty() { return platformReservedQty; }
    public void setPlatformReservedQty(Long platformReservedQty) { this.platformReservedQty = platformReservedQty; }
    public Long getPlatformInTransitQty() { return platformInTransitQty; }
    public void setPlatformInTransitQty(Long platformInTransitQty) { this.platformInTransitQty = platformInTransitQty; }
    public Long getPendingAvailableInboundQty() { return pendingAvailableInboundQty; }
    public void setPendingAvailableInboundQty(Long pendingAvailableInboundQty) { this.pendingAvailableInboundQty = pendingAvailableInboundQty; }
    public Long getPendingSourceDeductionQty() { return pendingSourceDeductionQty; }
    public void setPendingSourceDeductionQty(Long pendingSourceDeductionQty) { this.pendingSourceDeductionQty = pendingSourceDeductionQty; }
    public Long getPlatformAvailableQty() { return platformAvailableQty; }
    public void setPlatformAvailableQty(Long platformAvailableQty) { this.platformAvailableQty = platformAvailableQty; }
    public String getEffectiveStatus() { return effectiveStatus; }
    public void setEffectiveStatus(String effectiveStatus) { this.effectiveStatus = effectiveStatus; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public Long getSyncPolicyId() { return syncPolicyId; }
    public void setSyncPolicyId(Long syncPolicyId) { this.syncPolicyId = syncPolicyId; }
    public String getSyncPolicyScope() { return syncPolicyScope; }
    public void setSyncPolicyScope(String syncPolicyScope) { this.syncPolicyScope = syncPolicyScope; }
    public String getSyncPolicyKey() { return syncPolicyKey; }
    public void setSyncPolicyKey(String syncPolicyKey) { this.syncPolicyKey = syncPolicyKey; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public Date getLastAutoSyncTime() { return lastAutoSyncTime; }
    public void setLastAutoSyncTime(Date lastAutoSyncTime) { this.lastAutoSyncTime = lastAutoSyncTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getCalcTime() { return calcTime; }
    public void setCalcTime(Date calcTime) { this.calcTime = calcTime; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getPairingStatus() { return pairingStatus; }
    public void setPairingStatus(String pairingStatus) { this.pairingStatus = pairingStatus; }
    public String getWarehouseKey() { return warehouseKey; }
    public void setWarehouseKey(String warehouseKey) { this.warehouseKey = warehouseKey; }
    public Long getPlatformTotalQtyMin() { return platformTotalQtyMin; }
    public void setPlatformTotalQtyMin(Long platformTotalQtyMin) { this.platformTotalQtyMin = platformTotalQtyMin; }
    public Long getPlatformTotalQtyMax() { return platformTotalQtyMax; }
    public void setPlatformTotalQtyMax(Long platformTotalQtyMax) { this.platformTotalQtyMax = platformTotalQtyMax; }
    public Long getPlatformAvailableQtyMin() { return platformAvailableQtyMin; }
    public void setPlatformAvailableQtyMin(Long platformAvailableQtyMin) { this.platformAvailableQtyMin = platformAvailableQtyMin; }
    public Long getPlatformAvailableQtyMax() { return platformAvailableQtyMax; }
    public void setPlatformAvailableQtyMax(Long platformAvailableQtyMax) { this.platformAvailableQtyMax = platformAvailableQtyMax; }
    public Long getPlatformReservedQtyMin() { return platformReservedQtyMin; }
    public void setPlatformReservedQtyMin(Long platformReservedQtyMin) { this.platformReservedQtyMin = platformReservedQtyMin; }
    public Long getPlatformReservedQtyMax() { return platformReservedQtyMax; }
    public void setPlatformReservedQtyMax(Long platformReservedQtyMax) { this.platformReservedQtyMax = platformReservedQtyMax; }
    public Long getPlatformInTransitQtyMin() { return platformInTransitQtyMin; }
    public void setPlatformInTransitQtyMin(Long platformInTransitQtyMin) { this.platformInTransitQtyMin = platformInTransitQtyMin; }
    public Long getPlatformInTransitQtyMax() { return platformInTransitQtyMax; }
    public void setPlatformInTransitQtyMax(Long platformInTransitQtyMax) { this.platformInTransitQtyMax = platformInTransitQtyMax; }
    public Long getSourceAvailableQtyMin() { return sourceAvailableQtyMin; }
    public void setSourceAvailableQtyMin(Long sourceAvailableQtyMin) { this.sourceAvailableQtyMin = sourceAvailableQtyMin; }
    public Long getSourceAvailableQtyMax() { return sourceAvailableQtyMax; }
    public void setSourceAvailableQtyMax(Long sourceAvailableQtyMax) { this.sourceAvailableQtyMax = sourceAvailableQtyMax; }
    public Long getSourceInTransitQtyMin() { return sourceInTransitQtyMin; }
    public void setSourceInTransitQtyMin(Long sourceInTransitQtyMin) { this.sourceInTransitQtyMin = sourceInTransitQtyMin; }
    public Long getSourceInTransitQtyMax() { return sourceInTransitQtyMax; }
    public void setSourceInTransitQtyMax(Long sourceInTransitQtyMax) { this.sourceInTransitQtyMax = sourceInTransitQtyMax; }
    public String getLatestSourceSnapshotTimeStart() { return latestSourceSnapshotTimeStart; }
    public void setLatestSourceSnapshotTimeStart(String latestSourceSnapshotTimeStart) { this.latestSourceSnapshotTimeStart = latestSourceSnapshotTimeStart; }
    public String getLatestSourceSnapshotTimeEnd() { return latestSourceSnapshotTimeEnd; }
    public void setLatestSourceSnapshotTimeEnd(String latestSourceSnapshotTimeEnd) { this.latestSourceSnapshotTimeEnd = latestSourceSnapshotTimeEnd; }
    public String getLatestStockUpdateTimeStart() { return latestStockUpdateTimeStart; }
    public void setLatestStockUpdateTimeStart(String latestStockUpdateTimeStart) { this.latestStockUpdateTimeStart = latestStockUpdateTimeStart; }
    public String getLatestStockUpdateTimeEnd() { return latestStockUpdateTimeEnd; }
    public void setLatestStockUpdateTimeEnd(String latestStockUpdateTimeEnd) { this.latestStockUpdateTimeEnd = latestStockUpdateTimeEnd; }
}
