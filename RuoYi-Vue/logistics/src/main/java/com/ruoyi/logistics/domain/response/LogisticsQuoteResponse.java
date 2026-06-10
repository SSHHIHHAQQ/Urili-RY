package com.ruoyi.logistics.domain.response;

/**
 * 物流报价响应。
 */
public class LogisticsQuoteResponse
{
    private Long carrierAccountId;

    private String providerKind;

    private String systemChannelCode;

    private String externalChannelCode;

    private String providerResultJson;

    public Long getCarrierAccountId()
    {
        return carrierAccountId;
    }

    public void setCarrierAccountId(Long carrierAccountId)
    {
        this.carrierAccountId = carrierAccountId;
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

    public String getProviderResultJson()
    {
        return providerResultJson;
    }

    public void setProviderResultJson(String providerResultJson)
    {
        this.providerResultJson = providerResultJson;
    }
}
