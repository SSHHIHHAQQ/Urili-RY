package com.ruoyi.inventory.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存流水。
 */
public class InventoryStockLedger extends BaseEntity
{
    private Long ledgerId;
    private Long stockId;
    private String stockKey;
    private Long spuId;
    private Long skuId;
    private Long sellerId;
    private String warehouseKind;
    private String warehouseRefType;
    private String warehouseName;
    private String operationType;
    private String operationSource;
    private String bizType;
    private String bizNo;
    private Long deltaQty;
    private Long beforePlatformTotalQty;
    private Long afterPlatformTotalQty;
    private Long beforeAvailableQty;
    private Long afterAvailableQty;
    private Long beforeReservedQty;
    private Long afterReservedQty;
    private Long beforeInTransitQty;
    private Long afterInTransitQty;
    private String riskConfirmed;
    private String riskMessage;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private Date operateTime;

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
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
    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }
    public String getWarehouseRefType() { return warehouseRefType; }
    public void setWarehouseRefType(String warehouseRefType) { this.warehouseRefType = warehouseRefType; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getOperationSource() { return operationSource; }
    public void setOperationSource(String operationSource) { this.operationSource = operationSource; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public Long getDeltaQty() { return deltaQty; }
    public void setDeltaQty(Long deltaQty) { this.deltaQty = deltaQty; }
    public Long getBeforePlatformTotalQty() { return beforePlatformTotalQty; }
    public void setBeforePlatformTotalQty(Long beforePlatformTotalQty) { this.beforePlatformTotalQty = beforePlatformTotalQty; }
    public Long getAfterPlatformTotalQty() { return afterPlatformTotalQty; }
    public void setAfterPlatformTotalQty(Long afterPlatformTotalQty) { this.afterPlatformTotalQty = afterPlatformTotalQty; }
    public Long getBeforeAvailableQty() { return beforeAvailableQty; }
    public void setBeforeAvailableQty(Long beforeAvailableQty) { this.beforeAvailableQty = beforeAvailableQty; }
    public Long getAfterAvailableQty() { return afterAvailableQty; }
    public void setAfterAvailableQty(Long afterAvailableQty) { this.afterAvailableQty = afterAvailableQty; }
    public Long getBeforeReservedQty() { return beforeReservedQty; }
    public void setBeforeReservedQty(Long beforeReservedQty) { this.beforeReservedQty = beforeReservedQty; }
    public Long getAfterReservedQty() { return afterReservedQty; }
    public void setAfterReservedQty(Long afterReservedQty) { this.afterReservedQty = afterReservedQty; }
    public Long getBeforeInTransitQty() { return beforeInTransitQty; }
    public void setBeforeInTransitQty(Long beforeInTransitQty) { this.beforeInTransitQty = beforeInTransitQty; }
    public Long getAfterInTransitQty() { return afterInTransitQty; }
    public void setAfterInTransitQty(Long afterInTransitQty) { this.afterInTransitQty = afterInTransitQty; }
    public String getRiskConfirmed() { return riskConfirmed; }
    public void setRiskConfirmed(String riskConfirmed) { this.riskConfirmed = riskConfirmed; }
    public String getRiskMessage() { return riskMessage; }
    public void setRiskMessage(String riskMessage) { this.riskMessage = riskMessage; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Date getOperateTime() { return operateTime; }
    public void setOperateTime(Date operateTime) { this.operateTime = operateTime; }
}
