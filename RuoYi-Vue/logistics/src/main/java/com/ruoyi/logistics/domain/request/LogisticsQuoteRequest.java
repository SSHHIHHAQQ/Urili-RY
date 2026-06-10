package com.ruoyi.logistics.domain.request;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 物流报价请求。
 */
public class LogisticsQuoteRequest
{
    @NotNull(message = "物流商账号不能为空")
    private Long carrierAccountId;

    @NotBlank(message = "系统渠道不能为空")
    @Size(max = 64, message = "系统渠道不能超过64个字符")
    private String systemChannelCode;

    private BigDecimal declaredValue;

    @Valid
    @NotNull(message = "收件地址不能为空")
    private LogisticsAddressRequest recipientAddress;

    @Valid
    private LogisticsAddressRequest shipperAddress;

    @Size(max = 32, message = "签名服务不能超过32个字符")
    private String signatureService;

    @NotNull(message = "重量尺寸单位不能为空")
    private Integer weightUnitType;

    @Valid
    @NotEmpty(message = "包裹不能为空")
    private List<LogisticsBoxRequest> boxes;

    public Long getCarrierAccountId()
    {
        return carrierAccountId;
    }

    public void setCarrierAccountId(Long carrierAccountId)
    {
        this.carrierAccountId = carrierAccountId;
    }

    public String getSystemChannelCode()
    {
        return systemChannelCode;
    }

    public void setSystemChannelCode(String systemChannelCode)
    {
        this.systemChannelCode = systemChannelCode;
    }

    public BigDecimal getDeclaredValue()
    {
        return declaredValue;
    }

    public void setDeclaredValue(BigDecimal declaredValue)
    {
        this.declaredValue = declaredValue;
    }

    public LogisticsAddressRequest getRecipientAddress()
    {
        return recipientAddress;
    }

    public void setRecipientAddress(LogisticsAddressRequest recipientAddress)
    {
        this.recipientAddress = recipientAddress;
    }

    public LogisticsAddressRequest getShipperAddress()
    {
        return shipperAddress;
    }

    public void setShipperAddress(LogisticsAddressRequest shipperAddress)
    {
        this.shipperAddress = shipperAddress;
    }

    public String getSignatureService()
    {
        return signatureService;
    }

    public void setSignatureService(String signatureService)
    {
        this.signatureService = signatureService;
    }

    public Integer getWeightUnitType()
    {
        return weightUnitType;
    }

    public void setWeightUnitType(Integer weightUnitType)
    {
        this.weightUnitType = weightUnitType;
    }

    public List<LogisticsBoxRequest> getBoxes()
    {
        return boxes;
    }

    public void setBoxes(List<LogisticsBoxRequest> boxes)
    {
        this.boxes = boxes;
    }
}
