package com.ruoyi.inventory.domain;

/**
 * 库存总览卖家筛选选项。
 */
public class InventoryOverviewSellerOption
{
    private Long value;
    private String label;
    private Long sellerId;
    private String sellerNo;
    private String sellerName;
    private String searchText;

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerNo() { return sellerNo; }
    public void setSellerNo(String sellerNo) { this.sellerNo = sellerNo; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
}
