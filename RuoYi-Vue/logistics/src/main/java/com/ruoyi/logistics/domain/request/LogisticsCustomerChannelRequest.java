package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 客户渠道请求。
 */
public class LogisticsCustomerChannelRequest
{
    @NotBlank(message = "客户渠道代码不能为空")
    @Size(max = 64, message = "客户渠道代码不能超过64个字符")
    private String customerChannelCode;

    @NotBlank(message = "客户渠道名称不能为空")
    @Size(max = 200, message = "客户渠道名称不能超过200个字符")
    private String customerChannelName;

    @NotBlank(message = "渠道类型不能为空")
    @Size(max = 32, message = "渠道类型不能超过32个字符")
    private String channelType;

    @NotBlank(message = "标准最终承运商不能为空")
    @Size(max = 64, message = "标准最终承运商不能超过64个字符")
    private String standardCarrierCode;

    @Size(max = 128, message = "签名服务不能超过128个字符")
    private String signatureServices;

    @Size(max = 16, message = "上传物流面单不能超过16个字符")
    private String labelUploadRequired;

    @Size(max = 16, message = "平台面单获取不能超过16个字符")
    private String platformLabelFetch;

    @Size(max = 16, message = "客户上传面单支持不能超过16个字符")
    private String customerLabelUploadSupported;

    @Size(max = 16, message = "状态不能超过16个字符")
    private String status;

    private Integer displayOrder;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public String getCustomerChannelCode()
    {
        return customerChannelCode;
    }

    public void setCustomerChannelCode(String customerChannelCode)
    {
        this.customerChannelCode = customerChannelCode;
    }

    public String getCustomerChannelName()
    {
        return customerChannelName;
    }

    public void setCustomerChannelName(String customerChannelName)
    {
        this.customerChannelName = customerChannelName;
    }

    public String getChannelType()
    {
        return channelType;
    }

    public void setChannelType(String channelType)
    {
        this.channelType = channelType;
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

    public String getLabelUploadRequired()
    {
        return labelUploadRequired;
    }

    public void setLabelUploadRequired(String labelUploadRequired)
    {
        this.labelUploadRequired = labelUploadRequired;
    }

    public String getPlatformLabelFetch()
    {
        return platformLabelFetch;
    }

    public void setPlatformLabelFetch(String platformLabelFetch)
    {
        this.platformLabelFetch = platformLabelFetch;
    }

    public String getCustomerLabelUploadSupported()
    {
        return customerLabelUploadSupported;
    }

    public void setCustomerLabelUploadSupported(String customerLabelUploadSupported)
    {
        this.customerLabelUploadSupported = customerLabelUploadSupported;
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

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
