package com.ruoyi.finance.domain;

import java.math.BigDecimal;

/**
 * Merged package dimensions used for quote estimation.
 */
public class FeeEstimatePackageSummary
{
    private BigDecimal edge1Cm;

    private BigDecimal edge2Cm;

    private BigDecimal edge3Cm;

    private BigDecimal actualWeightKg;

    private BigDecimal volumeWeightKg;

    private BigDecimal chargeableWeightKg;

    private Integer packageCount;

    private String sizeExpression;

    private String mergeRule;

    public BigDecimal getEdge1Cm()
    {
        return edge1Cm;
    }

    public void setEdge1Cm(BigDecimal edge1Cm)
    {
        this.edge1Cm = edge1Cm;
    }

    public BigDecimal getEdge2Cm()
    {
        return edge2Cm;
    }

    public void setEdge2Cm(BigDecimal edge2Cm)
    {
        this.edge2Cm = edge2Cm;
    }

    public BigDecimal getEdge3Cm()
    {
        return edge3Cm;
    }

    public void setEdge3Cm(BigDecimal edge3Cm)
    {
        this.edge3Cm = edge3Cm;
    }

    public BigDecimal getActualWeightKg()
    {
        return actualWeightKg;
    }

    public void setActualWeightKg(BigDecimal actualWeightKg)
    {
        this.actualWeightKg = actualWeightKg;
    }

    public BigDecimal getVolumeWeightKg()
    {
        return volumeWeightKg;
    }

    public void setVolumeWeightKg(BigDecimal volumeWeightKg)
    {
        this.volumeWeightKg = volumeWeightKg;
    }

    public BigDecimal getChargeableWeightKg()
    {
        return chargeableWeightKg;
    }

    public void setChargeableWeightKg(BigDecimal chargeableWeightKg)
    {
        this.chargeableWeightKg = chargeableWeightKg;
    }

    public Integer getPackageCount()
    {
        return packageCount;
    }

    public void setPackageCount(Integer packageCount)
    {
        this.packageCount = packageCount;
    }

    public String getSizeExpression()
    {
        return sizeExpression;
    }

    public void setSizeExpression(String sizeExpression)
    {
        this.sizeExpression = sizeExpression;
    }

    public String getMergeRule()
    {
        return mergeRule;
    }

    public void setMergeRule(String mergeRule)
    {
        this.mergeRule = mergeRule;
    }
}
