package com.ruoyi.finance.domain.request;

import java.math.BigDecimal;

/**
 * Submitted package line for manual or SKU mode.
 */
public class FeeEstimatePackageLine
{
    private Long skuId;

    private Integer quantity;

    private BigDecimal lengthCm;

    private BigDecimal widthCm;

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getLengthCm()
    {
        return lengthCm;
    }

    public void setLengthCm(BigDecimal lengthCm)
    {
        this.lengthCm = lengthCm;
    }

    public BigDecimal getWidthCm()
    {
        return widthCm;
    }

    public void setWidthCm(BigDecimal widthCm)
    {
        this.widthCm = widthCm;
    }

    public BigDecimal getHeightCm()
    {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm)
    {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg()
    {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg)
    {
        this.weightKg = weightKg;
    }
}
