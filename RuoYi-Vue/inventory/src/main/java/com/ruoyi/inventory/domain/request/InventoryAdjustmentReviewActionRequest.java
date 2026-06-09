package com.ruoyi.inventory.domain.request;

import java.util.Date;

/**
 * 库存调整审核动作请求。
 */
public class InventoryAdjustmentReviewActionRequest
{
    private String reason;
    private Date plannedEffectiveTime;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Date getPlannedEffectiveTime() { return plannedEffectiveTime; }
    public void setPlannedEffectiveTime(Date plannedEffectiveTime) { this.plannedEffectiveTime = plannedEffectiveTime; }
}
