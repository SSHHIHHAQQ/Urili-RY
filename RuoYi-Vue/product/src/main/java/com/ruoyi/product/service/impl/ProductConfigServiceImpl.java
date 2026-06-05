package com.ruoyi.product.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.product.domain.ProductAttribute;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;
import com.ruoyi.product.mapper.ProductConfigMapper;
import com.ruoyi.product.service.IProductConfigService;

/**
 * 商品分类与属性配置服务实现。
 */
@Service
public class ProductConfigServiceImpl implements IProductConfigService
{
    private static final long ROOT_PARENT_ID = 0L;

    private static final String ROOT_ANCESTORS = "0";

    private static final String STATUS_NORMAL = "0";

    private static final String YES = "Y";

    private static final String NO = "N";

    private static final String OPTION_SOURCE_NONE = "NONE";

    private static final String OPTION_SOURCE_ATTRIBUTE_OPTION = "ATTRIBUTE_OPTION";

    private static final String OPTION_SOURCE_SYS_DICT = "SYS_DICT";

    private static final Set<String> ATTRIBUTE_TYPES = Set.of("TEXT", "NUMBER", "BOOLEAN", "SINGLE_SELECT",
        "MULTI_SELECT", "DATE");

    private static final Set<String> OPTION_ATTRIBUTE_TYPES = Set.of("SINGLE_SELECT", "MULTI_SELECT");

    private static final String ATTRIBUTE_TYPE_NUMBER = "NUMBER";

    private static final String RULE_ADD = "ADD";

    private static final String RULE_OVERRIDE = "OVERRIDE";

    private static final String RULE_DISABLE = "DISABLE";

    private static final int CACHE_TTL_MINUTES = 60;

    @Autowired
    private RedisCache redisCache;
    @Autowired
    private ProductConfigMapper productConfigMapper;

    @Override
    public List<ProductCategory> selectCategoryList(ProductCategory query)
    {
        return productConfigMapper.selectCategoryList(query);
    }

    @Override
    public List<ProductCategory> selectCategoryPath(Long categoryId)
    {
        ProductCategory category = selectCategoryById(categoryId);
        List<ProductCategory> path = new ArrayList<>();
        String[] ancestorIds = StringUtils.split(category.getAncestors(), ",");
        if (ancestorIds != null)
        {
            for (String ancestorId : ancestorIds)
            {
                if (!String.valueOf(ROOT_PARENT_ID).equals(ancestorId))
                {
                    path.add(selectCategoryById(Long.valueOf(ancestorId)));
                }
            }
        }
        path.add(category);
        return path;
    }

    @Override
    public ProductCategory selectCategoryById(Long categoryId)
    {
        ProductCategory category = productConfigMapper.selectCategoryById(categoryId);
        if (category == null)
        {
            throw new ServiceException("商品分类不存在");
        }
        return category;
    }

    @Override
    @Transactional
    public int insertCategory(ProductCategory category)
    {
        normalizeCategory(category);
        ProductCategory parent = resolveParent(category.getParentId());
        fillCategoryTreeFields(category, parent);
        if (productConfigMapper.selectCategoryByCode(category.getCategoryCode()) != null)
        {
            throw new ServiceException("商品分类编码已存在");
        }
        if (productConfigMapper.countCategoryNameByParent(category.getParentId(), category.getCategoryName(), null) > 0)
        {
            throw new ServiceException("同级商品分类名称已存在");
        }
        category.setSchemaVersion(1);
        category.setPublishEnabled(YES);
        String username = currentUsername();
        category.setCreateBy(username);
        int rows = productConfigMapper.insertCategory(category);
        if (parent != null)
        {
            productConfigMapper.updateCategoryPublishEnabled(parent.getCategoryId(), NO, username);
        }
        return rows;
    }

    @Override
    @Transactional
    public int updateCategory(ProductCategory category)
    {
        ProductCategory current = selectCategoryById(category.getCategoryId());
        normalizeCategory(category);
        if (!current.getParentId().equals(category.getParentId()))
        {
            throw new ServiceException("第一版不支持移动商品分类，请新建分类并迁移后再调整");
        }
        if (!current.getCategoryCode().equals(category.getCategoryCode())
            && productConfigMapper.selectCategoryByCode(category.getCategoryCode()) != null)
        {
            throw new ServiceException("商品分类编码已存在");
        }
        if (productConfigMapper.countCategoryNameByParent(category.getParentId(), category.getCategoryName(), category.getCategoryId()) > 0)
        {
            throw new ServiceException("同级商品分类名称已存在");
        }
        category.setAncestors(current.getAncestors());
        category.setCategoryLevel(current.getCategoryLevel());
        category.setSchemaVersion(current.getSchemaVersion());
        category.setPublishEnabled(derivedPublishEnabled(category.getCategoryId()));
        category.setUpdateBy(currentUsername());
        int rows = productConfigMapper.updateCategory(category);
        return rows;
    }

