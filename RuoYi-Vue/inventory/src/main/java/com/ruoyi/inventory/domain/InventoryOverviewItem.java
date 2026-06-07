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
    private Date latestSourceSnapshotTime;
    private Date latestStockUpdateTime;
    private String keyword;

    public String getStockKey() { return stockKey; }
    public void setStockKey(String stockKey) { this.stockKey = stockKey; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
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
    public Date getLatestSourceSnapshotTime() { return latestSourceSnapshotTime; }
    public void setLatestSourceSnapshotTime(Date latestSourceSnapshotTime) { this.latestSourceSnapshotTime = latestSourceSnapshotTime; }
    public Date getLatestStockUpdateTime() { return latestStockUpdateTime; }
    public void setLatestStockUpdateTime(Date latestStockUpdateTime) { this.latestStockUpdateTime = latestStockUpdateTime; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
