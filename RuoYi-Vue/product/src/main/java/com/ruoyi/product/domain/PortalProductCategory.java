package com.ruoyi.product.domain;

/**
 * Product category exposed to seller/buyer portals.
 */
public class PortalProductCategory
{
    private Long categoryId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer categoryLevel;
    private String publishEnabled;
    private Integer sortOrder;
    private Integer schemaVersion;
    private Integer childrenCount;

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

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

    public Integer getCategoryLevel()
    {
        return categoryLevel;
    }

    public void setCategoryLevel(Integer categoryLevel)
    {
        this.categoryLevel = categoryLevel;
    }

    public String getPublishEnabled()
    {
        return publishEnabled;
    }

    public void setPublishEnabled(String publishEnabled)
    {
        this.publishEnabled = publishEnabled;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSchemaVersion()
    {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion)
    {
        this.schemaVersion = schemaVersion;
    }

    public Integer getChildrenCount()
    {
        return childrenCount;
    }

    public void setChildrenCount(Integer childrenCount)
    {
        this.childrenCount = childrenCount;
    }

}
