package com.ruoyi.integration.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 上游同步可执行任务。
 */
public class UpstreamSyncTask implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String requestNo;
    private String syncBatchId;
    private String connectionCode;
    private String syncType;
    private String mode;
    private String triggerSource;
    private String status;
    private Integer priority;
    private String payloadRedacted;
    private String leaseOwner;
    private Date leaseUntil;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Date nextAttemptTime;
    private Date deadlineAt;
    private Date startedTime;
    private Date finishedTime;
    private Long currentRequestLogId;
    private String traceId;
    private String sysJobInvokeTarget;
    private Integer pulledCount;
    private Integer insertedCount;
    private Integer changedCount;
    private Integer unchangedCount;
    private Integer disabledCount;
    private Integer failedCount;
    private String errorCode;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
    private String remark;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getPayloadRedacted() { return payloadRedacted; }
    public void setPayloadRedacted(String payloadRedacted) { this.payloadRedacted = payloadRedacted; }
    public String getLeaseOwner() { return leaseOwner; }
    public void setLeaseOwner(String leaseOwner) { this.leaseOwner = leaseOwner; }
    public Date getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(Date leaseUntil) { this.leaseUntil = leaseUntil; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public Date getNextAttemptTime() { return nextAttemptTime; }
    public void setNextAttemptTime(Date nextAttemptTime) { this.nextAttemptTime = nextAttemptTime; }
    public Date getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Date deadlineAt) { this.deadlineAt = deadlineAt; }
    public Date getStartedTime() { return startedTime; }
    public void setStartedTime(Date startedTime) { this.startedTime = startedTime; }
    public Date getFinishedTime() { return finishedTime; }
    public void setFinishedTime(Date finishedTime) { this.finishedTime = finishedTime; }
    public Long getCurrentRequestLogId() { return currentRequestLogId; }
    public void setCurrentRequestLogId(Long currentRequestLogId) { this.currentRequestLogId = currentRequestLogId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSysJobInvokeTarget() { return sysJobInvokeTarget; }
    public void setSysJobInvokeTarget(String sysJobInvokeTarget) { this.sysJobInvokeTarget = sysJobInvokeTarget; }
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
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
