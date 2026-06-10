package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 系统渠道维度的物流商渠道映射请求。
 */
public class LogisticsSystemChannelCarrierMappingRequest
{
    @NotNull(message = "物流商账号不能为空")
    private Long carrierAccountId;

    @NotBlank(message = "物流商渠道代码不能为空")
    @Size(max = 128, message = "物流商渠道代码不能超过128个字符")
    private String externalChannelCode;

    @NotBlank(message = "标准最终承运商不能为空")
    @Size(max = 64, message = "标准最终承运商不能超过64个字符")
    private String standardCarrierCode;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public Long getCarrierAccountId()
    {
        return carrierAccountId;
    }

    public void setCarrierAccountId(Long carrierAccountId)
    {
        this.carrierAccountId = carrierAccountId;
    }

    public String getExternalChannelCode()
    {
        return externalChannelCode;
    }

    public void setExternalChannelCode(String externalChannelCode)
    {
        this.externalChannelCode = externalChannelCode;
    }

    public String getStandardCarrierCode()
    {
        return standardCarrierCode;
    }

    public void setStandardCarrierCode(String standardCarrierCode)
    {
        this.standardCarrierCode = standardCarrierCode;
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
