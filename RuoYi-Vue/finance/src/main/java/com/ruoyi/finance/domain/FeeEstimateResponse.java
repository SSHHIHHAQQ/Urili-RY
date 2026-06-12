package com.ruoyi.finance.domain;

import java.util.List;

/**
 * Fee estimate response payload.
 */
public class FeeEstimateResponse
{
    private String requestNo;

    private String estimateView;

    private String selectionMode;

    private Long buyerId;

    private String buyerCode;

    private String buyerName;

    private String buyerLevel;

    private String originWarehouseCode;

    private Long quoteSchemeId;

    private String quoteSchemeName;

    private String packageInputMode;

    private FeeEstimatePackageSummary packageSummary;

    private List<FeeEstimateSkuSnapshot> skuSnapshots;

    private FeeEstimateResolveSummary resolveSummary;

    private List<FeeEstimateRouteCandidate> routeCandidates;

    private List<FeeEstimateChannelResult> results;

    public String getRequestNo()
    {
        return requestNo;
    }

    public void setRequestNo(String requestNo)
    {
        this.requestNo = requestNo;
    }

    public String getEstimateView()
    {
        return estimateView;
    }

    public void setEstimateView(String estimateView)
    {
        this.estimateView = estimateView;
    }

    public String getSelectionMode()
    {
        return selectionMode;
    }

    public void setSelectionMode(String selectionMode)
    {
        this.selectionMode = selectionMode;
    }

    public Long getBuyerId()
    {
        return buyerId;
    }

    public void setBuyerId(Long buyerId)
    {
        this.buyerId = buyerId;
    }

    public String getBuyerCode()
    {
        return buyerCode;
    }

    public void setBuyerCode(String buyerCode)
    {
        this.buyerCode = buyerCode;
    }

    public String getBuyerName()
    {
        return buyerName;
    }

    public void setBuyerName(String buyerName)
    {
        this.buyerName = buyerName;
    }

    public String getBuyerLevel()
    {
        return buyerLevel;
    }

    public void setBuyerLevel(String buyerLevel)
    {
        this.buyerLevel = buyerLevel;
    }

    public String getOriginWarehouseCode()
    {
        return originWarehouseCode;
    }

    public void setOriginWarehouseCode(String originWarehouseCode)
    {
        this.originWarehouseCode = originWarehouseCode;
    }

    public Long getQuoteSchemeId()
    {
        return quoteSchemeId;
    }

    public void setQuoteSchemeId(Long quoteSchemeId)
    {
        this.quoteSchemeId = quoteSchemeId;
    }

    public String getQuoteSchemeName()
    {
        return quoteSchemeName;
    }

    public void setQuoteSchemeName(String quoteSchemeName)
    {
        this.quoteSchemeName = quoteSchemeName;
    }

    public String getPackageInputMode()
    {
        return packageInputMode;
    }

    public void setPackageInputMode(String packageInputMode)
    {
        this.packageInputMode = packageInputMode;
    }

    public FeeEstimatePackageSummary getPackageSummary()
    {
        return packageSummary;
    }

    public void setPackageSummary(FeeEstimatePackageSummary packageSummary)
    {
        this.packageSummary = packageSummary;
    }

    public List<FeeEstimateSkuSnapshot> getSkuSnapshots()
    {
        return skuSnapshots;
    }

    public void setSkuSnapshots(List<FeeEstimateSkuSnapshot> skuSnapshots)
    {
        this.skuSnapshots = skuSnapshots;
    }

    public FeeEstimateResolveSummary getResolveSummary()
    {
        return resolveSummary;
    }

    public void setResolveSummary(FeeEstimateResolveSummary resolveSummary)
    {
        this.resolveSummary = resolveSummary;
    }

    public List<FeeEstimateRouteCandidate> getRouteCandidates()
    {
        return routeCandidates;
    }

    public void setRouteCandidates(List<FeeEstimateRouteCandidate> routeCandidates)
    {
        this.routeCandidates = routeCandidates;
    }

    public List<FeeEstimateChannelResult> getResults()
    {
        return results;
    }

    public void setResults(List<FeeEstimateChannelResult> results)
    {
        this.results = results;
    }
}
