package com.ruoyi.logistics.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户渠道绑定系统渠道。
 */
public class LogisticsCustomerChannelSystemMapping extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long mappingId;

    private String customerChannelCode;

    private String systemChannelCode;

    private String systemChannelNameSnapshot;

    private String standardCarrierCodeSnapshot;

    private String signatureServicesSnapshot;

    private String status;

    private Integer displayOrder;

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

    public String getStandardCarrierCodeSnapshot()
    {
        return standardCarrierCodeSnapshot;
    }

    public void setStandardCarrierCodeSnapshot(String standardCarrierCodeSnapshot)
    {
        this.standardCarrierCodeSnapshot = standardCarrierCodeSnapshot;
    }

    public String getSignatureServicesSnapshot()
    {
        return signatureServicesSnapshot;
    }

    public void setSignatureServicesSnapshot(String signatureServicesSnapshot)
    {
        this.signatureServicesSnapshot = signatureServicesSnapshot;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getDisplayOrder()
    {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder)
    {
        this.displayOrder = displayOrder;
    }
}
