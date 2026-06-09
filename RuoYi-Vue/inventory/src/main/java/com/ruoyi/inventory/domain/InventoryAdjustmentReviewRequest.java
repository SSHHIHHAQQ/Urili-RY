package com.ruoyi.inventory.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存调整审核单。
 */
public class InventoryAdjustmentReviewRequest extends BaseEntity
{
    private Long reviewId;
    private String reviewNo;
    private String reviewStatus;
    private Long policyId;
    private String policySnapshotJson;
    private Long stockId;
    private String stockKey;
    private Long spuId;
    private Long skuId;
    private Long sellerId;
    private String systemSkuCode;
    private String productName;
    private String skuName;
    private String warehouseKind;
    private String warehouseRefType;
    private String warehouseName;
    private String adjustField;
    private String adjustDirection;
    private Long requestBeforePlatformTotalQty;
    private Long requestedAdjustQty;
    private Long requestExpectedAfterPlatformTotalQty;
    private Long platformReservedQtySnapshot;
    private Long sales7dQty;
    private BigDecimal sales7dDailyAvg;
    private Long sales30dQty;
    private BigDecimal sales30dDailyAvg;
    private BigDecimal thresholdDailyAvg;
    private Integer thresholdReserveDays;
    private Long protectedRetainedQty;
    private Long minRetainedQty;
    private Long immediateReturnableQty;
    private String triggerReason;
    private String submitTerminal;
    private Long submitUserId;
    private String submitUserName;
    private String submitReason;
    private Date submitTime;
    private Date plannedEffectiveTime;
    private Date effectiveTime;
    private Long effectiveOperatorId;
    private String effectiveOperatorName;
    private Long effectiveBeforePlatformTotalQty;
    private Long actualEffectQty;
    private Long unfulfilledQty;
    private Long effectiveAfterPlatformTotalQty;
    private String reviewReason;
    private Integer version;
    private String keyword;
    private String submitTimeStart;
    private String submitTimeEnd;
    private String plannedEffectiveTimeStart;
    private String plannedEffectiveTimeEnd;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getReviewNo() { return reviewNo; }
    public void setReviewNo(String reviewNo) { this.reviewNo = reviewNo; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getPolicySnapshotJson() { return policySnapshotJson; }
    public void setPolicySnapshotJson(String policySnapshotJson) { this.policySnapshotJson = policySnapshotJson; }
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
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }
    public String getWarehouseRefType() { return warehouseRefType; }
    public void setWarehouseRefType(String warehouseRefType) { this.warehouseRefType = warehouseRefType; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getAdjustField() { return adjustField; }
    public void setAdjustField(String adjustField) { this.adjustField = adjustField; }
    public String getAdjustDirection() { return adjustDirection; }
    public void setAdjustDirection(String adjustDirection) { this.adjustDirection = adjustDirection; }
    public Long getRequestBeforePlatformTotalQty() { return requestBeforePlatformTotalQty; }
    public void setRequestBeforePlatformTotalQty(Long requestBeforePlatformTotalQty) { this.requestBeforePlatformTotalQty = requestBeforePlatformTotalQty; }
    public Long getRequestedAdjustQty() { return requestedAdjustQty; }
    public void setRequestedAdjustQty(Long requestedAdjustQty) { this.requestedAdjustQty = requestedAdjustQty; }
    public Long getRequestExpectedAfterPlatformTotalQty() { return requestExpectedAfterPlatformTotalQty; }
    public void setRequestExpectedAfterPlatformTotalQty(Long requestExpectedAfterPlatformTotalQty) { this.requestExpectedAfterPlatformTotalQty = requestExpectedAfterPlatformTotalQty; }
    public Long getPlatformReservedQtySnapshot() { return platformReservedQtySnapshot; }
    public void setPlatformReservedQtySnapshot(Long platformReservedQtySnapshot) { this.platformReservedQtySnapshot = platformReservedQtySnapshot; }
    public Long getSales7dQty() { return sales7dQty; }
    public void setSales7dQty(Long sales7dQty) { this.sales7dQty = sales7dQty; }
    public BigDecimal getSales7dDailyAvg() { return sales7dDailyAvg; }
    public void setSales7dDailyAvg(BigDecimal sales7dDailyAvg) { this.sales7dDailyAvg = sales7dDailyAvg; }
    public Long getSales30dQty() { return sales30dQty; }
    public void setSales30dQty(Long sales30dQty) { this.sales30dQty = sales30dQty; }
    public BigDecimal getSales30dDailyAvg() { return sales30dDailyAvg; }
    public void setSales30dDailyAvg(BigDecimal sales30dDailyAvg) { this.sales30dDailyAvg = sales30dDailyAvg; }
    public BigDecimal getThresholdDailyAvg() { return thresholdDailyAvg; }
    public void setThresholdDailyAvg(BigDecimal thresholdDailyAvg) { this.thresholdDailyAvg = thresholdDailyAvg; }
    public Integer getThresholdReserveDays() { return thresholdReserveDays; }
    public void setThresholdReserveDays(Integer thresholdReserveDays) { this.thresholdReserveDays = thresholdReserveDays; }
    public Long getProtectedRetainedQty() { return protectedRetainedQty; }
    public void setProtectedRetainedQty(Long protectedRetainedQty) { this.protectedRetainedQty = protectedRetainedQty; }
    public Long getMinRetainedQty() { return minRetainedQty; }
    public void setMinRetainedQty(Long minRetainedQty) { this.minRetainedQty = minRetainedQty; }
    public Long getImmediateReturnableQty() { return immediateReturnableQty; }
    public void setImmediateReturnableQty(Long immediateReturnableQty) { this.immediateReturnableQty = immediateReturnableQty; }
    public String getTriggerReason() { return triggerReason; }
    public void setTriggerReason(String triggerReason) { this.triggerReason = triggerReason; }
    public String getSubmitTerminal() { return submitTerminal; }
    public void setSubmitTerminal(String submitTerminal) { this.submitTerminal = submitTerminal; }
    public Long getSubmitUserId() { return submitUserId; }
    public void setSubmitUserId(Long submitUserId) { this.submitUserId = submitUserId; }
    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }
    public String getSubmitReason() { return submitReason; }
    public void setSubmitReason(String submitReason) { this.submitReason = submitReason; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }
    public Date getPlannedEffectiveTime() { return plannedEffectiveTime; }
    public void setPlannedEffectiveTime(Date plannedEffectiveTime) { this.plannedEffectiveTime = plannedEffectiveTime; }
    public Date getEffectiveTime() { return effectiveTime; }
    public void setEffectiveTime(Date effectiveTime) { this.effectiveTime = effectiveTime; }
    public Long getEffectiveOperatorId() { return effectiveOperatorId; }
    public void setEffectiveOperatorId(Long effectiveOperatorId) { this.effectiveOperatorId = effectiveOperatorId; }
    public String getEffectiveOperatorName() { return effectiveOperatorName; }
    public void setEffectiveOperatorName(String effectiveOperatorName) { this.effectiveOperatorName = effectiveOperatorName; }
    public Long getEffectiveBeforePlatformTotalQty() { return effectiveBeforePlatformTotalQty; }
    public void setEffectiveBeforePlatformTotalQty(Long effectiveBeforePlatformTotalQty) { this.effectiveBeforePlatformTotalQty = effectiveBeforePlatformTotalQty; }
    public Long getActualEffectQty() { return actualEffectQty; }
    public void setActualEffectQty(Long actualEffectQty) { this.actualEffectQty = actualEffectQty; }
    public Long getUnfulfilledQty() { return unfulfilledQty; }
    public void setUnfulfilledQty(Long unfulfilledQty) { this.unfulfilledQty = unfulfilledQty; }
    public Long getEffectiveAfterPlatformTotalQty() { return effectiveAfterPlatformTotalQty; }
    public void setEffectiveAfterPlatformTotalQty(Long effectiveAfterPlatformTotalQty) { this.effectiveAfterPlatformTotalQty = effectiveAfterPlatformTotalQty; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSubmitTimeStart() { return submitTimeStart; }
    public void setSubmitTimeStart(String submitTimeStart) { this.submitTimeStart = submitTimeStart; }
    public String getSubmitTimeEnd() { return submitTimeEnd; }
    public void setSubmitTimeEnd(String submitTimeEnd) { this.submitTimeEnd = submitTimeEnd; }
    public String getPlannedEffectiveTimeStart() { return plannedEffectiveTimeStart; }
    public void setPlannedEffectiveTimeStart(String plannedEffectiveTimeStart) { this.plannedEffectiveTimeStart = plannedEffectiveTimeStart; }
    public String getPlannedEffectiveTimeEnd() { return plannedEffectiveTimeEnd; }
    public void setPlannedEffectiveTimeEnd(String plannedEffectiveTimeEnd) { this.plannedEffectiveTimeEnd = plannedEffectiveTimeEnd; }
}
