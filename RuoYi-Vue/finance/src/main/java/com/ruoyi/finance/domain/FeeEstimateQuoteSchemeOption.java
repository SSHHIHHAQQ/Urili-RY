package com.ruoyi.finance.domain;

import java.util.List;

/**
 * Enabled quote scheme option for fee estimate.
 */
public class FeeEstimateQuoteSchemeOption
{
    private Long schemeId;

    private Long value;

    private String label;

    private String schemeCode;

    private String schemeName;

    private String schemeType;

    private String feeSourceMode;

    private String currencyCode;

    private List<QuoteSchemeOption> channels;

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    public Long getValue()
    {
        return value;
    }

    public void setValue(Long value)
    {
        this.value = value;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getSchemeCode()
    {
        return schemeCode;
    }

    public void setSchemeCode(String schemeCode)
    {
        this.schemeCode = schemeCode;
    }

    public String getSchemeName()
    {
        return schemeName;
    }

    public void setSchemeName(String schemeName)
    {
        this.schemeName = schemeName;
    }

    public String getSchemeType()
    {
        return schemeType;
    }

    public void setSchemeType(String schemeType)
    {
        this.schemeType = schemeType;
    }

    public String getFeeSourceMode()
    {
        return feeSourceMode;
    }

    public void setFeeSourceMode(String feeSourceMode)
    {
        this.feeSourceMode = feeSourceMode;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public List<QuoteSchemeOption> getChannels()
    {
        return channels;
    }

    public void setChannels(List<QuoteSchemeOption> channels)
    {
        this.channels = channels;
    }
}