    @Override
    @Transactional
    public int deleteCategoryById(Long categoryId)
    {
        ProductCategory category = selectCategoryById(categoryId);
        if (productConfigMapper.countChildCategories(categoryId) > 0)
        {
            throw new ServiceException("存在下级商品分类，不允许删除");
        }
        if (productConfigMapper.countCategoryAttributes(categoryId) > 0)
        {
            throw new ServiceException("该分类已配置属性，请先移除类目属性配置");
        }
        String username = currentUsername();
        invalidateSchemaByCategoryId(categoryId);
        int rows = productConfigMapper.deleteCategoryById(categoryId, username);
        Long parentId = category.getParentId();
        if (parentId != null && parentId.longValue() != ROOT_PARENT_ID
            && productConfigMapper.countChildCategories(parentId) == 0)
        {
            productConfigMapper.updateCategoryPublishEnabled(parentId, YES, username);
        }
        return rows;
    }

    @Override
    public List<ProductAttribute> selectAttributeList(ProductAttribute query)
    {
        return productConfigMapper.selectAttributeList(query);
    }

    @Override
    public List<ProductAttribute> selectEnabledAttributeList(ProductAttribute query)
    {
        return productConfigMapper.selectEnabledAttributeList(query);
    }

    @Override
    public ProductAttribute selectAttributeById(Long attributeId)
    {
        ProductAttribute attribute = productConfigMapper.selectAttributeById(attributeId);
        if (attribute == null)
        {
            throw new ServiceException("商品属性不存在");
        }
        attribute.setOptions(productConfigMapper.selectOptionList(attributeId));
        return attribute;
    }

    @Override
    @Transactional
    public int insertAttribute(ProductAttribute attribute)
    {
        normalizeAttribute(attribute);
        if (productConfigMapper.selectAttributeByCode(attribute.getAttributeCode()) != null)
        {
            throw new ServiceException("商品属性编码已存在");
        }
        attribute.setCreateBy(currentUsername());
        int rows = productConfigMapper.insertAttribute(attribute);
        return rows;
    }

    @Override
    @Transactional
    public int updateAttribute(ProductAttribute attribute)
    {
        ProductAttribute current = selectAttributeById(attribute.getAttributeId());
        normalizeAttribute(attribute);
        ProductAttribute sameCode = productConfigMapper.selectAttributeByCode(attribute.getAttributeCode());
        if (sameCode != null && !sameCode.getAttributeId().equals(attribute.getAttributeId()))
        {
            throw new ServiceException("商品属性编码已存在");
        }
        if (productConfigMapper.countAttributeCategoryBindings(attribute.getAttributeId()) > 0)
        {
            if (!current.getAttributeCode().equals(attribute.getAttributeCode())
                || !current.getAttributeType().equals(attribute.getAttributeType()))
            {
                throw new ServiceException("属性已被类目引用，不允许修改编码或类型");
            }
        }
        attribute.setUpdateBy(currentUsername());
        invalidateSchemaByAttributeId(attribute.getAttributeId());
        int rows = productConfigMapper.updateAttribute(attribute);
        return rows;
    }

    @Override
    @Transactional
    public int deleteAttributeById(Long attributeId)
    {
        selectAttributeById(attributeId);
        if (productConfigMapper.countAttributeCategoryBindings(attributeId) > 0)
        {
            throw new ServiceException("属性已被类目引用，不能删除，请停用");
        }
        invalidateSchemaByAttributeId(attributeId);
        int rows = productConfigMapper.deleteAttributeById(attributeId, currentUsername());
        return rows;
    }

    @Override
    public List<ProductAttributeOption> selectOptionList(Long attributeId)
    {
        selectAttributeById(attributeId);
        return productConfigMapper.selectOptionList(attributeId);
    }

