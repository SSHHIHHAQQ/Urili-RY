package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AGG56 凭据保存请求。
 */
public class Agg56CredentialRequest
{
    @NotBlank(message = "app_token不能为空")
    @Size(max = 255, message = "app_token不能超过255个字符")
    private String appToken;

    @NotBlank(message = "app_key不能为空")
    @Size(max = 255, message = "app_key不能超过255个字符")
    private String appKey;

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
}
