package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 系统渠道请求。
 */
public class LogisticsSystemChannelRequest
{
    @NotBlank(message = "系统渠道代码不能为空")
    @Size(max = 64, message = "系统渠道代码不能超过64个字符")
    private String systemChannelCode;

    @NotBlank(message = "系统渠道名称不能为空")
    @Size(max = 200, message = "系统渠道名称不能超过200个字符")
    private String systemChannelName;

    @NotBlank(message = "渠道履约模式不能为空")
    @Size(max = 32, message = "渠道履约模式不能超过32个字符")
    private String fulfillmentMode;

    @NotBlank(message = "标准最终承运商不能为空")
    @Size(max = 64, message = "标准最终承运商不能超过64个字符")
    private String standardCarrierCode;

    @Size(max = 128, message = "签名服务不能超过128个字符")
    private String signatureServices;

    @Size(max = 16, message = "状态不能超过16个字符")
    private String status;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

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

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
