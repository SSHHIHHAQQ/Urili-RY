package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 物流商接入保存请求。
 */
public class LogisticsConnectionRequest
{
    @NotBlank(message = "物流商系统不能为空")
    @Size(max = 32, message = "物流商系统不能超过32个字符")
    private String providerKind;

    @NotBlank(message = "物流商名称不能为空")
    @Size(max = 200, message = "物流商名称不能超过200个字符")
    private String carrierName;

    @Size(max = 500, message = "API Base URL不能超过500个字符")
    private String apiBaseUrl;

    @Size(max = 255, message = "app_token不能超过255个字符")
    private String appToken;

    @Size(max = 255, message = "app_key不能超过255个字符")
    private String appKey;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public String getProviderKind()
    {
        return providerKind;
    }

    public void setProviderKind(String providerKind)
    {
        this.providerKind = providerKind;
    }

    public String getCarrierName()
    {
        return carrierName;
    }

    public void setCarrierName(String carrierName)
    {
        this.carrierName = carrierName;
    }

    public String getApiBaseUrl()
    {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl)
    {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getAppToken()
    {
        return appToken;
    }

    public void setAppToken(String appToken)
    {
        this.appToken = appToken;
    }

    public String getAppKey()
    {
        return appKey;
    }

    public void setAppKey(String appKey)
    {
        this.appKey = appKey;
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