    @Override
    @Transactional
    public int insertOption(Long attributeId, ProductAttributeOption option)
    {
        ProductAttribute attribute = selectAttributeById(attributeId);
        validateCustomOptionAttribute(attribute);
        normalizeOption(attributeId, option);
        if (productConfigMapper.countOptionCode(attributeId, option.getOptionCode(), null) > 0)
        {
            throw new ServiceException("属性选项编码已存在");
        }
        option.setCreateBy(currentUsername());
        invalidateSchemaByAttributeId(attributeId);
        int rows = productConfigMapper.insertOption(option);
        return rows;
    }

    @Override
    @Transactional
    public int updateOption(Long attributeId, Long optionId, ProductAttributeOption option)
    {
        ProductAttribute attribute = selectAttributeById(attributeId);
        validateCustomOptionAttribute(attribute);
        ProductAttributeOption current = productConfigMapper.selectOptionById(optionId);
        if (current == null || !attributeId.equals(current.getAttributeId()))
        {
            throw new ServiceException("属性选项不存在");
        }
        normalizeOption(attributeId, option);
        option.setOptionId(optionId);
        if (productConfigMapper.countOptionCode(attributeId, option.getOptionCode(), optionId) > 0)
        {
            throw new ServiceException("属性选项编码已存在");
        }
        option.setUpdateBy(currentUsername());
        invalidateSchemaByAttributeId(attributeId);
        int rows = productConfigMapper.updateOption(option);
        return rows;
    }

    @Override
    @Transactional
    public int deleteOptionById(Long attributeId, Long optionId)
    {
        ProductAttributeOption current = productConfigMapper.selectOptionById(optionId);
        if (current == null || !attributeId.equals(current.getAttributeId()))
        {
            throw new ServiceException("属性选项不存在");
        }
        invalidateSchemaByAttributeId(attributeId);
        int rows = productConfigMapper.deleteOptionById(optionId, currentUsername());
        return rows;
    }

    @Override
    public List<ProductCategoryAttribute> selectDirectCategoryAttributeList(Long categoryId)
    {
        selectCategoryById(categoryId);
        return withOptions(productConfigMapper.selectDirectCategoryAttributeList(categoryId));
    }

    @Override
    public ProductCategoryAttribute selectCategoryAttributeById(Long categoryAttributeId)
    {
        ProductCategoryAttribute categoryAttribute = productConfigMapper.selectCategoryAttributeById(categoryAttributeId);
        if (categoryAttribute == null)
        {
            throw new ServiceException("类目属性配置不存在");
        }
        return withOptions(categoryAttribute);
    }

    @Override
    @Transactional
    public int saveCategoryAttribute(ProductCategoryAttribute categoryAttribute)
    {
        ProductCategory category = selectCategoryById(categoryAttribute.getCategoryId());
        ProductAttribute attribute = selectAttributeById(categoryAttribute.getAttributeId());
        normalizeCategoryAttribute(categoryAttribute);
        validateRuleMode(category, attribute, categoryAttribute);
        ProductCategoryAttribute current = productConfigMapper.selectCategoryAttribute(categoryAttribute.getCategoryId(),
            categoryAttribute.getAttributeId());
        categoryAttribute.setUpdateBy(currentUsername());
        int rows;
        if (current == null)
        {
            categoryAttribute.setCreateBy(categoryAttribute.getUpdateBy());
            rows = productConfigMapper.insertCategoryAttribute(categoryAttribute);
        }
        else
        {
            categoryAttribute.setCategoryAttributeId(current.getCategoryAttributeId());
            rows = productConfigMapper.updateCategoryAttribute(categoryAttribute);
        }
        invalidateSchemaByCategoryId(category.getCategoryId());
        productConfigMapper.increaseCategorySchemaVersion(category.getCategoryId(), categoryAttribute.getUpdateBy());
        return rows;
    }

    @Override
    @Transactional
    public int deleteCategoryAttributeById(Long categoryAttributeId)
    {
        ProductCategoryAttribute current = selectCategoryAttributeById(categoryAttributeId);
        invalidateSchemaByCategoryId(current.getCategoryId());
        int rows = productConfigMapper.deleteCategoryAttributeById(categoryAttributeId, currentUsername());
        productConfigMapper.increaseCategorySchemaVersion(current.getCategoryId(), currentUsername());
        return rows;
    }

