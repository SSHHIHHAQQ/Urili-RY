package com.ruoyi.warehouse.domain;

/**
 * 官方仓同步可选主仓。
 */
public class WarehouseSyncConnection
{
    private String connectionCode;
    private String masterWarehouseName;
    private String systemKind;

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getMasterWarehouseName()
    {
        return masterWarehouseName;
    }

    public void setMasterWarehouseName(String masterWarehouseName)
    {
        this.masterWarehouseName = masterWarehouseName;
    }

    public String getSystemKind()
    {
        return systemKind;
    }

    public void setSystemKind(String systemKind)
    {
        this.systemKind = systemKind;
    }
}
