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
    private String systemSkuCode;
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
    private Integer version;
    private Date calcTime;

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
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
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
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getCalcTime() { return calcTime; }
    public void setCalcTime(Date calcTime) { this.calcTime = calcTime; }
}
