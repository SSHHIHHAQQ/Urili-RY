package com.ruoyi.finance.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 外部汇率同步响应。
 */
public class CurrencyRateSyncResponse
{
    private String baseCurrencyCode;

    private List<CurrencyRateCandidate> candidates = new ArrayList<>();

    private String responseSummary;

    public String getBaseCurrencyCode()
    {
        return baseCurrencyCode;
    }

    public void setBaseCurrencyCode(String baseCurrencyCode)
    {
        this.baseCurrencyCode = baseCurrencyCode;
    }

    public List<CurrencyRateCandidate> getCandidates()
    {
        return candidates;
    }

    public void setCandidates(List<CurrencyRateCandidate> candidates)
    {
        this.candidates = candidates;
    }

    public String getResponseSummary()
    {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary)
    {
        this.responseSummary = responseSummary;
    }
}
