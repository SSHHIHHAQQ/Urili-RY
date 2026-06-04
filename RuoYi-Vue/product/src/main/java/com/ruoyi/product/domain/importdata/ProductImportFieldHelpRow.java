package com.ruoyi.product.domain.importdata;

import com.ruoyi.common.annotation.Excel;

/**
 * 商品配置导入模板字段说明行。
 */
public class ProductImportFieldHelpRow
{
    @Excel(name = "模板", width = 18)
    private String templateName;

    @Excel(name = "字段", width = 18)
    private String fieldName;

    @Excel(name = "是否必填", width = 12)
    private String required;

    @Excel(name = "填写规则", width = 38)
    private String rule;

    @Excel(name = "示例", width = 24)
    private String example;

    @Excel(name = "常见错误", width = 38)
    private String commonMistake;

    public String getTemplateName()
    {
        return templateName;
    }

    public void setTemplateName(String templateName)
    {
        this.templateName = templateName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getRequired()
    {
        return required;
    }

    public void setRequired(String required)
    {
        this.required = required;
    }

    public String getRule()
    {
        return rule;
    }

    public void setRule(String rule)
    {
        this.rule = rule;
    }

    public String getExample()
    {
        return example;
    }

    public void setExample(String example)
    {
        this.example = example;
    }

    public String getCommonMistake()
    {
        return commonMistake;
    }

    public void setCommonMistake(String commonMistake)
    {
        this.commonMistake = commonMistake;
    }
}
