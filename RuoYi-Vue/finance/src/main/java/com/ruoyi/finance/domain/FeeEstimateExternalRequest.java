package com.ruoyi.finance.domain;

import java.io.Serializable;
import com.ruoyi.finance.domain.request.FeeEstimateRequest;

/**
 * Finance-owned request passed to an external fee estimate adapter.
 */
public class FeeEstimateExternalRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String traceId;

    private String selectionMode;

    private String currencyCode;

    private FeeEstimateRequest originalRequest;

    private FeeEstimatePackageSummary packageSummary;

    private FeeEstimateRouteCandidate routeCandidate;

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getSelectionMode()
    {
        return selectionMode;
    }

    public void setSelectionMode(String selectionMode)
    {
        this.selectionMode = selectionMode;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public FeeEstimateRequest getOriginalRequest()
    {
        return originalRequest;
    }

    public void setOriginalRequest(FeeEstimateRequest originalRequest)
    {
        this.originalRequest = originalRequest;
    }

    public FeeEstimatePackageSummary getPackageSummary()
    {
        return packageSummary;
    }

    public void setPackageSummary(FeeEstimatePackageSummary packageSummary)
    {
        this.packageSummary = packageSummary;
    }

    public FeeEstimateRouteCandidate getRouteCandidate()
    {
        return routeCandidate;
    }

    public void setRouteCandidate(FeeEstimateRouteCandidate routeCandidate)
    {
        this.routeCandidate = routeCandidate;
    }
}
