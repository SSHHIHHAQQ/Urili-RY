package com.ruoyi.logistics.agg56;

/**
 * AGG56 可用物流产品。
 */
public class Agg56ShippingMethod
{
    private String code;

    private String name;

    private String sourcePayloadJson;

    private String sourcePayloadHash;

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSourcePayloadJson()
    {
        return sourcePayloadJson;
    }

    public void setSourcePayloadJson(String sourcePayloadJson)
    {
        this.sourcePayloadJson = sourcePayloadJson;
    }

    public String getSourcePayloadHash()
    {
        return sourcePayloadHash;
    }

    public void setSourcePayloadHash(String sourcePayloadHash)
    {
        this.sourcePayloadHash = sourcePayloadHash;
    }
}
