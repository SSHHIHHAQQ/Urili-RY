package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 状态请求。
 */
public class LogisticsStatusRequest
{
    @NotBlank(message = "状态不能为空")
    private String status;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
