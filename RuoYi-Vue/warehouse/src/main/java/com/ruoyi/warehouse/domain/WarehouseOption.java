package com.ruoyi.warehouse.domain;

/**
 * 通用下拉选项。
 */
public class WarehouseOption
{
    private String label;
    private Object value;
    private String code;
    private String name;
    private String searchText;

    public WarehouseOption()
    {
    }

    public WarehouseOption(String label, Object value)
    {
        this.label = label;
        this.value = value;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object value)
    {
        this.value = value;
    }

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

    public String getSearchText()
    {
        return searchText;
    }

    public void setSearchText(String searchText)
    {
        this.searchText = searchText;
    }
}
