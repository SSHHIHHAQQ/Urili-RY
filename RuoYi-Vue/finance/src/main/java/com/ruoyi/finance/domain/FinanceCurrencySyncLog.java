package com.ruoyi.finance.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 财务币种汇率同步日志。
 */
public class FinanceCurrencySyncLog
{
    private Long syncLogId;

    private String traceId;

    private Long syncConfigId;

    private String providerCode;

    private String requestUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date requestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date responseTime;

    private Long costMs;

    private String status;

    private String errorCode;

    private String errorMessage;

    private Integer currencyCount;

    private Integer updatedCount;

    private String responseSummary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getSyncLogId()
    {
        return syncLogId;
    }

    public void setSyncLogId(Long syncLogId)
    {
        this.syncLogId = syncLogId;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public Long getSyncConfigId()
    {
        return syncConfigId;
    }

    public void setSyncConfigId(Long syncConfigId)
    {
        this.syncConfigId = syncConfigId;
    }

    public String getProviderCode()
    {
        return providerCode;
    }

    public void setProviderCode(String providerCode)
    {
        this.providerCode = providerCode;
    }

    public String getRequestUrl()
    {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl)
    {
        this.requestUrl = requestUrl;
    }

    public Date getRequestTime()
    {
        return requestTime;
    }

    public void setRequestTime(Date requestTime)
    {
        this.requestTime = requestTime;
    }

    public Date getResponseTime()
    {
        return responseTime;
    }

    public void setResponseTime(Date responseTime)
    {
        this.responseTime = responseTime;
    }

    public Long getCostMs()
    {
        return costMs;
    }

    public void setCostMs(Long costMs)
    {
        this.costMs = costMs;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(String errorCode)
    {
        this.errorCode = errorCode;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public Integer getCurrencyCount()
    {
        return currencyCount;
    }

    public void setCurrencyCount(Integer currencyCount)
    {
        this.currencyCount = currencyCount;
    }

    public Integer getUpdatedCount()
    {
        return updatedCount;
    }

    public void setUpdatedCount(Integer updatedCount)
    {
        this.updatedCount = updatedCount;
    }

    public String getResponseSummary()
    {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary)
    {
        this.responseSummary = responseSummary;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
