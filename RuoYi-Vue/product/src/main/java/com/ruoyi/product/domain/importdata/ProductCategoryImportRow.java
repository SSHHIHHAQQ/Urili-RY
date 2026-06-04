package com.ruoyi.product.domain.importdata;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;

/**
 * 商品分类导入行。
 */
public class ProductCategoryImportRow
{
    @Excel(name = "分类编码", prompt = "必填，保存为小写 code，例如 apparel")
    private String categoryCode;

    @Excel(name = "分类名称", prompt = "必填，例如 服装")
    private String categoryName;

    @Excel(name = "父级分类编码", prompt = "顶级分类留空；父级必须已存在或位于本文件前面的行")
    private String parentCategoryCode;

    @Excel(name = "排序", cellType = ColumnType.NUMERIC, defaultValue = "0")
    private Integer sortOrder;

    @Excel(name = "状态", readConverterExp = "0=正常,1=停用", combo = { "正常", "停用" }, defaultValue = "正常")
    private String status;

    @Excel(name = "备注")
    private String remark;

    public String getCategoryCode()
    {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode)
    {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getParentCategoryCode()
    {
        return parentCategoryCode;
    }

    public void setParentCategoryCode(String parentCategoryCode)
    {
        this.parentCategoryCode = parentCategoryCode;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
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
