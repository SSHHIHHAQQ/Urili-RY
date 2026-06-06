package com.ruoyi.integration.domain.response;

/**
 * 上游系统单个同步项结果。
 */
public class UpstreamSyncItemResult
{
    private String syncType;
    private String status;
    private int count;
    private int pulledCount;
    private int insertedCount;
    private int changedCount;
    private int unchangedCount;
    private int disabledCount;
    private String errorMessage;

    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public int getPulledCount() { return pulledCount; }
    public void setPulledCount(int pulledCount) { this.pulledCount = pulledCount; }
    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int insertedCount) { this.insertedCount = insertedCount; }
    public int getChangedCount() { return changedCount; }
    public void setChangedCount(int changedCount) { this.changedCount = changedCount; }
    public int getUnchangedCount() { return unchangedCount; }
    public void setUnchangedCount(int unchangedCount) { this.unchangedCount = unchangedCount; }
    public int getDisabledCount() { return disabledCount; }
    public void setDisabledCount(int disabledCount) { this.disabledCount = disabledCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
