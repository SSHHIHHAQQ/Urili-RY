package com.ruoyi.integration.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 凭证更新请求。
 */
public class UpstreamCredentialRequest
{
    @NotBlank(message = "appKey不能为空")
    private String appKey;

    @NotBlank(message = "appSecret不能为空")
    private String appSecret;

    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
}
