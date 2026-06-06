package com.ruoyi.integration.domain.response;

import java.util.List;
import com.ruoyi.integration.domain.SourceProductItem;

/**
 * 来源 SKU 组详情。
 */
public class SourceProductGroupDetail
{
    private SourceProductItem group;

    private List<SourceProductItem> dimensionGroups;

    private List<SourceProductItem> warehouses;

    public SourceProductItem getGroup()
    {
        return group;
    }

    public void setGroup(SourceProductItem group)
    {
        this.group = group;
    }

    public List<SourceProductItem> getDimensionGroups()
    {
        return dimensionGroups;
    }

    public void setDimensionGroups(List<SourceProductItem> dimensionGroups)
    {
        this.dimensionGroups = dimensionGroups;
    }

    public List<SourceProductItem> getWarehouses()
    {
        return warehouses;
    }

    public void setWarehouses(List<SourceProductItem> warehouses)
    {
        this.warehouses = warehouses;
    }
}
