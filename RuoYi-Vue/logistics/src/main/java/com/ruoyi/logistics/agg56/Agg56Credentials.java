package com.ruoyi.logistics.agg56;

/**
 * AGG56 凭据。
 */
public class Agg56Credentials
{
    private final String appToken;

    private final String appKey;

    public Agg56Credentials(String appToken, String appKey)
    {
        this.appToken = appToken;
        this.appKey = appKey;
    }

    public String getAppToken()
    {
        return appToken;
    }

    public String getAppKey()
    {
        return appKey;
    }
}
