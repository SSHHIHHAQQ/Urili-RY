package com.ruoyi.seller.domain;

import com.ruoyi.system.domain.PortalAccount;

/**
 * Seller account binding, backed by seller_account.
 */
public class SellerAccount extends PortalAccount
{
    private static final long serialVersionUID = 1L;

    private Long sellerAccountId;

    private Long sellerId;

    public Long getSellerAccountId()
    {
        return sellerAccountId;
    }

    public void setSellerAccountId(Long sellerAccountId)
    {
        this.sellerAccountId = sellerAccountId;
    }

    public Long getSellerId()
    {
        return sellerId;
    }

    public void setSellerId(Long sellerId)
    {
        this.sellerId = sellerId;
    }
}
