package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.PortalProductAttributeOption;
import com.ruoyi.product.domain.PortalProductCategory;
import com.ruoyi.product.domain.PortalProductCategorySchemaItem;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;
import com.ruoyi.product.service.IProductConfigService;

public class ProductPortalSchemaServiceImplTest
{
    @Test
    public void selectPortalSchemaRejectsMissingCategoryWithControlledException()
    {
        ProductPortalSchemaServiceImpl service = new ProductPortalSchemaServiceImpl();
        inject(service, "productConfigService", productConfigServiceReturningMissingCategory());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.selectPortalSchema(99L));

        assertEquals("Product category does not exist", exception.getMessage());
    }

    @Test
    public void selectPortalCategoriesUsesPublishedFilterAndPortalDtoWhitelist()
    {
        ProductPortalSchemaServiceImpl service = new ProductPortalSchemaServiceImpl();
        ProductCategory[] capturedQuery = new ProductCategory[1];
        ProductCategory category = new ProductCategory();
        category.setCategoryId(11L);
        category.setParentId(0L);
        category.setCategoryCode("CAT-A");
        category.setCategoryName("Category A");
        category.setCategoryLevel(1);
        category.setPublishEnabled("Y");
        category.setSortOrder(10);
        category.setSchemaVersion(3);
        category.setChildrenCount(2);
        inject(service, "productConfigService", productConfigServiceForCategories(capturedQuery, category));

        List<PortalProductCategory> categories = service.selectPortalCategories();

        assertEquals("0", capturedQuery[0].getEffectiveStatus());
        assertEquals("Y", capturedQuery[0].getPublishEnabled());
        assertEquals(1, categories.size());
        assertEquals(Long.valueOf(11L), categories.get(0).getCategoryId());
        assertPortalDtoWhitelist(PortalProductCategory.class, "categoryId", "parentId", "categoryCode",
                "categoryName", "categoryLevel", "publishEnabled", "sortOrder", "schemaVersion", "childrenCount");
    }

    @Test
    public void selectPortalSchemaFiltersHiddenRowsAndKeepsPortalDtoWhitelist()
    {
        ProductPortalSchemaServiceImpl service = new ProductPortalSchemaServiceImpl();
        ProductCategory category = new ProductCategory();
        category.setCategoryId(11L);
        category.setEffectiveStatus("0");
        category.setPublishEnabled("Y");
        inject(service, "productConfigService", productConfigServiceForSchema(category,
                visibleAttribute(101L, "color", "Color", activeOption("red"), disabledOption("blue")),
                hiddenAttribute(102L),
                disabledAttribute(103L)));

        List<PortalProductCategorySchemaItem> items = service.selectPortalSchema(11L);

        assertEquals(1, items.size());
        assertEquals(Long.valueOf(101L), items.get(0).getAttributeId());
        assertEquals("color", items.get(0).getAttributeCode());
        assertEquals(1, items.get(0).getOptions().size());
        assertEquals("red", items.get(0).getOptions().get(0).getOptionCode());
        assertPortalDtoWhitelist(PortalProductCategorySchemaItem.class, "serialVersionUID", "categoryId",
                "sourceCategoryName", "attributeId", "attributeCode", "attributeName", "attributeType",
                "optionSource", "dictType", "unit", "ruleMode", "requiredFlag", "visibleFlag", "editableFlag",
                "filterableFlag", "groupCode", "sortOrder", "placeholder", "helpText", "validationRule", "status",
                "options");
        assertPortalDtoWhitelist(PortalProductAttributeOption.class, "serialVersionUID", "optionCode",
                "optionLabel", "sortOrder", "defaultFlag", "status");
    }

    private IProductConfigService productConfigServiceReturningMissingCategory()
    {
        return (IProductConfigService) Proxy.newProxyInstance(IProductConfigService.class.getClassLoader(),
                new Class<?>[] { IProductConfigService.class }, (proxy, method, args) -> {
                    if ("selectCategoryById".equals(method.getName()))
                    {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private IProductConfigService productConfigServiceForCategories(ProductCategory[] capturedQuery,
            ProductCategory category)
    {
        return (IProductConfigService) Proxy.newProxyInstance(IProductConfigService.class.getClassLoader(),
                new Class<?>[] { IProductConfigService.class }, (proxy, method, args) -> {
                    if ("selectCategoryList".equals(method.getName()))
                    {
                        capturedQuery[0] = (ProductCategory) args[0];
                        return Collections.singletonList(category);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private IProductConfigService productConfigServiceForSchema(ProductCategory category,
            ProductCategoryAttribute... attributes)
    {
        return (IProductConfigService) Proxy.newProxyInstance(IProductConfigService.class.getClassLoader(),
                new Class<?>[] { IProductConfigService.class }, (proxy, method, args) -> {
                    if ("selectCategoryById".equals(method.getName()))
                    {
                        return category;
                    }
                    if ("previewCategorySchema".equals(method.getName()))
                    {
                        return Arrays.asList(attributes);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private ProductCategoryAttribute visibleAttribute(Long attributeId, String code, String name,
            ProductAttributeOption... options)
    {
        ProductCategoryAttribute attribute = new ProductCategoryAttribute();
        attribute.setCategoryId(11L);
        attribute.setSourceCategoryName("Category A");
        attribute.setAttributeId(attributeId);
        attribute.setAttributeCode(code);
        attribute.setAttributeName(name);
        attribute.setAttributeType("select");
        attribute.setOptionSource("manual");
        attribute.setDictType("");
        attribute.setUnit("");
        attribute.setRuleMode("optional");
        attribute.setRequiredFlag("N");
        attribute.setVisibleFlag("Y");
        attribute.setEditableFlag("Y");
        attribute.setFilterableFlag("Y");
        attribute.setGroupCode("basic");
        attribute.setSortOrder(1);
        attribute.setPlaceholder("select");
        attribute.setHelpText("help");
        attribute.setValidationRule("{}");
        attribute.setStatus("0");
        attribute.setOptions(Arrays.asList(options));
        return attribute;
    }

    private ProductCategoryAttribute hiddenAttribute(Long attributeId)
    {
        ProductCategoryAttribute attribute = visibleAttribute(attributeId, "hidden", "Hidden");
        attribute.setVisibleFlag("N");
        return attribute;
    }

    private ProductCategoryAttribute disabledAttribute(Long attributeId)
    {
        ProductCategoryAttribute attribute = visibleAttribute(attributeId, "disabled", "Disabled");
        attribute.setStatus("1");
        return attribute;
    }

    private ProductAttributeOption activeOption(String code)
    {
        ProductAttributeOption option = new ProductAttributeOption();
        option.setOptionCode(code);
        option.setOptionLabel(code);
        option.setSortOrder(1);
        option.setDefaultFlag("N");
        option.setStatus("0");
        return option;
    }

    private ProductAttributeOption disabledOption(String code)
    {
        ProductAttributeOption option = activeOption(code);
        option.setStatus("1");
        return option;
    }

    private void assertPortalDtoWhitelist(Class<?> dtoType, String... expectedFields)
    {
        Set<String> actualFields = new TreeSet<>();
        for (Field field : dtoType.getDeclaredFields())
        {
            if (!Modifier.isStatic(field.getModifiers()))
            {
                actualFields.add(field.getName());
            }
            else if ("serialVersionUID".equals(field.getName()))
            {
                actualFields.add(field.getName());
            }
        }

        assertEquals(new TreeSet<>(Arrays.asList(expectedFields)), actualFields);
        for (String forbidden : new String[] {
                "terminal", "subjectId", "sellerId", "buyerId", "accountId", "sellerAccountId", "buyerAccountId",
                "directLoginTicketId", "actingAdminId", "actingAdminName", "operParam", "jsonResult", "tokenId"
        })
        {
            assertFalse(dtoType.getSimpleName() + " must not expose " + forbidden, actualFields.contains(forbidden));
        }
        assertTrue(dtoType.getSimpleName() + " whitelist must not be empty", !actualFields.isEmpty());
    }

    private void inject(Object target, String fieldName, Object value)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError("Unable to inject " + fieldName, e);
        }
    }

    private Object defaultValue(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (returnType == boolean.class)
        {
            return false;
        }
        if (returnType == int.class)
        {
            return 0;
        }
        if (returnType == long.class)
        {
            return 0L;
        }
        return null;
    }
}
