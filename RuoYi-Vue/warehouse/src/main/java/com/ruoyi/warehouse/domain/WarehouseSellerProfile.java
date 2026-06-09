package com.ruoyi.warehouse.domain;

/**
 * 仓库模块使用的卖家资料快照。
 */
public class WarehouseSellerProfile
{
    private Long sellerId;
    private String sellerNo;
    private String sellerCode;
    private String sellerName;
    private String sellerShortName;

    public Long getSellerId()
    {
        return sellerId;
    }

    public void setSellerId(Long sellerId)
    {
        this.sellerId = sellerId;
    }

    public String getSellerNo()
    {
        return sellerNo;
    }

    public void setSellerNo(String sellerNo)
    {
        this.sellerNo = sellerNo;
    }

    public String getSellerCode()
    {
        return sellerCode;
    }

    public void setSellerCode(String sellerCode)
    {
        this.sellerCode = sellerCode;
    }

    public String getSellerName()
    {
        return sellerName;
    }

    public void setSellerName(String sellerName)
    {
        this.sellerName = sellerName;
    }

    public String getSellerShortName()
    {
        return sellerShortName;
    }

    public void setSellerShortName(String sellerShortName)
    {
        this.sellerShortName = sellerShortName;
    }
}
