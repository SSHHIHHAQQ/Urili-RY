package com.ruoyi.logistics.domain.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * 系统渠道下单规则请求。
 */
public class LogisticsSystemChannelOrderSettingRequest
{
    @Size(max = 500, message = "目的国家/地区集合不能超过500个字符")
    private String destinationCountries;

    @DecimalMin(value = "0", message = "最小重量不能小于0")
    private BigDecimal minWeight;

    @DecimalMin(value = "0", message = "最大重量不能小于0")
    private BigDecimal maxWeight;

    @DecimalMin(value = "0", message = "最大长度不能小于0")
    private BigDecimal maxLength;

    @DecimalMin(value = "0", message = "最大宽度不能小于0")
    private BigDecimal maxWidth;

    @DecimalMin(value = "0", message = "最大高度不能小于0")
    private BigDecimal maxHeight;

    @DecimalMin(value = "0", message = "最大围长不能小于0")
    private BigDecimal maxGirth;

    @Size(max = 64, message = "签名服务不能超过64个字符")
    private String signatureService;

    @Size(max = 16, message = "校验模式不能超过16个字符")
    private String validationMode;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public String getDestinationCountries()
    {
        return destinationCountries;
    }

    public void setDestinationCountries(String destinationCountries)
    {
        this.destinationCountries = destinationCountries;
    }

    public BigDecimal getMinWeight()
    {
        return minWeight;
    }

    public void setMinWeight(BigDecimal minWeight)
    {
        this.minWeight = minWeight;
    }

    public BigDecimal getMaxWeight()
    {
        return maxWeight;
    }

    public void setMaxWeight(BigDecimal maxWeight)
    {
        this.maxWeight = maxWeight;
    }

    public BigDecimal getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(BigDecimal maxLength)
    {
        this.maxLength = maxLength;
    }

    public BigDecimal getMaxWidth()
    {
        return maxWidth;
    }

    public void setMaxWidth(BigDecimal maxWidth)
    {
        this.maxWidth = maxWidth;
    }

    public BigDecimal getMaxHeight()
    {
        return maxHeight;
    }

    public void setMaxHeight(BigDecimal maxHeight)
    {
        this.maxHeight = maxHeight;
    }

    public BigDecimal getMaxGirth()
    {
        return maxGirth;
    }

    public void setMaxGirth(BigDecimal maxGirth)
    {
        this.maxGirth = maxGirth;
    }

    public String getSignatureService()
    {
        return signatureService;
    }

    public void setSignatureService(String signatureService)
    {
        this.signatureService = signatureService;
    }

    public String getValidationMode()
    {
        return validationMode;
    }

    public void setValidationMode(String validationMode)
    {
        this.validationMode = validationMode;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
