package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 上游库存同步状态。
 */
public class UpstreamInventorySyncState
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextSyncTime;
    private Integer totalCount;
    private Integer activeCount;
    private Integer missingCount;
    private String lastErrorMessage;
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
    public Date getNextSyncTime() { return nextSyncTime; }
    public void setNextSyncTime(Date nextSyncTime) { this.nextSyncTime = nextSyncTime; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getActiveCount() { return activeCount; }
    public void setActiveCount(Integer activeCount) { this.activeCount = activeCount; }
    public Integer getMissingCount() { return missingCount; }
    public void setMissingCount(Integer missingCount) { this.missingCount = missingCount; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
