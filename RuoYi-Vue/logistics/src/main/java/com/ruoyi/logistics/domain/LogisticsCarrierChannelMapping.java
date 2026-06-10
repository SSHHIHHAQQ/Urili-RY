package com.ruoyi.logistics.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物流商渠道映射。
 */
public class LogisticsCarrierChannelMapping extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long mappingId;

    private Long carrierAccountId;

    @JsonIgnore
    private String connectionCode;

    private String providerKind;

    private String carrierName;

    private String externalChannelCode;

    private String externalChannelNameSnapshot;

    private String systemChannelCode;

    private String systemChannelNameSnapshot;

    private String standardCarrierCode;

    private String status;

    public Long getMappingId()
    {
        return mappingId;
    }

    public void setMappingId(Long mappingId)
    {
        this.mappingId = mappingId;
    }

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

    public String getExternalChannelCode()
    {
        return externalChannelCode;
    }

    public void setExternalChannelCode(String externalChannelCode)
    {
        this.externalChannelCode = externalChannelCode;
    }

    public String getExternalChannelNameSnapshot()
    {
        return externalChannelNameSnapshot;
    }

    public void setExternalChannelNameSnapshot(String externalChannelNameSnapshot)
    {
        this.externalChannelNameSnapshot = externalChannelNameSnapshot;
    }

    public String getSystemChannelCode()
    {
        return systemChannelCode;
    }

    public void setSystemChannelCode(String systemChannelCode)
    {
        this.systemChannelCode = systemChannelCode;
    }

    public String getSystemChannelNameSnapshot()
    {
        return systemChannelNameSnapshot;
    }

    public void setSystemChannelNameSnapshot(String systemChannelNameSnapshot)
    {
        this.systemChannelNameSnapshot = systemChannelNameSnapshot;
    }

    public String getStandardCarrierCode()
    {
        return standardCarrierCode;
    }

    public void setStandardCarrierCode(String standardCarrierCode)
    {
        this.standardCarrierCode = standardCarrierCode;
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
