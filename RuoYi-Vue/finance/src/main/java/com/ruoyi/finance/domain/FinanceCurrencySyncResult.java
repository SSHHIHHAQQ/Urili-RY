package com.ruoyi.finance.domain;

/**
 * 汇率同步结果。
 */
public class FinanceCurrencySyncResult
{
    private Integer currencyCount;

    private Integer updatedCount;

    private String traceId;

    private String status;

    public Integer getCurrencyCount()
    {
        return currencyCount;
    }

    public void setCurrencyCount(Integer currencyCount)
    {
        this.currencyCount = currencyCount;
    }

    public Integer getUpdatedCount()
    {
        return updatedCount;
    }

    public void setUpdatedCount(Integer updatedCount)
    {
        this.updatedCount = updatedCount;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
