package com.ruoyi.finance.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Normalized result returned by an external fee estimate adapter.
 */
public class FeeEstimateExternalResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Boolean success;

    private BigDecimal totalAmount;

    private BigDecimal basicFreightAmount;

    private BigDecimal surchargeAmount;

    private BigDecimal operationFeeAmount;

    private BigDecimal packageMaterialFeeAmount;

    private String currencyCode;

    private String traceId;

    private String errorCode;

    private String errorMessage;

    public Boolean getSuccess()
    {
        return success;
    }

    public void setSuccess(Boolean success)
    {
        this.success = success;
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

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
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
}
