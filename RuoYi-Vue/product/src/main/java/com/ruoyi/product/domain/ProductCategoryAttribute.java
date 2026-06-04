package com.ruoyi.product.domain;

import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 类目属性配置， backed by product_category_attribute.
 */
public class ProductCategoryAttribute extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long categoryAttributeId;
    private Long categoryId;
    private Long attributeId;
    private String ruleMode;
    private String requiredFlag;
    private String visibleFlag;
    private String editableFlag;
    private String filterableFlag;
    private String groupCode;
    private Integer sortOrder;
    private String placeholder;
    private String helpText;
    private String validationRule;
    private String status;
    private String categoryName;
    private String sourceCategoryName;
    private String attributeCode;
    private String attributeName;
    private String attributeType;
    private String optionSource;
    private String dictType;
    private String unit;
    private List<ProductAttributeOption> options;

    public Long getCategoryAttributeId()
    {
        return categoryAttributeId;
    }

    public void setCategoryAttributeId(Long categoryAttributeId)
    {
        this.categoryAttributeId = categoryAttributeId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getAttributeId()
    {
        return attributeId;
    }

    public void setAttributeId(Long attributeId)
    {
        this.attributeId = attributeId;
    }

    public String getRuleMode()
    {
        return ruleMode;
    }

    public void setRuleMode(String ruleMode)
    {
        this.ruleMode = ruleMode;
    }

    public String getRequiredFlag()
    {
        return requiredFlag;
    }

    public void setRequiredFlag(String requiredFlag)
    {
        this.requiredFlag = requiredFlag;
    }

    public String getVisibleFlag()
    {
        return visibleFlag;
    }

    public void setVisibleFlag(String visibleFlag)
    {
        this.visibleFlag = visibleFlag;
    }

    public String getEditableFlag()
    {
        return editableFlag;
    }

    public void setEditableFlag(String editableFlag)
    {
        this.editableFlag = editableFlag;
    }

    public String getFilterableFlag()
    {
        return filterableFlag;
    }

    public void setFilterableFlag(String filterableFlag)
    {
        this.filterableFlag = filterableFlag;
    }

    public String getGroupCode()
    {
        return groupCode;
    }

    public void setGroupCode(String groupCode)
    {
        this.groupCode = groupCode;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getPlaceholder()
    {
        return placeholder;
    }

    public void setPlaceholder(String placeholder)
    {
        this.placeholder = placeholder;
    }

    public String getHelpText()
    {
        return helpText;
    }

    public void setHelpText(String helpText)
    {
        this.helpText = helpText;
    }

    public String getValidationRule()
    {
        return validationRule;
    }

    public void setValidationRule(String validationRule)
    {
        this.validationRule = validationRule;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getSourceCategoryName()
    {
        return sourceCategoryName;
    }

    public void setSourceCategoryName(String sourceCategoryName)
    {
        this.sourceCategoryName = sourceCategoryName;
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

    public List<ProductAttributeOption> getOptions()
    {
        return options;
    }

    public void setOptions(List<ProductAttributeOption> options)
    {
        this.options = options;
    }
}
