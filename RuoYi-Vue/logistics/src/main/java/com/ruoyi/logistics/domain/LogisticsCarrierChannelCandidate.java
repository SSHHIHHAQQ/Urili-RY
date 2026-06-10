package com.ruoyi.logistics.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 物流商渠道。
 */
public class LogisticsCarrierChannelCandidate implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long carrierAccountId;

    @JsonIgnore
    private String connectionCode;

    private String externalChannelCode;

    private String externalChannelName;

    private String rawFinalCarrierText;

    private String status;

    private String syncBatchId;

    private String sourcePayloadJson;

    private String sourcePayloadHash;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getCarrierAccountId()
    {
        return carrierAccountId;
    }

    public void setCarrierAccountId(Long carrierAccountId)
    {
        this.carrierAccountId = carrierAccountId;
    }

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getExternalChannelCode()
    {
        return externalChannelCode;
    }

    public void setExternalChannelCode(String externalChannelCode)
    {
        this.externalChannelCode = externalChannelCode;
    }

    public String getExternalChannelName()
    {
        return externalChannelName;
    }

    public void setExternalChannelName(String externalChannelName)
    {
        this.externalChannelName = externalChannelName;
    }

    public String getRawFinalCarrierText()
    {
        return rawFinalCarrierText;
    }

    public void setRawFinalCarrierText(String rawFinalCarrierText)
    {
        this.rawFinalCarrierText = rawFinalCarrierText;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getSyncBatchId()
    {
        return syncBatchId;
    }

    public void setSyncBatchId(String syncBatchId)
    {
        this.syncBatchId = syncBatchId;
    }

    public String getSourcePayloadJson()
    {
        return sourcePayloadJson;
    }

    public void setSourcePayloadJson(String sourcePayloadJson)
    {
        this.sourcePayloadJson = sourcePayloadJson;
    }

    public String getSourcePayloadHash()
    {
        return sourcePayloadHash;
    }

    public void setSourcePayloadHash(String sourcePayloadHash)
    {
        this.sourcePayloadHash = sourcePayloadHash;
    }

    public Date getFirstSeenTime()
    {
        return firstSeenTime;
    }

    public void setFirstSeenTime(Date firstSeenTime)
    {
        this.firstSeenTime = firstSeenTime;
    }

    public Date getLastSeenTime()
    {
        return lastSeenTime;
    }

    public void setLastSeenTime(Date lastSeenTime)
    {
        this.lastSeenTime = lastSeenTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
