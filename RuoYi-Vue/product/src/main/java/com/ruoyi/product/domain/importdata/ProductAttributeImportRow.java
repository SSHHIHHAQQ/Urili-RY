package com.ruoyi.product.domain.importdata;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;

/**
 * 商品属性导入行。
 */
public class ProductAttributeImportRow
{
    @Excel(name = "属性编码", prompt = "必填，保存为小写 code，例如 washable")
    private String attributeCode;

    @Excel(name = "属性名称", prompt = "必填，例如 是否可水洗")
    private String attributeName;

    @Excel(name = "属性类型", combo = { "TEXT", "NUMBER", "BOOLEAN", "SINGLE_SELECT", "MULTI_SELECT", "DATE" },
        prompt = "必填，填写 TEXT / NUMBER / BOOLEAN / SINGLE_SELECT / MULTI_SELECT / DATE")
    private String attributeType;

    @Excel(name = "选项来源", combo = { "NONE", "ATTRIBUTE_OPTION", "SYS_DICT" },
        prompt = "必填，填写 NONE / ATTRIBUTE_OPTION / SYS_DICT")
    private String optionSource;

    @Excel(name = "字典类型", prompt = "选项来源为 SYS_DICT 时必填")
    private String dictType;

    @Excel(name = "单位")
    private String unit;

    @Excel(name = "数值精度", cellType = ColumnType.NUMERIC, defaultValue = "0")
    private Integer valuePrecision;

    @Excel(name = "状态", readConverterExp = "0=正常,1=停用", combo = { "正常", "停用" }, defaultValue = "正常")
    private String status;

    @Excel(name = "备注")
    private String remark;

    public String getAttributeCode()
    {
        return attributeCode;
    }

    public void setAttributeCode(String attributeCode)
    {
        this.attributeCode = attributeCode;
    }

    public String getAttributeName()
    {
        return attributeName;
    }

    public void setAttributeName(String attributeName)
    {
        this.attributeName = attributeName;
    }

    public String getAttributeType()
    {
        return attributeType;
    }

    public void setAttributeType(String attributeType)
    {
        this.attributeType = attributeType;
    }

    public String getOptionSource()
    {
        return optionSource;
    }

    public void setOptionSource(String optionSource)
    {
        this.optionSource = optionSource;
    }

    public String getDictType()
    {
        return dictType;
    }

    public void setDictType(String dictType)
    {
        this.dictType = dictType;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public Integer getValuePrecision()
    {
        return valuePrecision;
    }

    public void setValuePrecision(Integer valuePrecision)
    {
        this.valuePrecision = valuePrecision;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
