package com.ruoyi.buyer.domain;

import com.ruoyi.system.domain.PortalAccount;

/**
 * Buyer account binding, backed by buyer_account.
 */
public class BuyerAccount extends PortalAccount
{
    private static final long serialVersionUID = 1L;

    private Long buyerAccountId;

    private Long buyerId;

    public Long getBuyerAccountId()
    {
        return buyerAccountId;
    }

    public void setBuyerAccountId(Long buyerAccountId)
    {
        this.buyerAccountId = buyerAccountId;
    }

    public Long getBuyerId()
    {
        return buyerId;
    }

    public void setBuyerId(Long buyerId)
    {
        this.buyerId = buyerId;
    }
}
