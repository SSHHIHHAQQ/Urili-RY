package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.Size;

/**
 * 面单动作请求。
 */
public class LogisticsLabelActionRequest
{
    private Long carrierAccountId;

    @Size(max = 100, message = "业务单号不能超过100个字符")
    private String businessOrderNo;

    @Size(max = 100, message = "物流商订单号不能超过100个字符")
    private String providerOrderNo;

    public Long getCarrierAccountId()
    {
        return carrierAccountId;
    }

    public void setCarrierAccountId(Long carrierAccountId)
    {
        this.carrierAccountId = carrierAccountId;
    }

    public String getBusinessOrderNo()
    {
        return businessOrderNo;
    }

    public void setBusinessOrderNo(String businessOrderNo)
    {
        this.businessOrderNo = businessOrderNo;
    }

    public String getProviderOrderNo()
    {
        return providerOrderNo;
    }

    public void setProviderOrderNo(String providerOrderNo)
    {
        this.providerOrderNo = providerOrderNo;
    }
}
