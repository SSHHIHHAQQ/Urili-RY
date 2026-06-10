package com.ruoyi.logistics.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物流商 API 接入方通用连接。
 */
public class LogisticsCarrierConnection extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long carrierAccountId;

    @JsonIgnore
    private String connectionCode;

    private String providerKind;

    private String carrierName;

    private String apiBaseUrl;

    private String status;

    private String credentialStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastAuthorizedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastChannelSyncTime;

    private Integer displayOrder;

    private LogisticsAgg56Connection agg56;

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

    public String getProviderKind()
    {
        return providerKind;
    }

    public void setProviderKind(String providerKind)
    {
        this.providerKind = providerKind;
    }

    public String getCarrierName()
    {
        return carrierName;
    }

    public void setCarrierName(String carrierName)
    {
        this.carrierName = carrierName;
    }

    @JsonIgnore
    public String getConnectionName()
    {
        return carrierName;
    }

    public void setConnectionName(String connectionName)
    {
        this.carrierName = connectionName;
    }

    public String getApiBaseUrl()
    {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl)
    {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getCredentialStatus()
    {
        return credentialStatus;
    }

    public void setCredentialStatus(String credentialStatus)
    {
        this.credentialStatus = credentialStatus;
    }

    public Date getLastAuthorizedTime()
    {
        return lastAuthorizedTime;
    }

    public void setLastAuthorizedTime(Date lastAuthorizedTime)
    {
        this.lastAuthorizedTime = lastAuthorizedTime;
    }

    public Date getLastChannelSyncTime()
    {
        return lastChannelSyncTime;
    }

    public void setLastChannelSyncTime(Date lastChannelSyncTime)
    {
        this.lastChannelSyncTime = lastChannelSyncTime;
    }

    public Integer getDisplayOrder()
    {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder)
    {
        this.displayOrder = displayOrder;
    }

    public LogisticsAgg56Connection getAgg56()
    {
        return agg56;
    }

    public void setAgg56(LogisticsAgg56Connection agg56)
    {
        this.agg56 = agg56;
    }
}
