package com.ruoyi.product.domain;

import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品属性库， backed by product_attribute.
 */
public class ProductAttribute extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long attributeId;
    private String attributeCode;
    private String attributeName;
    private String attributeType;
    private String optionSource;
    private String dictType;
    private String unit;
    private Integer valuePrecision;
    private String status;
    private String delFlag;
    private String keyword;
    private List<ProductAttributeOption> options;

    public Long getAttributeId()
    {
        return attributeId;
    }

    public void setAttributeId(Long attributeId)
    {
        this.attributeId = attributeId;
    }

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

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public List<ProductAttributeOption> getOptions()
    {
        return options;
    }

    public void setOptions(List<ProductAttributeOption> options)
    {
        this.options = options;
    }
}
