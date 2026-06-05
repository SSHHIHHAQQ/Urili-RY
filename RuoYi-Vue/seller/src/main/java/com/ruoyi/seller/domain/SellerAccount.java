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

    private String lockStatus;

    private String lockReason;

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

    public String getLockStatus()
    {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus)
    {
        this.lockStatus = lockStatus;
    }

    public String getLockReason()
    {
        return lockReason;
    }

    public void setLockReason(String lockReason)
    {
        this.lockReason = lockReason;
    }
}
