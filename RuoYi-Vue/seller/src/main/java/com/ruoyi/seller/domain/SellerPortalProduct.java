package com.ruoyi.seller.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seller terminal product response without admin-only scope fields.
 */
public class SellerPortalProduct
{
    private Long spuId;
    private String sellerSpuCode;
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
    private BigDecimal supplyPriceMin;
    private BigDecimal supplyPriceMax;
    private BigDecimal salePriceMin;
    private BigDecimal salePriceMax;
    private String currencySummary;
    private Integer warehouseCount;
    private List<SellerPortalProductSku> skus;

    public Long getSpuId()
    {
        return spuId;
    }

    public void setSpuId(Long spuId)
    {
        this.spuId = spuId;
    }

    public String getSellerSpuCode()
    {
        return sellerSpuCode;
    }

    public void setSellerSpuCode(String sellerSpuCode)
    {
        this.sellerSpuCode = sellerSpuCode;
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

    public BigDecimal getSupplyPriceMin()
    {
        return supplyPriceMin;
    }

    public void setSupplyPriceMin(BigDecimal supplyPriceMin)
    {
        this.supplyPriceMin = supplyPriceMin;
    }

    public BigDecimal getSupplyPriceMax()
    {
        return supplyPriceMax;
    }

    public void setSupplyPriceMax(BigDecimal supplyPriceMax)
    {
        this.supplyPriceMax = supplyPriceMax;
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

    public Integer getWarehouseCount()
    {
        return warehouseCount;
    }

    public void setWarehouseCount(Integer warehouseCount)
    {
        this.warehouseCount = warehouseCount;
    }

    public List<SellerPortalProductSku> getSkus()
    {
        return skus;
    }

    public void setSkus(List<SellerPortalProductSku> skus)
    {
        this.skus = skus;
    }
}
