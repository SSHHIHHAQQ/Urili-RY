package com.ruoyi.product.domain.importdata;

import com.ruoyi.common.annotation.Excel;

/**
 * 商品属性类型与选项来源规则说明行。
 */
public class ProductAttributeTypeSourceRuleRow
{
    @Excel(name = "属性类型", width = 18)
    private String attributeType;

    @Excel(name = "类型说明", width = 22)
    private String typeDescription;

    @Excel(name = "允许选项来源", width = 28)
    private String allowedOptionSources;

    @Excel(name = "选项来源填写规则", width = 44)
    private String optionSourceRule;

    @Excel(name = "字典类型规则", width = 36)
    private String dictTypeRule;

    @Excel(name = "单位规则", width = 30)
    private String unitRule;

    @Excel(name = "小数位数规则", width = 34)
    private String precisionRule;

    @Excel(name = "选项值维护方式", width = 38)
    private String optionValueRule;

    @Excel(name = "正确示例", width = 36)
    private String example;

    public String getAttributeType()
    {
        return attributeType;
    }

    public void setAttributeType(String attributeType)
    {
        this.attributeType = attributeType;
    }

    public String getTypeDescription()
    {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription)
    {
        this.typeDescription = typeDescription;
    }

    public String getAllowedOptionSources()
    {
        return allowedOptionSources;
    }

    public void setAllowedOptionSources(String allowedOptionSources)
    {
        this.allowedOptionSources = allowedOptionSources;
    }

    public String getOptionSourceRule()
    {
        return optionSourceRule;
    }

    public void setOptionSourceRule(String optionSourceRule)
    {
        this.optionSourceRule = optionSourceRule;
    }

    public String getDictTypeRule()
    {
        return dictTypeRule;
    }

    public void setDictTypeRule(String dictTypeRule)
    {
        this.dictTypeRule = dictTypeRule;
    }

    public String getUnitRule()
    {
        return unitRule;
    }

    public void setUnitRule(String unitRule)
    {
        this.unitRule = unitRule;
    }

    public String getPrecisionRule()
    {
        return precisionRule;
    }

    public void setPrecisionRule(String precisionRule)
    {
        this.precisionRule = precisionRule;
    }

    public String getOptionValueRule()
    {
        return optionValueRule;
    }

    public void setOptionValueRule(String optionValueRule)
    {
        this.optionValueRule = optionValueRule;
    }

    public String getExample()
    {
        return example;
    }

    public void setExample(String example)
    {
        this.example = example;
    }
}