    @Override
    public List<ProductCategoryAttribute> previewCategorySchema(Long categoryId)
    {
        String cacheKey = CacheConstants.PRODUCT_SCHEMA + categoryId;

        List<ProductCategoryAttribute> cached = redisCache.getCacheObject(cacheKey);
        if (cached != null)
        {
            return cached;
        }

        ProductCategory category = selectCategoryById(categoryId);
        List<Long> categoryIds = categoryChain(category);
        List<ProductCategoryAttribute> rules = productConfigMapper.selectCategoryAttributeRulesByCategoryIds(categoryIds);
        rules.sort(Comparator
                .comparingInt((ProductCategoryAttribute rule) -> categoryIds.indexOf(rule.getCategoryId()))
                .thenComparing(rule -> defaultInt(rule.getSortOrder())));

        Map<Long, ProductCategoryAttribute> schema = new LinkedHashMap<>();
        for (ProductCategoryAttribute rule : rules)
        {
            if (RULE_DISABLE.equals(rule.getRuleMode()))
            {
                schema.remove(rule.getAttributeId());
                continue;
            }
            schema.put(rule.getAttributeId(), rule);
        }

        List<ProductCategoryAttribute> result = withOptions(new ArrayList<>(schema.values()));
        redisCache.setCacheObject(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }
    private void normalizeCategory(ProductCategory category)
    {
        category.setParentId(category.getParentId() == null ? ROOT_PARENT_ID : category.getParentId());
        category.setCategoryCode(normalizeCode(category.getCategoryCode(), "商品分类编码不能为空"));
        category.setCategoryName(requireTrim(category.getCategoryName(), "商品分类名称不能为空"));
        category.setStatus(StringUtils.defaultIfBlank(category.getStatus(), STATUS_NORMAL));
        category.setSortOrder(defaultInt(category.getSortOrder()));
        category.setRemark(StringUtils.defaultString(category.getRemark()));
    }

    private ProductCategory resolveParent(Long parentId)
    {
        if (parentId == null || ROOT_PARENT_ID == parentId)
        {
            return null;
        }
        return selectCategoryById(parentId);
    }

    private void fillCategoryTreeFields(ProductCategory category, ProductCategory parent)
    {
        if (parent == null)
        {
            category.setParentId(ROOT_PARENT_ID);
            category.setAncestors(ROOT_ANCESTORS);
            category.setCategoryLevel(1);
            return;
        }
        category.setAncestors(parent.getAncestors() + "," + parent.getCategoryId());
        category.setCategoryLevel(defaultInt(parent.getCategoryLevel()) + 1);
    }

    private String derivedPublishEnabled(Long categoryId)
    {
        return productConfigMapper.countChildCategories(categoryId) > 0 ? NO : YES;
    }

    private void normalizeAttribute(ProductAttribute attribute)
    {
        attribute.setAttributeCode(normalizeCode(attribute.getAttributeCode(), "商品属性编码不能为空"));
        attribute.setAttributeName(requireTrim(attribute.getAttributeName(), "商品属性名称不能为空"));
        attribute.setAttributeType(normalizeEnumCode(attribute.getAttributeType(), "商品属性类型不能为空"));
        if (!ATTRIBUTE_TYPES.contains(attribute.getAttributeType()))
        {
            throw new ServiceException("商品属性类型不正确：" + attribute.getAttributeType());
        }
        attribute.setOptionSource(StringUtils.defaultIfBlank(attribute.getOptionSource(), OPTION_SOURCE_NONE).trim().toUpperCase());
        normalizeAttributeOptionSource(attribute);
        normalizeAttributeNumberConfig(attribute);
        attribute.setStatus(StringUtils.defaultIfBlank(attribute.getStatus(), STATUS_NORMAL));
        attribute.setRemark(StringUtils.defaultString(attribute.getRemark()));
    }

    private void normalizeAttributeOptionSource(ProductAttribute attribute)
    {
        String attributeType = attribute.getAttributeType();
        String optionSource = attribute.getOptionSource();
        if (!OPTION_ATTRIBUTE_TYPES.contains(attributeType))
        {
            if (!OPTION_SOURCE_NONE.equals(optionSource))
            {
                throw new ServiceException("文本、数字、布尔、日期属性不允许配置选项来源");
            }
            attribute.setOptionSource(OPTION_SOURCE_NONE);
            attribute.setDictType("");
            return;
        }
        if (!OPTION_SOURCE_ATTRIBUTE_OPTION.equals(optionSource) && !OPTION_SOURCE_SYS_DICT.equals(optionSource))
        {
            throw new ServiceException("单选、多选属性必须选择属性自定义选项或若依字典");
        }
        if (OPTION_SOURCE_SYS_DICT.equals(optionSource))
        {
            attribute.setDictType(requireTrim(attribute.getDictType(), "选项来源为若依字典时，字典类型不能为空"));
            return;
        }
        attribute.setDictType("");
    }

    private void normalizeAttributeNumberConfig(ProductAttribute attribute)
    {
        if (!ATTRIBUTE_TYPE_NUMBER.equals(attribute.getAttributeType()))
        {
            if (StringUtils.isNotBlank(attribute.getUnit()))
            {
                throw new ServiceException("只有数字属性才允许配置单位");
            }
            if (attribute.getValuePrecision() != null && attribute.getValuePrecision() != 0)
            {
                throw new ServiceException("只有数字属性才允许配置数值精度");
            }
            attribute.setUnit("");
            attribute.setValuePrecision(0);
            return;
        }
        attribute.setUnit(StringUtils.defaultString(attribute.getUnit()).trim());
        attribute.setValuePrecision(defaultInt(attribute.getValuePrecision()));
        if (attribute.getValuePrecision() < 0 || attribute.getValuePrecision() > 8)
        {
            throw new ServiceException("数字属性的数值精度必须在 0 到 8 之间");
        }
    }

    private void validateCustomOptionAttribute(ProductAttribute attribute)
    {
        if (!OPTION_SOURCE_ATTRIBUTE_OPTION.equals(attribute.getOptionSource()))
        {
            throw new ServiceException("只有选项来源为属性自定义选项的属性才能维护选项");
        }
    }

    private void normalizeOption(Long attributeId, ProductAttributeOption option)
    {
        option.setAttributeId(attributeId);
        option.setOptionCode(normalizeCode(option.getOptionCode(), "属性选项编码不能为空").toUpperCase());
        option.setOptionLabel(requireTrim(option.getOptionLabel(), "属性选项名称不能为空"));
        option.setSortOrder(defaultInt(option.getSortOrder()));
        option.setDefaultFlag(normalizeYesNo(option.getDefaultFlag(), NO));
        option.setStatus(StringUtils.defaultIfBlank(option.getStatus(), STATUS_NORMAL));
        option.setRemark(StringUtils.defaultString(option.getRemark()));
    }

    private void normalizeCategoryAttribute(ProductCategoryAttribute categoryAttribute)
    {
        categoryAttribute.setRuleMode(StringUtils.defaultIfBlank(categoryAttribute.getRuleMode(), RULE_ADD).trim().toUpperCase());
        categoryAttribute.setRequiredFlag(normalizeYesNo(categoryAttribute.getRequiredFlag(), NO));
        categoryAttribute.setVisibleFlag(normalizeYesNo(categoryAttribute.getVisibleFlag(), YES));
        categoryAttribute.setEditableFlag(normalizeYesNo(categoryAttribute.getEditableFlag(), YES));
        categoryAttribute.setFilterableFlag(normalizeYesNo(categoryAttribute.getFilterableFlag(), NO));
        categoryAttribute.setGroupCode(StringUtils.defaultString(categoryAttribute.getGroupCode()).trim().toUpperCase());
        categoryAttribute.setSortOrder(defaultInt(categoryAttribute.getSortOrder()));
        categoryAttribute.setPlaceholder(StringUtils.defaultString(categoryAttribute.getPlaceholder()).trim());
        categoryAttribute.setHelpText(StringUtils.defaultString(categoryAttribute.getHelpText()).trim());
        categoryAttribute.setValidationRule(StringUtils.defaultString(categoryAttribute.getValidationRule()).trim());
        categoryAttribute.setStatus(StringUtils.defaultIfBlank(categoryAttribute.getStatus(), STATUS_NORMAL));
        categoryAttribute.setRemark(StringUtils.defaultString(categoryAttribute.getRemark()));
    }

    private void validateRuleMode(ProductCategory category, ProductAttribute attribute, ProductCategoryAttribute categoryAttribute)
    {
        String ruleMode = categoryAttribute.getRuleMode();
        if (!RULE_ADD.equals(ruleMode) && !RULE_OVERRIDE.equals(ruleMode) && !RULE_DISABLE.equals(ruleMode))
        {
            throw new ServiceException("类目属性规则模式不正确");
        }
        if ((RULE_OVERRIDE.equals(ruleMode) || RULE_DISABLE.equals(ruleMode))
            && !hasInheritedAttribute(category, attribute.getAttributeId()))
        {
            throw new ServiceException("当前类目没有可继承的该属性，不能调整或停用继承规则");
        }
    }

    private boolean hasInheritedAttribute(ProductCategory category, Long attributeId)
    {
        List<Long> categoryIds = categoryChain(category);
        categoryIds.remove(category.getCategoryId());
        if (categoryIds.isEmpty())
        {
            return false;
        }
        for (ProductCategoryAttribute rule : productConfigMapper.selectCategoryAttributeRulesByCategoryIds(categoryIds))
        {
            if (attributeId.equals(rule.getAttributeId()) && !RULE_DISABLE.equals(rule.getRuleMode()))
            {
                return true;
            }
        }
        return false;
    }

    private List<Long> categoryChain(ProductCategory category)
    {
        List<Long> categoryIds = new ArrayList<>();
        String[] ancestorIds = StringUtils.split(category.getAncestors(), ",");
        if (ancestorIds != null)
        {
            for (String ancestorId : ancestorIds)
            {
                if (!String.valueOf(ROOT_PARENT_ID).equals(ancestorId))
                {
                    categoryIds.add(Long.valueOf(ancestorId));
                }
            }
        }
        categoryIds.add(category.getCategoryId());
        return categoryIds;
    }

    private List<ProductCategoryAttribute> withOptions(List<ProductCategoryAttribute> rules)
    {
        for (ProductCategoryAttribute rule : rules)
        {
            withOptions(rule);
        }
        return rules;
    }

    private ProductCategoryAttribute withOptions(ProductCategoryAttribute rule)
    {
        if ("ATTRIBUTE_OPTION".equals(rule.getOptionSource()))
        {
            rule.setOptions(productConfigMapper.selectOptionList(rule.getAttributeId()));
        }
        return rule;
    }

    private String normalizeCode(String code, String message)
    {
        String value = requireTrim(code, message);
        return value.toLowerCase();
    }

    private String normalizeEnumCode(String code, String message)
    {
        String value = requireTrim(code, message);
        return value.toUpperCase();
    }

    private String requireTrim(String value, String message)
    {
        String result = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(result))
        {
            throw new ServiceException(message);
        }
        return result;
    }

