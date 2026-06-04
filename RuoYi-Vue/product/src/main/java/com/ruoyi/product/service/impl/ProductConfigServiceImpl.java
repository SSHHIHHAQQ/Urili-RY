package com.ruoyi.product.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final String OPTION_SOURCE_SYS_DICT = "SYS_DICT";

    private static final String RULE_ADD = "ADD";

    private static final String RULE_OVERRIDE = "OVERRIDE";

    private static final String RULE_DISABLE = "DISABLE";

    @Autowired
    private ProductConfigMapper productConfigMapper;

    @Override
    public List<ProductCategory> selectCategoryList(ProductCategory query)
    {
        return productConfigMapper.selectCategoryList(query);
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
        validateParentCanHaveChild(parent);
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
        category.setCreateBy(currentUsername());
        return productConfigMapper.insertCategory(category);
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
        validatePublishLeaf(category);
        category.setAncestors(current.getAncestors());
        category.setCategoryLevel(current.getCategoryLevel());
        category.setSchemaVersion(current.getSchemaVersion());
        category.setUpdateBy(currentUsername());
        return productConfigMapper.updateCategory(category);
    }

    @Override
    @Transactional
    public int deleteCategoryById(Long categoryId)
    {
        selectCategoryById(categoryId);
        if (productConfigMapper.countChildCategories(categoryId) > 0)
        {
            throw new ServiceException("存在下级商品分类，不允许删除");
        }
        if (productConfigMapper.countCategoryAttributes(categoryId) > 0)
        {
            throw new ServiceException("该分类已配置属性，请先移除类目属性配置");
        }
        return productConfigMapper.deleteCategoryById(categoryId, currentUsername());
    }

    @Override
    public List<ProductAttribute> selectAttributeList(ProductAttribute query)
    {
        return productConfigMapper.selectAttributeList(query);
    }

    @Override
    public List<ProductAttribute> selectEnabledAttributeList()
    {
        return productConfigMapper.selectEnabledAttributeList();
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
        return productConfigMapper.insertAttribute(attribute);
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
        return productConfigMapper.updateAttribute(attribute);
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
        return productConfigMapper.deleteAttributeById(attributeId, currentUsername());
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
        selectAttributeById(attributeId);
        normalizeOption(attributeId, option);
        if (productConfigMapper.countOptionCode(attributeId, option.getOptionCode(), null) > 0)
        {
            throw new ServiceException("属性选项编码已存在");
        }
        option.setCreateBy(currentUsername());
        return productConfigMapper.insertOption(option);
    }

    @Override
    @Transactional
    public int updateOption(Long attributeId, Long optionId, ProductAttributeOption option)
    {
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
        return productConfigMapper.updateOption(option);
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
        return productConfigMapper.deleteOptionById(optionId, currentUsername());
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
        productConfigMapper.increaseCategorySchemaVersion(category.getCategoryId(), categoryAttribute.getUpdateBy());
        return rows;
    }

    @Override
    @Transactional
    public int deleteCategoryAttributeById(Long categoryAttributeId)
    {
        ProductCategoryAttribute current = selectCategoryAttributeById(categoryAttributeId);
        int rows = productConfigMapper.deleteCategoryAttributeById(categoryAttributeId, currentUsername());
        productConfigMapper.increaseCategorySchemaVersion(current.getCategoryId(), currentUsername());
        return rows;
    }

    @Override
    public List<ProductCategoryAttribute> previewCategorySchema(Long categoryId)
    {
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
        return withOptions(new ArrayList<>(schema.values()));
    }

    private void normalizeCategory(ProductCategory category)
    {
        category.setParentId(category.getParentId() == null ? ROOT_PARENT_ID : category.getParentId());
        category.setCategoryCode(normalizeCode(category.getCategoryCode(), "商品分类编码不能为空"));
        category.setCategoryName(requireTrim(category.getCategoryName(), "商品分类名称不能为空"));
        category.setPublishEnabled(normalizeYesNo(category.getPublishEnabled(), NO));
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

    private void validateParentCanHaveChild(ProductCategory parent)
    {
        if (parent != null && YES.equals(parent.getPublishEnabled()))
        {
            throw new ServiceException("可发布的叶子类目不能继续添加子类目");
        }
    }

    private void validatePublishLeaf(ProductCategory category)
    {
        if (YES.equals(category.getPublishEnabled()) && productConfigMapper.countChildCategories(category.getCategoryId()) > 0)
        {
            throw new ServiceException("只有最末级商品分类可以设置为可发布");
        }
    }

    private void normalizeAttribute(ProductAttribute attribute)
    {
        attribute.setAttributeCode(normalizeCode(attribute.getAttributeCode(), "商品属性编码不能为空"));
        attribute.setAttributeName(requireTrim(attribute.getAttributeName(), "商品属性名称不能为空"));
        attribute.setAttributeType(normalizeEnumCode(attribute.getAttributeType(), "商品属性类型不能为空"));
        attribute.setOptionSource(StringUtils.defaultIfBlank(attribute.getOptionSource(), OPTION_SOURCE_NONE).trim().toUpperCase());
        if (!OPTION_SOURCE_SYS_DICT.equals(attribute.getOptionSource()))
        {
            attribute.setDictType("");
        }
        attribute.setUnit(StringUtils.defaultString(attribute.getUnit()).trim());
        attribute.setStatus(StringUtils.defaultIfBlank(attribute.getStatus(), STATUS_NORMAL));
        attribute.setRemark(StringUtils.defaultString(attribute.getRemark()));
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
}
