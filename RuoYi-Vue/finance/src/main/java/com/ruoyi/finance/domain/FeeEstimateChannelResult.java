package com.ruoyi.finance.domain;

import java.math.BigDecimal;

/**
 * Per-channel estimate result row.
 */
public class FeeEstimateChannelResult
{
    private Long schemeChannelId;

    private String selectionMode;

    private String warehouseCode;

    private String warehouseName;

    private String systemChannelCode;

    private String systemChannelName;

    private String fulfillmentMode;

    private String channelCode;

    private String channelName;

    private String schemeType;

    private String feeSourceMode;

    private String currencyCode;

    private Boolean success;

    private Boolean recommended;

    private BigDecimal totalAmount;

    private BigDecimal basicFreightAmount;

    private BigDecimal surchargeAmount;

    private BigDecimal operationFeeAmount;

    private BigDecimal packageMaterialFeeAmount;

    private BigDecimal actualWeightKg;

    private BigDecimal volumeWeightKg;

    private BigDecimal chargeableWeightKg;

    private Integer packageCount;

    private String errorCode;

    private String errorMessage;

    private String traceId;

    public String getSelectionMode()
    {
        return selectionMode;
    }

    public void setSelectionMode(String selectionMode)
    {
        this.selectionMode = selectionMode;
    }

    public Long getSchemeChannelId()
    {
        return schemeChannelId;
    }

    public void setSchemeChannelId(Long schemeChannelId)
    {
        this.schemeChannelId = schemeChannelId;
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

    public String getChannelCode()
    {
        return channelCode;
    }

    public void setChannelCode(String channelCode)
    {
        this.channelCode = channelCode;
    }

    public String getChannelName()
    {
        return channelName;
    }

    public void setChannelName(String channelName)
    {
        this.channelName = channelName;
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

    public Boolean getSuccess()
    {
        return success;
    }

    public void setSuccess(Boolean success)
    {
        this.success = success;
    }

    public Boolean getRecommended()
    {
        return recommended;
    }

    public void setRecommended(Boolean recommended)
    {
        this.recommended = recommended;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getBasicFreightAmount()
    {
        return basicFreightAmount;
    }

    public void setBasicFreightAmount(BigDecimal basicFreightAmount)
    {
        this.basicFreightAmount = basicFreightAmount;
    }

    public BigDecimal getSurchargeAmount()
    {
        return surchargeAmount;
    }

    public void setSurchargeAmount(BigDecimal surchargeAmount)
    {
        this.surchargeAmount = surchargeAmount;
    }

    public BigDecimal getOperationFeeAmount()
    {
        return operationFeeAmount;
    }

    public void setOperationFeeAmount(BigDecimal operationFeeAmount)
    {
        this.operationFeeAmount = operationFeeAmount;
    }

    public BigDecimal getPackageMaterialFeeAmount()
    {
        return packageMaterialFeeAmount;
    }

    public void setPackageMaterialFeeAmount(BigDecimal packageMaterialFeeAmount)
    {
        this.packageMaterialFeeAmount = packageMaterialFeeAmount;
    }

    public BigDecimal getActualWeightKg()
    {
        return actualWeightKg;
    }

    public void setActualWeightKg(BigDecimal actualWeightKg)
    {
        this.actualWeightKg = actualWeightKg;
    }

    public BigDecimal getVolumeWeightKg()
    {
        return volumeWeightKg;
    }

    public void setVolumeWeightKg(BigDecimal volumeWeightKg)
    {
        this.volumeWeightKg = volumeWeightKg;
    }

    public BigDecimal getChargeableWeightKg()
    {
        return chargeableWeightKg;
    }

    public void setChargeableWeightKg(BigDecimal chargeableWeightKg)
    {
        this.chargeableWeightKg = chargeableWeightKg;
    }

    public Integer getPackageCount()
    {
        return packageCount;
    }

    public void setPackageCount(Integer packageCount)
    {
        this.packageCount = packageCount;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(String errorCode)
    {
        this.errorCode = errorCode;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }
}
