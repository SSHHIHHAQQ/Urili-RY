package com.ruoyi.product.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品属性选项， backed by product_attribute_option.
 */
public class ProductAttributeOption extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long optionId;
    private Long attributeId;
    private String optionCode;
    private String optionLabel;
    private Integer sortOrder;
    private String defaultFlag;
    private String status;

    public Long getOptionId()
    {
        return optionId;
    }

    public void setOptionId(Long optionId)
    {
        this.optionId = optionId;
    }

    public Long getAttributeId()
    {
        return attributeId;
    }

    public void setAttributeId(Long attributeId)
    {
        this.attributeId = attributeId;
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
}
