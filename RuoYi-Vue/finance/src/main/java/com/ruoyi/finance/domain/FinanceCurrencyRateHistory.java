package com.ruoyi.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 财务币种汇率历史。
 */
public class FinanceCurrencyRateHistory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long rateHistoryId;

    private String currencyCode;

    private String baseCurrencyCode;

    private BigDecimal officialRate;

    private BigDecimal effectiveRate;

    private String adjustmentMode;

    private BigDecimal adjustmentValue;

    private String sourceType;

    private Long sourceConfigId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date officialRateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveRateTime;

    private String changeReason;

    public Long getRateHistoryId()
    {
        return rateHistoryId;
    }

    public void setRateHistoryId(Long rateHistoryId)
    {
        this.rateHistoryId = rateHistoryId;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
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

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public Long getSourceConfigId()
    {
        return sourceConfigId;
    }

    public void setSourceConfigId(Long sourceConfigId)
    {
        this.sourceConfigId = sourceConfigId;
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

    public String getChangeReason()
    {
        return changeReason;
    }

    public void setChangeReason(String changeReason)
    {
        this.changeReason = changeReason;
    }
}
