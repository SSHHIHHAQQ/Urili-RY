package com.ruoyi.finance.support;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 外部官方汇率候选记录。
 */
public class CurrencyRateCandidate
{
    private String currencyCode;

    private BigDecimal officialRate;

    private Date officialRateTime;

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getOfficialRate()
    {
        return officialRate;
    }

    public void setOfficialRate(BigDecimal officialRate)
    {
        this.officialRate = officialRate;
    }

    public Date getOfficialRateTime()
    {
        return officialRateTime;
    }

    public void setOfficialRateTime(Date officialRateTime)
    {
        this.officialRateTime = officialRateTime;
    }
}
