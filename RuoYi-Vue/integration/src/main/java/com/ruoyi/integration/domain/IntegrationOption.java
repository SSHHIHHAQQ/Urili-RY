package com.ruoyi.integration.domain;

/**
 * integration 模块通用下拉选项。
 */
public class IntegrationOption
{
    private String label;
    private String value;
    private String code;
    private String name;
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
