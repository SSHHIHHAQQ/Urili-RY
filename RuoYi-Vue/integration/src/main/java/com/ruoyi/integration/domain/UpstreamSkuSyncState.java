package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * SKU 同步状态。
 */
public class UpstreamSkuSyncState
{
    private String connectionCode;
    private String status;
    private String syncBatchId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastStartedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastFinishedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSuccessTime;
    private String lastErrorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextSyncTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
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
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public Date getNextSyncTime() { return nextSyncTime; }
    public void setNextSyncTime(Date nextSyncTime) { this.nextSyncTime = nextSyncTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
