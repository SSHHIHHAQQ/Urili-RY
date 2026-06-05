package com.ruoyi.product.domain;

/**
 * 商品模块保存卖家快照时使用的轻量对象。
 */
public class ProductSellerSnapshot
{
    private Long sellerId;
    private String sellerNo;
    private String sellerName;

    public ProductSellerSnapshot()
    {
    }

    public ProductSellerSnapshot(Long sellerId, String sellerNo, String sellerName)
    {
        this.sellerId = sellerId;
        this.sellerNo = sellerNo;
        this.sellerName = sellerName;
    }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerNo() { return sellerNo; }
    public void setSellerNo(String sellerNo) { this.sellerNo = sellerNo; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
}
