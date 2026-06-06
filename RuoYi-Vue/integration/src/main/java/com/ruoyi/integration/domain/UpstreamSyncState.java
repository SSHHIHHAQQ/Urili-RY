package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 上游系统分项同步状态。
 */
public class UpstreamSyncState
{
    private Long stateId;
    private String connectionCode;
    private String syncType;
    private String status;
    private String syncBatchId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastStartedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastFinishedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextSyncTime;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private String lastMode;
    private Integer rateLimitMs;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getStateId() { return stateId; }
    public void setStateId(Long stateId) { this.stateId = stateId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public Date getLastStartedTime() { return lastStartedTime; }
    public void setLastStartedTime(Date lastStartedTime) { this.lastStartedTime = lastStartedTime; }
    public Date getLastFinishedTime() { return lastFinishedTime; }
    public void setLastFinishedTime(Date lastFinishedTime) { this.lastFinishedTime = lastFinishedTime; }
    public Date getLastSuccessTime() { return lastSuccessTime; }
    public void setLastSuccessTime(Date lastSuccessTime) { this.lastSuccessTime = lastSuccessTime; }
    public Date getNextSyncTime() { return nextSyncTime; }
    public void setNextSyncTime(Date nextSyncTime) { this.nextSyncTime = nextSyncTime; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public String getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public String getLastMode() { return lastMode; }
    public void setLastMode(String lastMode) { this.lastMode = lastMode; }
    public Integer getRateLimitMs() { return rateLimitMs; }
    public void setRateLimitMs(Integer rateLimitMs) { this.rateLimitMs = rateLimitMs; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
