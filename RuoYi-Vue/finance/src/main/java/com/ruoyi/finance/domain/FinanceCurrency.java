package com.ruoyi.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 财务币种配置。
 */
public class FinanceCurrency extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long currencyId;

    private String currencyCode;

    private String currencyName;

    private String currencySymbol;

    private String baseCurrencyCode;

    private BigDecimal officialRate;

    private BigDecimal effectiveRate;

    private Integer ratePrecision;

    private Integer amountPrecision;

    private String roundingMode;

    private String adjustmentMode;

    private BigDecimal adjustmentValue;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date officialRateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveRateTime;

    private String isDefault;

    private String status;

    private String keyword;

    public Long getCurrencyId()
    {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId)
    {
        this.currencyId = currencyId;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyName()
    {
        return currencyName;
    }

    public void setCurrencyName(String currencyName)
    {
        this.currencyName = currencyName;
    }

    public String getCurrencySymbol()
    {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol)
    {
        this.currencySymbol = currencySymbol;
    }

    public String getBaseCurrencyCode()
    {
        return baseCurrencyCode;
    }

    public void setBaseCurrencyCode(String baseCurrencyCode)
    {
        this.baseCurrencyCode = baseCurrencyCode;
    }

    public BigDecimal getOfficialRate()
    {
        return officialRate;
    }

    public void setOfficialRate(BigDecimal officialRate)
    {
        this.officialRate = officialRate;
    }

    public BigDecimal getEffectiveRate()
    {
        return effectiveRate;
    }

    public void setEffectiveRate(BigDecimal effectiveRate)
    {
        this.effectiveRate = effectiveRate;
    }

    public Integer getRatePrecision()
    {
        return ratePrecision;
    }

    public void setRatePrecision(Integer ratePrecision)
    {
        this.ratePrecision = ratePrecision;
    }

    public Integer getAmountPrecision()
    {
        return amountPrecision;
    }

    public void setAmountPrecision(Integer amountPrecision)
    {
        this.amountPrecision = amountPrecision;
    }

    public String getRoundingMode()
    {
        return roundingMode;
    }

    public void setRoundingMode(String roundingMode)
    {
        this.roundingMode = roundingMode;
    }

    public String getAdjustmentMode()
    {
        return adjustmentMode;
    }

    public void setAdjustmentMode(String adjustmentMode)
    {
        this.adjustmentMode = adjustmentMode;
    }

    public BigDecimal getAdjustmentValue()
    {
        return adjustmentValue;
    }

    public void setAdjustmentValue(BigDecimal adjustmentValue)
    {
        this.adjustmentValue = adjustmentValue;
    }

    public Date getOfficialRateTime()
    {
        return officialRateTime;
    }

    public void setOfficialRateTime(Date officialRateTime)
    {
        this.officialRateTime = officialRateTime;
    }

    public Date getEffectiveRateTime()
    {
        return effectiveRateTime;
    }

    public void setEffectiveRateTime(Date effectiveRateTime)
    {
        this.effectiveRateTime = effectiveRateTime;
    }

    public String getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(String isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }
}
