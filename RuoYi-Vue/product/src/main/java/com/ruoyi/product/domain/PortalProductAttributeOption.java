package com.ruoyi.product.domain;

import java.io.Serializable;

/**
 * Product attribute option exposed to seller/buyer portals.
 */
public class PortalProductAttributeOption implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String optionCode;
    private String optionLabel;
    private Integer sortOrder;
    private String defaultFlag;
    private String status;

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