    private String normalizeYesNo(String value, String defaultValue)
    {
        String result = StringUtils.defaultIfBlank(value, defaultValue).trim().toUpperCase();
        return YES.equals(result) ? YES : NO;
    }

    private int defaultInt(Integer value)
    {
        return value == null ? 0 : value;
    }

    private String currentUsername()
    {
        return SecurityUtils.getUsername();
    }

    private void invalidateSchemaByCategoryId(Long categoryId)
    {
        List<Long> categoryIds = collectSchemaCategoryIdsByCategoryId(categoryId);
        clearSchemaCaches(categoryIds);
        delayClearSchemaCaches(categoryIds);
    }

    private void invalidateSchemaByAttributeId(Long attributeId)
    {
        List<Long> categoryIds = collectSchemaCategoryIdsByAttributeId(attributeId);
        clearSchemaCaches(categoryIds);
        delayClearSchemaCaches(categoryIds);
    }

    private List<Long> collectSchemaCategoryIdsByCategoryId(Long categoryId)
    {
        return productConfigMapper.selectCategoryDescendantIds(categoryId);
    }

    private List<Long> collectSchemaCategoryIdsByAttributeId(Long attributeId)
    {
        List<Long> categoryIds = productConfigMapper.selectCategoryIdsByAttributeId(attributeId);
        if (categoryIds == null || categoryIds.isEmpty())
        {
            return categoryIds;
        }
        LinkedHashSet<Long> descendantCategoryIds = new LinkedHashSet<>();
        for (Long categoryId : categoryIds)
        {
            descendantCategoryIds.addAll(productConfigMapper.selectCategoryDescendantIds(categoryId));
        }
        return new ArrayList<>(descendantCategoryIds);
    }

    private void clearSchemaCaches(Collection<Long> categoryIds)
    {
        if (categoryIds == null || categoryIds.isEmpty())
        {
            return;
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (Long categoryId : categoryIds)
        {
            keys.add(CacheConstants.PRODUCT_SCHEMA + categoryId);
        }
        redisCache.deleteObject(keys);
    }

    private void delayClearSchemaCaches(Collection<Long> categoryIds)
    {
        if (categoryIds == null || categoryIds.isEmpty())
        {
            return;
        }
        List<Long> snapshot = new ArrayList<>(categoryIds);
        CompletableFuture.runAsync(() -> {
            try
            {
                Thread.sleep(500L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
            clearSchemaCaches(snapshot);
        });
    }
}
