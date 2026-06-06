package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 上游系统同步批次。
 */
public class UpstreamSyncBatch
{
    private String syncBatchId;
    private String connectionCode;
    private String syncType;
    private String mode;
    private String status;
    private Integer pulledCount;
    private Integer insertedCount;
    private Integer changedCount;
    private Integer unchangedCount;
    private Integer disabledCount;
    private Integer failedCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;
    private String errorMessage;

    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPulledCount() { return pulledCount; }
    public void setPulledCount(Integer pulledCount) { this.pulledCount = pulledCount; }
    public Integer getInsertedCount() { return insertedCount; }
    public void setInsertedCount(Integer insertedCount) { this.insertedCount = insertedCount; }
    public Integer getChangedCount() { return changedCount; }
    public void setChangedCount(Integer changedCount) { this.changedCount = changedCount; }
    public Integer getUnchangedCount() { return unchangedCount; }
    public void setUnchangedCount(Integer unchangedCount) { this.unchangedCount = unchangedCount; }
    public Integer getDisabledCount() { return disabledCount; }
    public void setDisabledCount(Integer disabledCount) { this.disabledCount = disabledCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public Date getStartedTime() { return startedTime; }
    public void setStartedTime(Date startedTime) { this.startedTime = startedTime; }
    public Date getFinishedTime() { return finishedTime; }
    public void setFinishedTime(Date finishedTime) { this.finishedTime = finishedTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
