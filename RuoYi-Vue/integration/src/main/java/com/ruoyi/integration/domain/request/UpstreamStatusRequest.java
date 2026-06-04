package com.ruoyi.integration.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 主仓接入状态请求。
 */
public class UpstreamStatusRequest
{
    @NotBlank(message = "状态不能为空")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
