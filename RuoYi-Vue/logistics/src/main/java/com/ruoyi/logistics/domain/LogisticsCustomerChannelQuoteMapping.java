package com.ruoyi.logistics.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Customer logistics channel quote-channel mapping.
 */
public class LogisticsCustomerChannelQuoteMapping extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long mappingId;

    private String customerChannelCode;

    private String connectionCode;

    private String masterWarehouseNameSnapshot;

    private String upstreamChannelCode;

    private String upstreamChannelName;

    private String pairingRole;

    private String status;

    public Long getMappingId()
    {
        return mappingId;
    }

    public void setMappingId(Long mappingId)
    {
        this.mappingId = mappingId;
    }

    public String getCustomerChannelCode()
    {
        return customerChannelCode;
    }

    public void setCustomerChannelCode(String customerChannelCode)
    {
        this.customerChannelCode = customerChannelCode;
    }

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getMasterWarehouseNameSnapshot()
    {
        return masterWarehouseNameSnapshot;
    }

    public void setMasterWarehouseNameSnapshot(String masterWarehouseNameSnapshot)
    {
        this.masterWarehouseNameSnapshot = masterWarehouseNameSnapshot;
    }

    public String getUpstreamChannelCode()
    {
        return upstreamChannelCode;
    }

    public void setUpstreamChannelCode(String upstreamChannelCode)
    {
        this.upstreamChannelCode = upstreamChannelCode;
    }

    public String getUpstreamChannelName()
    {
        return upstreamChannelName;
    }

    public void setUpstreamChannelName(String upstreamChannelName)
    {
        this.upstreamChannelName = upstreamChannelName;
    }

    public String getPairingRole()
    {
        return pairingRole;
    }

    public void setPairingRole(String pairingRole)
    {
        this.pairingRole = pairingRole;
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
