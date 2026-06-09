package com.ruoyi.inventory.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 库存总览批量调整预览结果。
 */
public class InventoryOverviewBatchAdjustPreviewResult
{
    private Boolean allowed;

    private Boolean confirmationRequired;

    private String message;

    private Long changedRowCount;

    private Long beforePlatformTotalQty;

    private Long afterPlatformTotalQty;

    private Long beforePlatformInTransitQty;

    private Long afterPlatformInTransitQty;

    private Long beforeAvailableQty;

    private Long afterAvailableQty;

    private Long reviewRequiredCount;

    private List<InventoryOverviewBatchAdjustRowPreview> rows = new ArrayList<>();

    public Boolean getAllowed()
    {
        return allowed;
    }

    public void setAllowed(Boolean allowed)
    {
        this.allowed = allowed;
    }

    public Boolean getConfirmationRequired()
    {
        return confirmationRequired;
    }

    public void setConfirmationRequired(Boolean confirmationRequired)
    {
        this.confirmationRequired = confirmationRequired;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Long getChangedRowCount()
    {
        return changedRowCount;
    }

    public void setChangedRowCount(Long changedRowCount)
    {
        this.changedRowCount = changedRowCount;
    }

    public Long getBeforePlatformTotalQty()
    {
        return beforePlatformTotalQty;
    }

    public void setBeforePlatformTotalQty(Long beforePlatformTotalQty)
    {
        this.beforePlatformTotalQty = beforePlatformTotalQty;
    }

    public Long getAfterPlatformTotalQty()
    {
        return afterPlatformTotalQty;
    }

    public void setAfterPlatformTotalQty(Long afterPlatformTotalQty)
    {
        this.afterPlatformTotalQty = afterPlatformTotalQty;
    }

    public Long getBeforePlatformInTransitQty()
    {
        return beforePlatformInTransitQty;
    }

    public void setBeforePlatformInTransitQty(Long beforePlatformInTransitQty)
    {
        this.beforePlatformInTransitQty = beforePlatformInTransitQty;
    }

    public Long getAfterPlatformInTransitQty()
    {
        return afterPlatformInTransitQty;
    }

    public void setAfterPlatformInTransitQty(Long afterPlatformInTransitQty)
    {
        this.afterPlatformInTransitQty = afterPlatformInTransitQty;
    }

    public Long getBeforeAvailableQty()
    {
        return beforeAvailableQty;
    }

    public void setBeforeAvailableQty(Long beforeAvailableQty)
    {
        this.beforeAvailableQty = beforeAvailableQty;
    }

    public Long getAfterAvailableQty()
    {
        return afterAvailableQty;
    }

    public void setAfterAvailableQty(Long afterAvailableQty)
    {
        this.afterAvailableQty = afterAvailableQty;
    }

    public Long getReviewRequiredCount()
    {
        return reviewRequiredCount;
    }

    public void setReviewRequiredCount(Long reviewRequiredCount)
    {
        this.reviewRequiredCount = reviewRequiredCount;
    }

    public List<InventoryOverviewBatchAdjustRowPreview> getRows()
    {
        return rows;
    }

    public void setRows(List<InventoryOverviewBatchAdjustRowPreview> rows)
    {
        this.rows = rows;
    }
}
