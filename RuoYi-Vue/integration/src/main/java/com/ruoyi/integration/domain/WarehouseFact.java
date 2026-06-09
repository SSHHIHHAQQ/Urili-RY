package com.ruoyi.integration.domain;

/**
 * System warehouse facts exposed to integration without depending on warehouse internals.
 */
public class WarehouseFact
{
    private String warehouseCode;
    private String warehouseName;

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
}
