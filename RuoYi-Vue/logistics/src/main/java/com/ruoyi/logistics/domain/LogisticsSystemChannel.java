package com.ruoyi.logistics.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统渠道。
 */
public class LogisticsSystemChannel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String systemChannelCode;

    private String systemChannelName;

    private String fulfillmentMode;

    private String standardCarrierCode;

    private String signatureServices;

    private String status;

    private Integer displayOrder;

    private Integer carrierMappingCount;

    private Integer warehouseCount;

    private String carrierAccountSummary;

    private String warehouseSummary;

    private String orderRuleSummary;

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

    public Integer getCarrierMappingCount()
    {
        return carrierMappingCount;
    }

    public void setCarrierMappingCount(Integer carrierMappingCount)
    {
        this.carrierMappingCount = carrierMappingCount;
    }

    public Integer getWarehouseCount()
    {
        return warehouseCount;
    }

    public void setWarehouseCount(Integer warehouseCount)
    {
        this.warehouseCount = warehouseCount;
    }

    public String getCarrierAccountSummary()
    {
        return carrierAccountSummary;
    }

    public void setCarrierAccountSummary(String carrierAccountSummary)
    {
        this.carrierAccountSummary = carrierAccountSummary;
    }

    public String getWarehouseSummary()
    {
        return warehouseSummary;
    }

    public void setWarehouseSummary(String warehouseSummary)
    {
        this.warehouseSummary = warehouseSummary;
    }

    public String getOrderRuleSummary()
    {
        return orderRuleSummary;
    }

    public void setOrderRuleSummary(String orderRuleSummary)
    {
        this.orderRuleSummary = orderRuleSummary;
    }
}
