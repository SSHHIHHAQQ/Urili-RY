package com.ruoyi.inventory.domain;

import java.math.BigDecimal;

/**
 * 库存调整审核判定结果。
 */
public class InventoryAdjustmentReviewDecision
{
    private Boolean reviewRequired;
    private String message;
    private InventoryAdjustmentReviewPolicy policy;
    private Long requestedAdjustQty;
    private Long requestExpectedAfterPlatformTotalQty;
    private Long sales7dQty;
    private BigDecimal sales7dDailyAvg;
    private Long sales30dQty;
    private BigDecimal sales30dDailyAvg;
    private BigDecimal thresholdDailyAvg;
    private Integer thresholdReserveDays;
    private Long protectedRetainedQty;
    private Long minRetainedQty;
    private Long immediateReturnableQty;

    public Boolean getReviewRequired() { return reviewRequired; }
    public void setReviewRequired(Boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public InventoryAdjustmentReviewPolicy getPolicy() { return policy; }
    public void setPolicy(InventoryAdjustmentReviewPolicy policy) { this.policy = policy; }
    public Long getRequestedAdjustQty() { return requestedAdjustQty; }
    public void setRequestedAdjustQty(Long requestedAdjustQty) { this.requestedAdjustQty = requestedAdjustQty; }
    public Long getRequestExpectedAfterPlatformTotalQty() { return requestExpectedAfterPlatformTotalQty; }
    public void setRequestExpectedAfterPlatformTotalQty(Long requestExpectedAfterPlatformTotalQty) { this.requestExpectedAfterPlatformTotalQty = requestExpectedAfterPlatformTotalQty; }
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
}
