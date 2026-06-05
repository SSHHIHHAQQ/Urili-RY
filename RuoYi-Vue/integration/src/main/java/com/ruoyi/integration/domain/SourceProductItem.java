package com.ruoyi.integration.domain;

/**
 * 来源商品库列表项。
 */
public class SourceProductItem extends UpstreamSkuSyncItem
{
    private String systemKind;

    private String systemKindLabel;

    private String masterWarehouseName;

    public String getSystemKind()
    {
        return systemKind;
    }

    public void setSystemKind(String systemKind)
    {
        this.systemKind = systemKind;
    }

    public String getSystemKindLabel()
    {
        return systemKindLabel;
    }

    public void setSystemKindLabel(String systemKindLabel)
    {
        this.systemKindLabel = systemKindLabel;
    }

    public String getMasterWarehouseName()
    {
        return masterWarehouseName;
    }

    public void setMasterWarehouseName(String masterWarehouseName)
    {
        this.masterWarehouseName = masterWarehouseName;
    }
}
