package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 客户渠道绑定系统渠道请求。
 */
public class LogisticsCustomerChannelSystemMappingRequest
{
    @NotBlank(message = "系统渠道代码不能为空")
    @Size(max = 64, message = "系统渠道代码不能超过64个字符")
    private String systemChannelCode;

    @Size(max = 16, message = "状态不能超过16个字符")
    private String status;

    private Integer displayOrder;

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
