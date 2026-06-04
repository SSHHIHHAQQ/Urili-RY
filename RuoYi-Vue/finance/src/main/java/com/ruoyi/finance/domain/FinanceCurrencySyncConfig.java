package com.ruoyi.finance.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 财务币种汇率同步配置。
 */
public class FinanceCurrencySyncConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long syncConfigId;

    private String providerCode;

    private String providerName;

    private String baseCurrencyCode;

    private String apiBaseUrl;

    private String authType;

    @JsonIgnore
    private String credentialCiphertext;

    private String credentialKeyId;

    private String credentialMasked;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String credential;

    private Integer requestTimeoutMs;

    private Integer retryCount;

    private String scheduleType;

    private String cronExpression;

    private String rateAnchorTime;

    private String syncEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncTime;

    private String lastSyncStatus;

    private String status;

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

    public String getProviderName()
    {
        return providerName;
    }

    public void setProviderName(String providerName)
    {
        this.providerName = providerName;
    }

    public String getBaseCurrencyCode()
    {
        return baseCurrencyCode;
    }

    public void setBaseCurrencyCode(String baseCurrencyCode)
    {
        this.baseCurrencyCode = baseCurrencyCode;
    }

    public String getApiBaseUrl()
    {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl)
    {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getAuthType()
    {
        return authType;
    }

    public void setAuthType(String authType)
    {
        this.authType = authType;
    }

    public String getCredentialCiphertext()
    {
        return credentialCiphertext;
    }

    public void setCredentialCiphertext(String credentialCiphertext)
    {
        this.credentialCiphertext = credentialCiphertext;
    }

    public String getCredentialKeyId()
    {
        return credentialKeyId;
    }

    public void setCredentialKeyId(String credentialKeyId)
    {
        this.credentialKeyId = credentialKeyId;
    }

    public String getCredentialMasked()
    {
        return credentialMasked;
    }

    public void setCredentialMasked(String credentialMasked)
    {
        this.credentialMasked = credentialMasked;
    }

    public String getCredential()
    {
        return credential;
    }

    public void setCredential(String credential)
    {
        this.credential = credential;
    }

    public Integer getRequestTimeoutMs()
    {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(Integer requestTimeoutMs)
    {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public String getScheduleType()
    {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType)
    {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression)
    {
        this.cronExpression = cronExpression;
    }

    public String getRateAnchorTime()
    {
        return rateAnchorTime;
    }

    public void setRateAnchorTime(String rateAnchorTime)
    {
        this.rateAnchorTime = rateAnchorTime;
    }

    public String getSyncEnabled()
    {
        return syncEnabled;
    }

    public void setSyncEnabled(String syncEnabled)
    {
        this.syncEnabled = syncEnabled;
    }

    public Date getLastSyncTime()
    {
        return lastSyncTime;
    }

    public void setLastSyncTime(Date lastSyncTime)
    {
        this.lastSyncTime = lastSyncTime;
    }

    public String getLastSyncStatus()
    {
        return lastSyncStatus;
    }

    public void setLastSyncStatus(String lastSyncStatus)
    {
        this.lastSyncStatus = lastSyncStatus;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
