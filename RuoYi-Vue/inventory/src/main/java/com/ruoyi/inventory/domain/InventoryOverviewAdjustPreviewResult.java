package com.ruoyi.inventory.domain;

import java.util.Date;

/**
 * 库存调整预览结果。
 */
public class InventoryOverviewAdjustPreviewResult
{
    private Boolean allowed;
    private Boolean confirmationRequired;
    private String message;
    private Long beforeValue;
    private Long afterValue;
    private Long beforeAvailableQty;
    private Long afterAvailableQty;
    private Long reservedQty;
    private Boolean reviewRequired;
    private Long reviewId;
    private String reviewNo;
    private Long requestedAdjustQty;
    private Long protectedRetainedQty;
    private Long minRetainedQty;
    private Long immediateReturnableQty;
    private Date plannedEffectiveTime;

    public Boolean getAllowed() { return allowed; }
    public void setAllowed(Boolean allowed) { this.allowed = allowed; }
    public Boolean getConfirmationRequired() { return confirmationRequired; }
    public void setConfirmationRequired(Boolean confirmationRequired) { this.confirmationRequired = confirmationRequired; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getBeforeValue() { return beforeValue; }
    public void setBeforeValue(Long beforeValue) { this.beforeValue = beforeValue; }
    public Long getAfterValue() { return afterValue; }
    public void setAfterValue(Long afterValue) { this.afterValue = afterValue; }
    public Long getBeforeAvailableQty() { return beforeAvailableQty; }
    public void setBeforeAvailableQty(Long beforeAvailableQty) { this.beforeAvailableQty = beforeAvailableQty; }
    public Long getAfterAvailableQty() { return afterAvailableQty; }
    public void setAfterAvailableQty(Long afterAvailableQty) { this.afterAvailableQty = afterAvailableQty; }
    public Long getReservedQty() { return reservedQty; }
    public void setReservedQty(Long reservedQty) { this.reservedQty = reservedQty; }
    public Boolean getReviewRequired() { return reviewRequired; }
    public void setReviewRequired(Boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getReviewNo() { return reviewNo; }
    public void setReviewNo(String reviewNo) { this.reviewNo = reviewNo; }
    public Long getRequestedAdjustQty() { return requestedAdjustQty; }
    public void setRequestedAdjustQty(Long requestedAdjustQty) { this.requestedAdjustQty = requestedAdjustQty; }
    public Long getProtectedRetainedQty() { return protectedRetainedQty; }
    public void setProtectedRetainedQty(Long protectedRetainedQty) { this.protectedRetainedQty = protectedRetainedQty; }
    public Long getMinRetainedQty() { return minRetainedQty; }
    public void setMinRetainedQty(Long minRetainedQty) { this.minRetainedQty = minRetainedQty; }
    public Long getImmediateReturnableQty() { return immediateReturnableQty; }
    public void setImmediateReturnableQty(Long immediateReturnableQty) { this.immediateReturnableQty = immediateReturnableQty; }
    public Date getPlannedEffectiveTime() { return plannedEffectiveTime; }
    public void setPlannedEffectiveTime(Date plannedEffectiveTime) { this.plannedEffectiveTime = plannedEffectiveTime; }
}
