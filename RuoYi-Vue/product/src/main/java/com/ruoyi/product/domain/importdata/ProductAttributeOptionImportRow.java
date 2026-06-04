package com.ruoyi.product.domain.importdata;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;

/**
 * 商品属性选项导入行。
 */
public class ProductAttributeOptionImportRow
{
    @Excel(name = "属性编码", prompt = "必填，属性必须已存在，且选项来源为 ATTRIBUTE_OPTION")
    private String attributeCode;

    @Excel(name = "选项编码", prompt = "必填，同一个属性下唯一，例如 yes")
    private String optionCode;

    @Excel(name = "选项名称", prompt = "必填，例如 可以")
    private String optionLabel;

    @Excel(name = "排序", cellType = ColumnType.NUMERIC, defaultValue = "0")
    private Integer sortOrder;

    @Excel(name = "默认项", readConverterExp = "Y=是,N=否", combo = { "是", "否" }, defaultValue = "否")
    private String defaultFlag;

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

    public String getOptionCode()
    {
        return optionCode;
    }

    public void setOptionCode(String optionCode)
    {
        this.optionCode = optionCode;
    }

    public String getOptionLabel()
    {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel)
    {
        this.optionLabel = optionLabel;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getDefaultFlag()
    {
        return defaultFlag;
    }

    public void setDefaultFlag(String defaultFlag)
    {
        this.defaultFlag = defaultFlag;
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
