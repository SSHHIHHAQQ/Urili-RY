package com.ruoyi.product.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.PortalProductAttributeOption;
import com.ruoyi.product.domain.PortalProductCategory;
import com.ruoyi.product.domain.PortalProductCategorySchemaItem;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;
import com.ruoyi.product.service.IProductConfigService;
import com.ruoyi.product.service.IProductPortalSchemaService;

/**
 * Shared seller/buyer portal product schema queries.
 */
@Service
public class ProductPortalSchemaServiceImpl implements IProductPortalSchemaService
{
    private static final String STATUS_NORMAL = "0";

    private static final String YES = "Y";

    @Autowired
    private IProductConfigService productConfigService;

    @Override
    public List<PortalProductCategory> selectPortalCategories()
    {
        ProductCategory query = new ProductCategory();
        query.setEffectiveStatus(STATUS_NORMAL);
        query.setPublishEnabled(YES);

        List<PortalProductCategory> result = new ArrayList<>();
        for (ProductCategory category : productConfigService.selectCategoryList(query))
        {
            result.add(toPortalCategory(category));
        }
        return result;
    }

    @Override
    public List<PortalProductCategorySchemaItem> selectPortalSchema(Long categoryId)
    {
        ProductCategory category = productConfigService.selectCategoryById(categoryId);
        if (category == null)
        {
            throw new ServiceException("Product category does not exist");
        }
        if (!STATUS_NORMAL.equals(category.getEffectiveStatus()))
        {
            throw new ServiceException("Product category is disabled");
        }
        if (!YES.equals(category.getPublishEnabled()))
        {
            throw new ServiceException("Product category is not publishable");
        }

        List<PortalProductCategorySchemaItem> items = new ArrayList<>();
        for (ProductCategoryAttribute attribute : productConfigService.previewCategorySchema(categoryId))
        {
            if (STATUS_NORMAL.equals(attribute.getStatus()) && YES.equals(attribute.getVisibleFlag()))
            {
                items.add(toPortalSchemaItem(attribute));
            }
        }
        return items;
    }

    private PortalProductCategory toPortalCategory(ProductCategory category)
    {
        PortalProductCategory result = new PortalProductCategory();
        result.setCategoryId(category.getCategoryId());
        result.setParentId(category.getParentId());
        result.setCategoryCode(category.getCategoryCode());
        result.setCategoryName(category.getCategoryName());
        result.setCategoryLevel(category.getCategoryLevel());
        result.setPublishEnabled(category.getPublishEnabled());
        result.setSortOrder(category.getSortOrder());
        result.setSchemaVersion(category.getSchemaVersion());
        result.setChildrenCount(category.getChildrenCount());
        return result;
    }

    private PortalProductCategorySchemaItem toPortalSchemaItem(ProductCategoryAttribute attribute)
    {
        PortalProductCategorySchemaItem item = new PortalProductCategorySchemaItem();
        item.setCategoryId(attribute.getCategoryId());
        item.setSourceCategoryName(attribute.getSourceCategoryName());
        item.setAttributeId(attribute.getAttributeId());
        item.setAttributeCode(attribute.getAttributeCode());
        item.setAttributeName(attribute.getAttributeName());
        item.setAttributeType(attribute.getAttributeType());
        item.setOptionSource(attribute.getOptionSource());
        item.setDictType(attribute.getDictType());
        item.setUnit(attribute.getUnit());
        item.setRuleMode(attribute.getRuleMode());
        item.setRequiredFlag(attribute.getRequiredFlag());
        item.setVisibleFlag(attribute.getVisibleFlag());
        item.setEditableFlag(attribute.getEditableFlag());
        item.setFilterableFlag(attribute.getFilterableFlag());
        item.setGroupCode(attribute.getGroupCode());
        item.setSortOrder(attribute.getSortOrder());
        item.setPlaceholder(attribute.getPlaceholder());
        item.setHelpText(attribute.getHelpText());
        item.setValidationRule(attribute.getValidationRule());
        item.setStatus(attribute.getStatus());
        item.setOptions(toPortalOptions(attribute.getOptions()));
        return item;
    }

    private List<PortalProductAttributeOption> toPortalOptions(List<ProductAttributeOption> options)
    {
        List<PortalProductAttributeOption> result = new ArrayList<>();
        if (options == null)
        {
            return result;
        }
        for (ProductAttributeOption option : options)
        {
            if (STATUS_NORMAL.equals(option.getStatus()))
            {
                result.add(toPortalOption(option));
            }
        }
        return result;
    }

    private PortalProductAttributeOption toPortalOption(ProductAttributeOption option)
    {
        PortalProductAttributeOption result = new PortalProductAttributeOption();
        result.setOptionCode(option.getOptionCode());
        result.setOptionLabel(option.getOptionLabel());
        result.setSortOrder(option.getSortOrder());
        result.setDefaultFlag(option.getDefaultFlag());
        result.setStatus(option.getStatus());
        return result;
    }
}
