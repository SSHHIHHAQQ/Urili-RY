package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 物流商渠道映射请求。
 */
public class LogisticsChannelMappingRequest
{
    @NotBlank(message = "物流商渠道代码不能为空")
    @Size(max = 128, message = "物流商渠道代码不能超过128个字符")
    private String externalChannelCode;

    @NotBlank(message = "系统渠道代码不能为空")
    @Size(max = 64, message = "系统渠道代码不能超过64个字符")
    private String systemChannelCode;

    @NotBlank(message = "标准最终承运商不能为空")
    @Size(max = 64, message = "标准最终承运商不能超过64个字符")
    private String standardCarrierCode;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public String getExternalChannelCode()
    {
        return externalChannelCode;
    }

    public void setExternalChannelCode(String externalChannelCode)
    {
        this.externalChannelCode = externalChannelCode;
    }

    public String getSystemChannelCode()
    {
        return systemChannelCode;
    }

    public void setSystemChannelCode(String systemChannelCode)
    {
        this.systemChannelCode = systemChannelCode;
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
