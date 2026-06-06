package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 领星物流渠道同步清单。
 */
public class UpstreamLogisticsChannelSyncItem
{
    private String connectionCode;
    private String warehouseCode;
    private String channelCode;
    private String channelName;
    private String status;
    private String sourcePayloadJson;
    private String sourcePayloadHash;
    private String syncBatchId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourcePayloadJson() { return sourcePayloadJson; }
    public void setSourcePayloadJson(String sourcePayloadJson) { this.sourcePayloadJson = sourcePayloadJson; }
    public String getSourcePayloadHash() { return sourcePayloadHash; }
    public void setSourcePayloadHash(String sourcePayloadHash) { this.sourcePayloadHash = sourcePayloadHash; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public Date getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Date firstSeenTime) { this.firstSeenTime = firstSeenTime; }
    public Date getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Date lastSeenTime) { this.lastSeenTime = lastSeenTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
