package com.ruoyi.buyer.domain;

import java.math.BigDecimal;

/**
 * Buyer terminal SKU response without seller/admin-only fields.
 */
public class BuyerPortalProductSku
{
    private Long skuId;
    private Long spuId;
    private String color;
    private String size;
    private String lengthValue;
    private String widthValue;
    private String heightValue;
    private String weight;
    private String material;
    private String style;
    private String model;
    private String packageQuantity;
    private String capacity;
    private String skuImageUrl;
    private BigDecimal salePrice;
    private String currencyCode;
    private String skuStatus;
    private Integer sortOrder;

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public Long getSpuId()
    {
        return spuId;
    }

    public void setSpuId(Long spuId)
    {
        this.spuId = spuId;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public String getSize()
    {
        return size;
    }

    public void setSize(String size)
    {
        this.size = size;
    }

    public String getLengthValue()
    {
        return lengthValue;
    }

    public void setLengthValue(String lengthValue)
    {
        this.lengthValue = lengthValue;
    }

    public String getWidthValue()
    {
        return widthValue;
    }

    public void setWidthValue(String widthValue)
    {
        this.widthValue = widthValue;
    }

    public String getHeightValue()
    {
        return heightValue;
    }

    public void setHeightValue(String heightValue)
    {
        this.heightValue = heightValue;
    }

    public String getWeight()
    {
        return weight;
    }

    public void setWeight(String weight)
    {
        this.weight = weight;
    }

    public String getMaterial()
    {
        return material;
    }

    public void setMaterial(String material)
    {
        this.material = material;
    }

    public String getStyle()
    {
        return style;
    }

    public void setStyle(String style)
    {
        this.style = style;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getPackageQuantity()
    {
        return packageQuantity;
    }

    public void setPackageQuantity(String packageQuantity)
    {
        this.packageQuantity = packageQuantity;
    }

    public String getCapacity()
    {
        return capacity;
    }

    public void setCapacity(String capacity)
    {
        this.capacity = capacity;
    }

    public String getSkuImageUrl()
    {
        return skuImageUrl;
    }

    public void setSkuImageUrl(String skuImageUrl)
    {
        this.skuImageUrl = skuImageUrl;
    }

    public BigDecimal getSalePrice()
    {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice)
    {
        this.salePrice = salePrice;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public String getSkuStatus()
    {
        return skuStatus;
    }

    public void setSkuStatus(String skuStatus)
    {
        this.skuStatus = skuStatus;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }
}
