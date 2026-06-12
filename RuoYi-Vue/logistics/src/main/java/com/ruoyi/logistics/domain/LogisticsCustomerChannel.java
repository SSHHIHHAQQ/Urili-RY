package com.ruoyi.logistics.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户渠道。
 */
public class LogisticsCustomerChannel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String customerChannelCode;

    private String customerChannelName;

    private String channelType;

    private String standardCarrierCode;

    private String signatureServices;

    private String labelUploadRequired;

    private String platformLabelFetch;

    private String customerLabelUploadSupported;

    private String buyerScopeMode;

    private String status;

    private Integer displayOrder;

    private Integer systemMappingCount;

    private Integer buyerScopeCount;

    private String systemChannelSummary;

    private String quoteMasterWarehouseSummary;

    private String quoteUpstreamChannelSummary;

    private String buyerScopeSummary;

    public String getCustomerChannelCode()
    {
        return customerChannelCode;
    }

    public void setCustomerChannelCode(String customerChannelCode)
    {
        this.customerChannelCode = customerChannelCode;
    }

    public String getCustomerChannelName()
    {
        return customerChannelName;
    }

    public void setCustomerChannelName(String customerChannelName)
    {
        this.customerChannelName = customerChannelName;
    }

    public String getChannelType()
    {
        return channelType;
    }

    public void setChannelType(String channelType)
    {
        this.channelType = channelType;
    }

    public String getStandardCarrierCode()
    {
        return standardCarrierCode;
    }

    public void setStandardCarrierCode(String standardCarrierCode)
    {
        this.standardCarrierCode = standardCarrierCode;
    }

    public String getSignatureServices()
    {
        return signatureServices;
    }

    public void setSignatureServices(String signatureServices)
    {
        this.signatureServices = signatureServices;
    }

    public String getLabelUploadRequired()
    {
        return labelUploadRequired;
    }

    public void setLabelUploadRequired(String labelUploadRequired)
    {
        this.labelUploadRequired = labelUploadRequired;
    }

    public String getPlatformLabelFetch()
    {
        return platformLabelFetch;
    }

    public void setPlatformLabelFetch(String platformLabelFetch)
    {
        this.platformLabelFetch = platformLabelFetch;
    }

    public String getCustomerLabelUploadSupported()
    {
        return customerLabelUploadSupported;
    }

    public void setCustomerLabelUploadSupported(String customerLabelUploadSupported)
    {
        this.customerLabelUploadSupported = customerLabelUploadSupported;
    }

    public String getBuyerScopeMode()
    {
        return buyerScopeMode;
    }

    public void setBuyerScopeMode(String buyerScopeMode)
    {
        this.buyerScopeMode = buyerScopeMode;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getDisplayOrder()
    {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder)
    {
        this.displayOrder = displayOrder;
    }

    public Integer getSystemMappingCount()
    {
        return systemMappingCount;
    }

    public void setSystemMappingCount(Integer systemMappingCount)
    {
        this.systemMappingCount = systemMappingCount;
    }

    public Integer getBuyerScopeCount()
    {
        return buyerScopeCount;
    }

    public void setBuyerScopeCount(Integer buyerScopeCount)
    {
        this.buyerScopeCount = buyerScopeCount;
    }

    public String getSystemChannelSummary()
    {
        return systemChannelSummary;
    }

    public void setSystemChannelSummary(String systemChannelSummary)
    {
        this.systemChannelSummary = systemChannelSummary;
    }

    public String getQuoteMasterWarehouseSummary()
    {
        return quoteMasterWarehouseSummary;
    }

    public void setQuoteMasterWarehouseSummary(String quoteMasterWarehouseSummary)
    {
        this.quoteMasterWarehouseSummary = quoteMasterWarehouseSummary;
    }

    public String getQuoteUpstreamChannelSummary()
    {
        return quoteUpstreamChannelSummary;
    }

    public void setQuoteUpstreamChannelSummary(String quoteUpstreamChannelSummary)
    {
        this.quoteUpstreamChannelSummary = quoteUpstreamChannelSummary;
    }

    public String getBuyerScopeSummary()
    {
        return buyerScopeSummary;
    }

    public void setBuyerScopeSummary(String buyerScopeSummary)
    {
        this.buyerScopeSummary = buyerScopeSummary;
    }
}
