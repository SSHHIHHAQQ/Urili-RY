package com.ruoyi.inventory.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存调整审核策略。
 */
public class InventoryAdjustmentReviewPolicy extends BaseEntity
{
    private Long policyId;
    private String policyName;
    private String policyStatus;
    private String reviewMode;
    private String directionScope;
    private String fieldScope;
    private String salesWindowDays;
    private String salesAggregateMode;
    private Integer reserveDays;
    private Integer cooldownHours;
    private Long minReturnQtyToReview;
    private BigDecimal minReturnRatioToReview;
    private String autoEffectEnabled;
    private String manualEffectAllowed;

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getPolicyStatus() { return policyStatus; }
    public void setPolicyStatus(String policyStatus) { this.policyStatus = policyStatus; }
    public String getReviewMode() { return reviewMode; }
    public void setReviewMode(String reviewMode) { this.reviewMode = reviewMode; }
    public String getDirectionScope() { return directionScope; }
    public void setDirectionScope(String directionScope) { this.directionScope = directionScope; }
    public String getFieldScope() { return fieldScope; }
    public void setFieldScope(String fieldScope) { this.fieldScope = fieldScope; }
    public String getSalesWindowDays() { return salesWindowDays; }
    public void setSalesWindowDays(String salesWindowDays) { this.salesWindowDays = salesWindowDays; }
    public String getSalesAggregateMode() { return salesAggregateMode; }
    public void setSalesAggregateMode(String salesAggregateMode) { this.salesAggregateMode = salesAggregateMode; }
    public Integer getReserveDays() { return reserveDays; }
    public void setReserveDays(Integer reserveDays) { this.reserveDays = reserveDays; }
    public Integer getCooldownHours() { return cooldownHours; }
    public void setCooldownHours(Integer cooldownHours) { this.cooldownHours = cooldownHours; }
    public Long getMinReturnQtyToReview() { return minReturnQtyToReview; }
    public void setMinReturnQtyToReview(Long minReturnQtyToReview) { this.minReturnQtyToReview = minReturnQtyToReview; }
    public BigDecimal getMinReturnRatioToReview() { return minReturnRatioToReview; }
    public void setMinReturnRatioToReview(BigDecimal minReturnRatioToReview) { this.minReturnRatioToReview = minReturnRatioToReview; }
    public String getAutoEffectEnabled() { return autoEffectEnabled; }
    public void setAutoEffectEnabled(String autoEffectEnabled) { this.autoEffectEnabled = autoEffectEnabled; }
    public String getManualEffectAllowed() { return manualEffectAllowed; }
    public void setManualEffectAllowed(String manualEffectAllowed) { this.manualEffectAllowed = manualEffectAllowed; }
}
