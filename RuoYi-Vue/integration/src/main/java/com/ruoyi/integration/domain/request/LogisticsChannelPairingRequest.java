package com.ruoyi.integration.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 物流渠道配对请求。
 */
public class LogisticsChannelPairingRequest
{
    @NotBlank(message = "领星渠道代码不能为空")
    private String upstreamChannelCode;

    @NotBlank(message = "系统渠道代码不能为空")
    private String systemChannelCode;

    @NotBlank(message = "系统渠道名称不能为空")
    private String systemChannelName;

    private String pairingRole;

    private String remark;

    public String getUpstreamChannelCode() { return upstreamChannelCode; }
    public void setUpstreamChannelCode(String upstreamChannelCode) { this.upstreamChannelCode = upstreamChannelCode; }
    public String getSystemChannelCode() { return systemChannelCode; }
    public void setSystemChannelCode(String systemChannelCode) { this.systemChannelCode = systemChannelCode; }
    public String getSystemChannelName() { return systemChannelName; }
    public void setSystemChannelName(String systemChannelName) { this.systemChannelName = systemChannelName; }
    public String getPairingRole() { return pairingRole; }
    public void setPairingRole(String pairingRole) { this.pairingRole = pairingRole; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
