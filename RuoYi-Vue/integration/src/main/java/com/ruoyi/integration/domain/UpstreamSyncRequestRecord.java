package com.ruoyi.integration.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 上游同步受理请求。
 */
public class UpstreamSyncRequestRecord implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long requestId;
    private String requestNo;
    private String connectionCode;
    private String triggerSource;
    private String mode;
    private String requestedSyncTypes;
    private String status;
    private String submittedBy;
    private Date submittedTime;
    private Date startedTime;
    private Date finishedTime;
    private Integer taskCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer timeoutCount;
    private Integer skippedCount;
    private Integer cancelledCount;
    private String lastErrorMessage;
    private Date createTime;
    private Date updateTime;
    private String remark;

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getRequestedSyncTypes() { return requestedSyncTypes; }
    public void setRequestedSyncTypes(String requestedSyncTypes) { this.requestedSyncTypes = requestedSyncTypes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Date getSubmittedTime() { return submittedTime; }
    public void setSubmittedTime(Date submittedTime) { this.submittedTime = submittedTime; }
    public Date getStartedTime() { return startedTime; }
    public void setStartedTime(Date startedTime) { this.startedTime = startedTime; }
    public Date getFinishedTime() { return finishedTime; }
    public void setFinishedTime(Date finishedTime) { this.finishedTime = finishedTime; }
    public Integer getTaskCount() { return taskCount; }
    public void setTaskCount(Integer taskCount) { this.taskCount = taskCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public Integer getTimeoutCount() { return timeoutCount; }
    public void setTimeoutCount(Integer timeoutCount) { this.timeoutCount = timeoutCount; }
    public Integer getSkippedCount() { return skippedCount; }
    public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }
    public Integer getCancelledCount() { return cancelledCount; }
    public void setCancelledCount(Integer cancelledCount) { this.cancelledCount = cancelledCount; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
