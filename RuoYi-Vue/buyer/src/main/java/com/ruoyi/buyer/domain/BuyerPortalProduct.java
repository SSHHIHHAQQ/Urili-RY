package com.ruoyi.buyer.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Buyer terminal product response without seller/admin-only fields.
 */
public class BuyerPortalProduct
{
    private Long spuId;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String productName;
    private String productNameEn;
    private String sellingPoint;
    private String mainImageUrl;
    private String detailContent;
    private String spuStatus;
    private Integer skuCount;
    private BigDecimal salePriceMin;
    private BigDecimal salePriceMax;
    private String currencySummary;
    private List<BuyerPortalProductSku> skus;

    public Long getSpuId()
    {
        return spuId;
    }

    public void setSpuId(Long spuId)
    {
        this.spuId = spuId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryCode()
    {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode)
    {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductNameEn()
    {
        return productNameEn;
    }

    public void setProductNameEn(String productNameEn)
    {
        this.productNameEn = productNameEn;
    }

    public String getSellingPoint()
    {
        return sellingPoint;
    }

    public void setSellingPoint(String sellingPoint)
    {
        this.sellingPoint = sellingPoint;
    }

    public String getMainImageUrl()
    {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl)
    {
        this.mainImageUrl = mainImageUrl;
    }

    public String getDetailContent()
    {
        return detailContent;
    }

    public void setDetailContent(String detailContent)
    {
        this.detailContent = detailContent;
    }

    public String getSpuStatus()
    {
        return spuStatus;
    }

    public void setSpuStatus(String spuStatus)
    {
        this.spuStatus = spuStatus;
    }

    public Integer getSkuCount()
    {
        return skuCount;
    }

    public void setSkuCount(Integer skuCount)
    {
        this.skuCount = skuCount;
    }

    public BigDecimal getSalePriceMin()
    {
        return salePriceMin;
    }

    public void setSalePriceMin(BigDecimal salePriceMin)
    {
        this.salePriceMin = salePriceMin;
    }

    public BigDecimal getSalePriceMax()
    {
        return salePriceMax;
    }

    public void setSalePriceMax(BigDecimal salePriceMax)
    {
        this.salePriceMax = salePriceMax;
    }

    public String getCurrencySummary()
    {
        return currencySummary;
    }

    public void setCurrencySummary(String currencySummary)
    {
        this.currencySummary = currencySummary;
    }

    public List<BuyerPortalProductSku> getSkus()
    {
        return skus;
    }

    public void setSkus(List<BuyerPortalProductSku> skus)
    {
        this.skus = skus;
    }
}
