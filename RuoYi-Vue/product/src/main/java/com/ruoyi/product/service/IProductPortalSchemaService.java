package com.ruoyi.product.service;

import java.util.List;
import com.ruoyi.product.domain.PortalProductCategory;
import com.ruoyi.product.domain.PortalProductCategorySchemaItem;

/**
 * Read-only product schema service for seller/buyer portals.
 */
public interface IProductPortalSchemaService
{
    public List<PortalProductCategory> selectPortalCategories();

    public List<PortalProductCategorySchemaItem> selectPortalSchema(Long categoryId);
}
