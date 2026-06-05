package com.ruoyi.product.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品分类， backed by product_category.
 */
public class ProductCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private Long parentId;
    private String ancestors;
    private String categoryCode;
    private String categoryName;
    private Integer categoryLevel;
    private String publishEnabled;
    private Integer sortOrder;
    private Integer schemaVersion;
    private String status;
    private String delFlag;
    private Integer childrenCount;
    private String keyword;
    private String fullPath;
    private Boolean leafOnly;

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

    public String getAncestors()
    {
        return ancestors;
    }

    public void setAncestors(String ancestors)
    {
        this.ancestors = ancestors;
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

    public Integer getChildrenCount()
    {
        return childrenCount;
    }

    public void setChildrenCount(Integer childrenCount)
    {
        this.childrenCount = childrenCount;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public String getFullPath()
    {
        return fullPath;
    }

    public void setFullPath(String fullPath)
    {
        this.fullPath = fullPath;
    }

    public Boolean getLeafOnly()
    {
        return leafOnly;
    }

    public void setLeafOnly(Boolean leafOnly)
    {
        this.leafOnly = leafOnly;
    }
}
