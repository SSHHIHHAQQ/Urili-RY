package com.ruoyi.logistics.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户渠道买家范围。
 */
public class LogisticsCustomerChannelBuyerScope extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long scopeId;

    private String customerChannelCode;

    private Long buyerId;

    private String buyerCodeSnapshot;

    private String buyerNameSnapshot;

    private String buyerShortNameSnapshot;

    public Long getScopeId()
    {
        return scopeId;
    }

    public void setScopeId(Long scopeId)
    {
        this.scopeId = scopeId;
    }

    public String getCustomerChannelCode()
    {
        return customerChannelCode;
    }

    public void setCustomerChannelCode(String customerChannelCode)
    {
        this.customerChannelCode = customerChannelCode;
    }

    public Long getBuyerId()
    {
        return buyerId;
    }

    public void setBuyerId(Long buyerId)
    {
        this.buyerId = buyerId;
    }

    public String getBuyerCodeSnapshot()
    {
        return buyerCodeSnapshot;
    }

    public void setBuyerCodeSnapshot(String buyerCodeSnapshot)
    {
        this.buyerCodeSnapshot = buyerCodeSnapshot;
    }

    public String getBuyerNameSnapshot()
    {
        return buyerNameSnapshot;
    }

    public void setBuyerNameSnapshot(String buyerNameSnapshot)
    {
        this.buyerNameSnapshot = buyerNameSnapshot;
    }

    public String getBuyerShortNameSnapshot()
    {
        return buyerShortNameSnapshot;
    }

    public void setBuyerShortNameSnapshot(String buyerShortNameSnapshot)
    {
        this.buyerShortNameSnapshot = buyerShortNameSnapshot;
    }
}
