package com.ruoyi.inventory.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 库存同步策略预览结果。
 */
public class InventoryStockSyncPolicyPreviewResult
{
    private Boolean allowed;
    private Boolean confirmationRequired;
    private String message;
    private String scopeType;
    private String syncMode;
    private Long affectedRowCount;
    private Long eligibleRowCount;
    private Long skippedRowCount;
    private Long changedRowCount;
    private Long beforePlatformTotalQty;
    private Long afterPlatformTotalQty;
    private Long beforeAvailableQty;
    private Long afterAvailableQty;
    private final List<InventoryStockSyncPolicyPreviewRow> rows = new ArrayList<>();

    public Boolean getAllowed() { return allowed; }
    public void setAllowed(Boolean allowed) { this.allowed = allowed; }
    public Boolean getConfirmationRequired() { return confirmationRequired; }
    public void setConfirmationRequired(Boolean confirmationRequired) { this.confirmationRequired = confirmationRequired; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public Long getAffectedRowCount() { return affectedRowCount; }
    public void setAffectedRowCount(Long affectedRowCount) { this.affectedRowCount = affectedRowCount; }
    public Long getEligibleRowCount() { return eligibleRowCount; }
    public void setEligibleRowCount(Long eligibleRowCount) { this.eligibleRowCount = eligibleRowCount; }
    public Long getSkippedRowCount() { return skippedRowCount; }
    public void setSkippedRowCount(Long skippedRowCount) { this.skippedRowCount = skippedRowCount; }
    public Long getChangedRowCount() { return changedRowCount; }
    public void setChangedRowCount(Long changedRowCount) { this.changedRowCount = changedRowCount; }
    public Long getBeforePlatformTotalQty() { return beforePlatformTotalQty; }
    public void setBeforePlatformTotalQty(Long beforePlatformTotalQty) { this.beforePlatformTotalQty = beforePlatformTotalQty; }
    public Long getAfterPlatformTotalQty() { return afterPlatformTotalQty; }
    public void setAfterPlatformTotalQty(Long afterPlatformTotalQty) { this.afterPlatformTotalQty = afterPlatformTotalQty; }
    public Long getBeforeAvailableQty() { return beforeAvailableQty; }
    public void setBeforeAvailableQty(Long beforeAvailableQty) { this.beforeAvailableQty = beforeAvailableQty; }
    public Long getAfterAvailableQty() { return afterAvailableQty; }
    public void setAfterAvailableQty(Long afterAvailableQty) { this.afterAvailableQty = afterAvailableQty; }
    public List<InventoryStockSyncPolicyPreviewRow> getRows() { return rows; }
}
