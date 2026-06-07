package com.ruoyi.warehouse.domain;

/**
 * 官方仓同步候选。
 */
public class WarehouseSyncCandidate
{
    private String connectionCode;
    private String masterWarehouseName;
    private String warehouseCode;
    private String warehouseName;
    private String countryCode;
    private String status;
    private Boolean paired;
    private String pairingRole;
    private String systemWarehouseCode;
    private String systemWarehouseName;

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

    public String getWarehouseCode()
    {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode)
    {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName()
    {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName)
    {
        this.warehouseName = warehouseName;
    }

    public String getCountryCode()
    {
        return countryCode;
    }

    public void setCountryCode(String countryCode)
    {
        this.countryCode = countryCode;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Boolean getPaired()
    {
        return paired;
    }

    public void setPaired(Boolean paired)
    {
        this.paired = paired;
    }

    public String getPairingRole()
    {
        return pairingRole;
    }

    public void setPairingRole(String pairingRole)
    {
        this.pairingRole = pairingRole;
    }

    public String getSystemWarehouseCode()
    {
        return systemWarehouseCode;
    }

    public void setSystemWarehouseCode(String systemWarehouseCode)
    {
        this.systemWarehouseCode = systemWarehouseCode;
    }

    public String getSystemWarehouseName()
    {
        return systemWarehouseName;
    }

    public void setSystemWarehouseName(String systemWarehouseName)
    {
        this.systemWarehouseName = systemWarehouseName;
    }
}
