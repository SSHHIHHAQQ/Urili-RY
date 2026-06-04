package com.ruoyi.integration.lingxing;

/**
 * 领星接口凭证。
 */
public class LingxingCredentials
{
    private final String appKey;

    private final String appSecret;

    public LingxingCredentials(String appKey, String appSecret)
    {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    public String getAppKey()
    {
        return appKey;
    }

    public String getAppSecret()
    {
        return appSecret;
    }
}
