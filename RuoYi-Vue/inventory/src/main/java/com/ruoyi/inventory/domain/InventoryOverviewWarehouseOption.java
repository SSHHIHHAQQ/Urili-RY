package com.ruoyi.inventory.domain;

/**
 * 库存总览仓库筛选选项。
 */
public class InventoryOverviewWarehouseOption
{
    private String label;

    private String value;

    private String warehouseKind;

    private String warehouseRefType;

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private String searchText;

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getWarehouseKind()
    {
        return warehouseKind;
    }

    public void setWarehouseKind(String warehouseKind)
    {
        this.warehouseKind = warehouseKind;
    }

    public String getWarehouseRefType()
    {
        return warehouseRefType;
    }

    public void setWarehouseRefType(String warehouseRefType)
    {
        this.warehouseRefType = warehouseRefType;
    }

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
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

    public String getSearchText()
    {
        return searchText;
    }

    public void setSearchText(String searchText)
    {
        this.searchText = searchText;
    }
}
