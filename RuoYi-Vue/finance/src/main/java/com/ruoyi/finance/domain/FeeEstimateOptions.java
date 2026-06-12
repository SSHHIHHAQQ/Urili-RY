package com.ruoyi.finance.domain;

import java.util.List;

/**
 * Option payload for the fee estimate page.
 */
public class FeeEstimateOptions
{
    private List<FeeEstimateQuoteSchemeOption> quoteSchemes;

    private List<QuoteSchemeOption> buyers;

    private List<QuoteSchemeOption> warehouses;

    private List<QuoteSchemeOption> channels;

    private List<QuoteSchemeOption> customerChannels;

    private FeeEstimateQuoteSchemeOption selectedScheme;

    public List<FeeEstimateQuoteSchemeOption> getQuoteSchemes()
    {
        return quoteSchemes;
    }

    public void setQuoteSchemes(List<FeeEstimateQuoteSchemeOption> quoteSchemes)
    {
        this.quoteSchemes = quoteSchemes;
    }

    public List<QuoteSchemeOption> getBuyers()
    {
        return buyers;
    }

    public void setBuyers(List<QuoteSchemeOption> buyers)
    {
        this.buyers = buyers;
    }

    public List<QuoteSchemeOption> getWarehouses()
    {
        return warehouses;
    }

    public void setWarehouses(List<QuoteSchemeOption> warehouses)
    {
        this.warehouses = warehouses;
    }

    public List<QuoteSchemeOption> getChannels()
    {
        return channels;
    }

    public void setChannels(List<QuoteSchemeOption> channels)
    {
        this.channels = channels;
    }

    public List<QuoteSchemeOption> getCustomerChannels()
    {
        return customerChannels;
    }

    public void setCustomerChannels(List<QuoteSchemeOption> customerChannels)
    {
        this.customerChannels = customerChannels;
    }

    public FeeEstimateQuoteSchemeOption getSelectedScheme()
    {
        return selectedScheme;
    }

    public void setSelectedScheme(FeeEstimateQuoteSchemeOption selectedScheme)
    {
        this.selectedScheme = selectedScheme;
    }
}
