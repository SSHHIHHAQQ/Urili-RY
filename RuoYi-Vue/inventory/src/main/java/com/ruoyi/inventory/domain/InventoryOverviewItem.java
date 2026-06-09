package com.ruoyi.inventory.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存总览 SPU/SKU 读模型行。
 */
public class InventoryOverviewItem extends BaseEntity
{
    private String stockKey;
    private Long spuId;
    private Long skuId;
    private Long sellerId;
    private String sellerNo;
    private String sellerName;
    private String systemSpuCode;
    private String systemSkuCode;
    private String productName;
    private String mainImageUrl;
    private String skuName;
    private String skuImageUrl;
    private Integer skuCount;
    private String warehouseKindSummary;
    private Integer warehouseCount;
    private Long platformTotalQty;
    private Long platformAvailableQty;
    private Long platformReservedQty;
    private Long platformInTransitQty;
    private Long sourceTotalQty;
    private Long sourceAvailableQty;
    private Long sourceInTransitQty;
    private String inventoryStatus;
    private String syncModeSummary;
    private String syncPolicyScopeSummary;
    private Date latestSourceSnapshotTime;
    private Date latestStockUpdateTime;
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
    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getSkuImageUrl() { return skuImageUrl; }
    public void setSkuImageUrl(String skuImageUrl) { this.skuImageUrl = skuImageUrl; }
    public Integer getSkuCount() { return skuCount; }
    public void setSkuCount(Integer skuCount) { this.skuCount = skuCount; }
    public String getWarehouseKindSummary() { return warehouseKindSummary; }
    public void setWarehouseKindSummary(String warehouseKindSummary) { this.warehouseKindSummary = warehouseKindSummary; }
    public Integer getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(Integer warehouseCount) { this.warehouseCount = warehouseCount; }
    public Long getPlatformTotalQty() { return platformTotalQty; }
    public void setPlatformTotalQty(Long platformTotalQty) { this.platformTotalQty = platformTotalQty; }
    public Long getPlatformAvailableQty() { return platformAvailableQty; }
    public void setPlatformAvailableQty(Long platformAvailableQty) { this.platformAvailableQty = platformAvailableQty; }
    public Long getPlatformReservedQty() { return platformReservedQty; }
    public void setPlatformReservedQty(Long platformReservedQty) { this.platformReservedQty = platformReservedQty; }
    public Long getPlatformInTransitQty() { return platformInTransitQty; }
    public void setPlatformInTransitQty(Long platformInTransitQty) { this.platformInTransitQty = platformInTransitQty; }
    public Long getSourceTotalQty() { return sourceTotalQty; }
    public void setSourceTotalQty(Long sourceTotalQty) { this.sourceTotalQty = sourceTotalQty; }
    public Long getSourceAvailableQty() { return sourceAvailableQty; }
    public void setSourceAvailableQty(Long sourceAvailableQty) { this.sourceAvailableQty = sourceAvailableQty; }
    public Long getSourceInTransitQty() { return sourceInTransitQty; }
    public void setSourceInTransitQty(Long sourceInTransitQty) { this.sourceInTransitQty = sourceInTransitQty; }
    public String getInventoryStatus() { return inventoryStatus; }
    public void setInventoryStatus(String inventoryStatus) { this.inventoryStatus = inventoryStatus; }
    public String getSyncModeSummary() { return syncModeSummary; }
    public void setSyncModeSummary(String syncModeSummary) { this.syncModeSummary = syncModeSummary; }
    public String getSyncPolicyScopeSummary() { return syncPolicyScopeSummary; }
    public void setSyncPolicyScopeSummary(String syncPolicyScopeSummary) { this.syncPolicyScopeSummary = syncPolicyScopeSummary; }
    public Date getLatestSourceSnapshotTime() { return latestSourceSnapshotTime; }
    public void setLatestSourceSnapshotTime(Date latestSourceSnapshotTime) { this.latestSourceSnapshotTime = latestSourceSnapshotTime; }
    public Date getLatestStockUpdateTime() { return latestStockUpdateTime; }
    public void setLatestStockUpdateTime(Date latestStockUpdateTime) { this.latestStockUpdateTime = latestStockUpdateTime; }
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
