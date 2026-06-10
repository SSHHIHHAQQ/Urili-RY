package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Customer logistics channel quote-channel mapping request.
 */
public class LogisticsCustomerChannelQuoteMappingRequest
{
    @NotBlank(message = "上游系统不能为空")
    private String connectionCode;

    @NotBlank(message = "主仓渠道不能为空")
    private String upstreamChannelCode;

    private String remark;

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getUpstreamChannelCode()
    {
        return upstreamChannelCode;
    }

    public void setUpstreamChannelCode(String upstreamChannelCode)
    {
        this.upstreamChannelCode = upstreamChannelCode;
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
