package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品审核主列表的轻量展示项，不落库。
 */
public class ProductReviewListDisplayItem
{
    private Long itemId;
    private Long skuId;
    private String systemSkuCode;
    private String sellerSkuCode;
    private String skuCode;
    private String changeType;
    private BigDecimal beforeSupplyPrice;
    private BigDecimal afterSupplyPrice;
    private String currencyCode;
    private String priceDirection;
    private String beforeSpecSummary;
    private String afterSpecSummary;
    private String beforeDimensionSummary;
    private String afterDimensionSummary;
    private String beforeWarehouseSummary;
    private String afterWarehouseSummary;
    private List<String> changedFieldNames;
    private String changeSummary;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getSellerSkuCode() { return sellerSkuCode; }
    public void setSellerSkuCode(String sellerSkuCode) { this.sellerSkuCode = sellerSkuCode; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public BigDecimal getBeforeSupplyPrice() { return beforeSupplyPrice; }
    public void setBeforeSupplyPrice(BigDecimal beforeSupplyPrice) { this.beforeSupplyPrice = beforeSupplyPrice; }
    public BigDecimal getAfterSupplyPrice() { return afterSupplyPrice; }
    public void setAfterSupplyPrice(BigDecimal afterSupplyPrice) { this.afterSupplyPrice = afterSupplyPrice; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getPriceDirection() { return priceDirection; }
    public void setPriceDirection(String priceDirection) { this.priceDirection = priceDirection; }
    public String getBeforeSpecSummary() { return beforeSpecSummary; }
    public void setBeforeSpecSummary(String beforeSpecSummary) { this.beforeSpecSummary = beforeSpecSummary; }
    public String getAfterSpecSummary() { return afterSpecSummary; }
    public void setAfterSpecSummary(String afterSpecSummary) { this.afterSpecSummary = afterSpecSummary; }
    public String getBeforeDimensionSummary() { return beforeDimensionSummary; }
    public void setBeforeDimensionSummary(String beforeDimensionSummary) { this.beforeDimensionSummary = beforeDimensionSummary; }
    public String getAfterDimensionSummary() { return afterDimensionSummary; }
    public void setAfterDimensionSummary(String afterDimensionSummary) { this.afterDimensionSummary = afterDimensionSummary; }
    public String getBeforeWarehouseSummary() { return beforeWarehouseSummary; }
    public void setBeforeWarehouseSummary(String beforeWarehouseSummary) { this.beforeWarehouseSummary = beforeWarehouseSummary; }
    public String getAfterWarehouseSummary() { return afterWarehouseSummary; }
    public void setAfterWarehouseSummary(String afterWarehouseSummary) { this.afterWarehouseSummary = afterWarehouseSummary; }
    public List<String> getChangedFieldNames() { return changedFieldNames; }
    public void setChangedFieldNames(List<String> changedFieldNames) { this.changedFieldNames = changedFieldNames; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
}
