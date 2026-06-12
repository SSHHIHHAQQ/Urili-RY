package com.ruoyi.finance.domain;

/**
 * Candidate route produced before external fee estimate calls.
 */
public class FeeEstimateRouteCandidate
{
    private Long schemeId;

    private Long schemeChannelId;

    private String schemeName;

    private String feeSourceMode;

    private String currencyCode;

    private String warehouseCode;

    private String warehouseName;

    private String warehouseKind;

    private String countryCode;

    private String customerChannelCode;

    private String customerChannelName;

    private String labelUploadRequired;

    private String buyerScopeMode;

    private String systemChannelCode;

    private String systemChannelName;

    private String fulfillmentMode;

    private String carrierConnectionCode;

    private String carrierExternalChannelCode;

    private Boolean executable;

    private String failureCode;

    private String failureMessage;

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    public Long getSchemeChannelId()
    {
        return schemeChannelId;
    }

    public void setSchemeChannelId(Long schemeChannelId)
    {
        this.schemeChannelId = schemeChannelId;
    }

    public String getSchemeName()
    {
        return schemeName;
    }

    public void setSchemeName(String schemeName)
    {
        this.schemeName = schemeName;
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

    public String getWarehouseCode()
    {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode)
    {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName()
    {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName)
    {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseKind()
    {
        return warehouseKind;
    }

    public void setWarehouseKind(String warehouseKind)
    {
        this.warehouseKind = warehouseKind;
    }

    public String getCountryCode()
    {
        return countryCode;
    }

    public void setCountryCode(String countryCode)
    {
        this.countryCode = countryCode;
    }

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

    public String getLabelUploadRequired()
    {
        return labelUploadRequired;
    }

    public void setLabelUploadRequired(String labelUploadRequired)
    {
        this.labelUploadRequired = labelUploadRequired;
    }

    public String getBuyerScopeMode()
    {
        return buyerScopeMode;
    }

    public void setBuyerScopeMode(String buyerScopeMode)
    {
        this.buyerScopeMode = buyerScopeMode;
    }

    public String getSystemChannelCode()
    {
        return systemChannelCode;
    }

    public void setSystemChannelCode(String systemChannelCode)
    {
        this.systemChannelCode = systemChannelCode;
    }

    public String getSystemChannelName()
    {
        return systemChannelName;
    }

    public void setSystemChannelName(String systemChannelName)
    {
        this.systemChannelName = systemChannelName;
    }

    public String getFulfillmentMode()
    {
        return fulfillmentMode;
    }

    public void setFulfillmentMode(String fulfillmentMode)
    {
        this.fulfillmentMode = fulfillmentMode;
    }

    public String getCarrierConnectionCode()
    {
        return carrierConnectionCode;
    }

    public void setCarrierConnectionCode(String carrierConnectionCode)
    {
        this.carrierConnectionCode = carrierConnectionCode;
    }

    public String getCarrierExternalChannelCode()
    {
        return carrierExternalChannelCode;
    }

    public void setCarrierExternalChannelCode(String carrierExternalChannelCode)
    {
        this.carrierExternalChannelCode = carrierExternalChannelCode;
    }

    public Boolean getExecutable()
    {
        return executable;
    }

    public void setExecutable(Boolean executable)
    {
        this.executable = executable;
    }

    public String getFailureCode()
    {
        return failureCode;
    }

    public void setFailureCode(String failureCode)
    {
        this.failureCode = failureCode;
    }

    public String getFailureMessage()
    {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage)
    {
        this.failureMessage = failureMessage;
    }
}
