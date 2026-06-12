package com.ruoyi.integration.domain.query;

/**
 * 上游同步任务查询条件。
 */
public class UpstreamSyncTaskQuery
{
    private String connectionCode;
    private String requestNo;
    private String syncType;
    private String status;
    private String triggerSource;
    private String mode;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
