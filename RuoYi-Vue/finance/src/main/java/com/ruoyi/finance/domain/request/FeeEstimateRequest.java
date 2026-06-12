package com.ruoyi.finance.domain.request;

import java.util.List;

/**
 * Fee estimate calculation request.
 */
public class FeeEstimateRequest
{
    private String estimateView;

    private String selectionMode;

    private Long buyerId;

    private String packageInputMode;

    private String originWarehouseCode;

    private List<String> warehouseCodes;

    private String customerChannelCode;

    private String destinationCountryCode;

    private String destinationState;

    private String destinationCity;

    private String destinationPostalCode;

    private String destinationAddress1;

    private String destinationAddress2;

    private Long quoteSchemeId;

    private List<String> channelCodes;

    private List<FeeEstimatePackageLine> packageLines;

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

    public String getPackageInputMode()
    {
        return packageInputMode;
    }

    public void setPackageInputMode(String packageInputMode)
    {
        this.packageInputMode = packageInputMode;
    }

    public String getOriginWarehouseCode()
    {
        return originWarehouseCode;
    }

    public void setOriginWarehouseCode(String originWarehouseCode)
    {
        this.originWarehouseCode = originWarehouseCode;
    }

    public List<String> getWarehouseCodes()
    {
        return warehouseCodes;
    }

    public void setWarehouseCodes(List<String> warehouseCodes)
    {
        this.warehouseCodes = warehouseCodes;
    }

    public String getCustomerChannelCode()
    {
        return customerChannelCode;
    }

    public void setCustomerChannelCode(String customerChannelCode)
    {
        this.customerChannelCode = customerChannelCode;
    }

    public String getDestinationCountryCode()
    {
        return destinationCountryCode;
    }

    public void setDestinationCountryCode(String destinationCountryCode)
    {
        this.destinationCountryCode = destinationCountryCode;
    }

    public String getDestinationState()
    {
        return destinationState;
    }

    public void setDestinationState(String destinationState)
    {
        this.destinationState = destinationState;
    }

    public String getDestinationCity()
    {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity)
    {
        this.destinationCity = destinationCity;
    }

    public String getDestinationPostalCode()
    {
        return destinationPostalCode;
    }

    public void setDestinationPostalCode(String destinationPostalCode)
    {
        this.destinationPostalCode = destinationPostalCode;
    }

    public String getDestinationAddress1()
    {
        return destinationAddress1;
    }

    public void setDestinationAddress1(String destinationAddress1)
    {
        this.destinationAddress1 = destinationAddress1;
    }

    public String getDestinationAddress2()
    {
        return destinationAddress2;
    }

    public void setDestinationAddress2(String destinationAddress2)
    {
        this.destinationAddress2 = destinationAddress2;
    }

    public Long getQuoteSchemeId()
    {
        return quoteSchemeId;
    }

    public void setQuoteSchemeId(Long quoteSchemeId)
    {
        this.quoteSchemeId = quoteSchemeId;
    }

    public List<String> getChannelCodes()
    {
        return channelCodes;
    }

    public void setChannelCodes(List<String> channelCodes)
    {
        this.channelCodes = channelCodes;
    }

    public List<FeeEstimatePackageLine> getPackageLines()
    {
        return packageLines;
    }

    public void setPackageLines(List<FeeEstimatePackageLine> packageLines)
    {
        this.packageLines = packageLines;
    }
}
