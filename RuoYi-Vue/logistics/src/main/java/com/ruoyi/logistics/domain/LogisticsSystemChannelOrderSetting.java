package com.ruoyi.logistics.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统渠道第一版下单规则。
 */
public class LogisticsSystemChannelOrderSetting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long settingId;
    private String systemChannelCode;
    private String destinationCountries;
    private BigDecimal minWeight;
    private BigDecimal maxWeight;
    private BigDecimal maxLength;
    private BigDecimal maxWidth;
    private BigDecimal maxHeight;
    private BigDecimal maxGirth;
    private String signatureService;
    private String validationMode;

    public Long getSettingId()
    {
        return settingId;
    }

    public void setSettingId(Long settingId)
    {
        this.settingId = settingId;
    }

    public String getSystemChannelCode()
    {
        return systemChannelCode;
    }

    public void setSystemChannelCode(String systemChannelCode)
    {
        this.systemChannelCode = systemChannelCode;
    }

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
}
