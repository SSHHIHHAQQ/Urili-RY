package com.ruoyi.logistics.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物流商面单订单。
 */
public class LogisticsLabelOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long labelOrderId;

    private String businessOrderNo;

    private Long carrierAccountId;

    private String connectionCode;

    private String providerKind;

    private String systemChannelCode;

    private String externalChannelCode;

    private String providerOrderNo;

    private String status;

    private String labelFileTypes;

    private String zoneCode;

    private String chargeWeight;

    private String logisticsError;

    @JsonIgnore
    private String createPayloadJson;

    private String providerResultJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastFetchedTime;

    private List<LogisticsLabelPackage> packages;

    public Long getLabelOrderId()
    {
        return labelOrderId;
    }

    public void setLabelOrderId(Long labelOrderId)
    {
        this.labelOrderId = labelOrderId;
    }

    public String getBusinessOrderNo()
    {
        return businessOrderNo;
    }

    public void setBusinessOrderNo(String businessOrderNo)
    {
        this.businessOrderNo = businessOrderNo;
    }

    public Long getCarrierAccountId()
    {
        return carrierAccountId;
    }

    public void setCarrierAccountId(Long carrierAccountId)
    {
        this.carrierAccountId = carrierAccountId;
    }

    @JsonIgnore
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

    public String getSystemChannelCode()
    {
        return systemChannelCode;
    }

    public void setSystemChannelCode(String systemChannelCode)
    {
        this.systemChannelCode = systemChannelCode;
    }

    public String getExternalChannelCode()
    {
        return externalChannelCode;
    }

    public void setExternalChannelCode(String externalChannelCode)
    {
        this.externalChannelCode = externalChannelCode;
    }

    public String getProviderOrderNo()
    {
        return providerOrderNo;
    }

    public void setProviderOrderNo(String providerOrderNo)
    {
        this.providerOrderNo = providerOrderNo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getLabelFileTypes()
    {
        return labelFileTypes;
    }

    public void setLabelFileTypes(String labelFileTypes)
    {
        this.labelFileTypes = labelFileTypes;
    }

    public String getZoneCode()
    {
        return zoneCode;
    }

    public void setZoneCode(String zoneCode)
    {
        this.zoneCode = zoneCode;
    }

    public String getChargeWeight()
    {
        return chargeWeight;
    }

    public void setChargeWeight(String chargeWeight)
    {
        this.chargeWeight = chargeWeight;
    }

    public String getLogisticsError()
    {
        return logisticsError;
    }

    public void setLogisticsError(String logisticsError)
    {
        this.logisticsError = logisticsError;
    }

    public String getCreatePayloadJson()
    {
        return createPayloadJson;
    }

    public void setCreatePayloadJson(String createPayloadJson)
    {
        this.createPayloadJson = createPayloadJson;
    }

    public String getProviderResultJson()
    {
        return providerResultJson;
    }

    public void setProviderResultJson(String providerResultJson)
    {
        this.providerResultJson = providerResultJson;
    }

    public Date getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime)
    {
        this.createdTime = createdTime;
    }

    public Date getLastFetchedTime()
    {
        return lastFetchedTime;
    }

    public void setLastFetchedTime(Date lastFetchedTime)
    {
        this.lastFetchedTime = lastFetchedTime;
    }

    public List<LogisticsLabelPackage> getPackages()
    {
        return packages;
    }

    public void setPackages(List<LogisticsLabelPackage> packages)
    {
        this.packages = packages;
    }
}
