package com.ruoyi.finance.domain;

/**
 * Candidate resolution counters for diagnostics.
 */
public class FeeEstimateResolveSummary
{
    private Integer warehouseCandidateCount;

    private Integer quoteSchemeCandidateCount;

    private Integer customerChannelCandidateCount;

    private Integer routeCandidateCount;

    private Integer executableRouteCount;

    private Integer failedCandidateCount;

    private Long resolveCostMs;

    public Integer getWarehouseCandidateCount()
    {
        return warehouseCandidateCount;
    }

    public void setWarehouseCandidateCount(Integer warehouseCandidateCount)
    {
        this.warehouseCandidateCount = warehouseCandidateCount;
    }

    public Integer getQuoteSchemeCandidateCount()
    {
        return quoteSchemeCandidateCount;
    }

    public void setQuoteSchemeCandidateCount(Integer quoteSchemeCandidateCount)
    {
        this.quoteSchemeCandidateCount = quoteSchemeCandidateCount;
    }

    public Integer getCustomerChannelCandidateCount()
    {
        return customerChannelCandidateCount;
    }

    public void setCustomerChannelCandidateCount(Integer customerChannelCandidateCount)
    {
        this.customerChannelCandidateCount = customerChannelCandidateCount;
    }

    public Integer getRouteCandidateCount()
    {
        return routeCandidateCount;
    }

    public void setRouteCandidateCount(Integer routeCandidateCount)
    {
        this.routeCandidateCount = routeCandidateCount;
    }

    public Integer getExecutableRouteCount()
    {
        return executableRouteCount;
    }

    public void setExecutableRouteCount(Integer executableRouteCount)
    {
        this.executableRouteCount = executableRouteCount;
    }

    public Integer getFailedCandidateCount()
    {
        return failedCandidateCount;
    }

    public void setFailedCandidateCount(Integer failedCandidateCount)
    {
        this.failedCandidateCount = failedCandidateCount;
    }

    public Long getResolveCostMs()
    {
        return resolveCostMs;
    }

    public void setResolveCostMs(Long resolveCostMs)
    {
        this.resolveCostMs = resolveCostMs;
    }
}
