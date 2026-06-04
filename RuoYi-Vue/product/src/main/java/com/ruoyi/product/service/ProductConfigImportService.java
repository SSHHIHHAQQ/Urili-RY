package com.ruoyi.product.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductAttribute;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.importdata.ProductAttributeImportRow;
import com.ruoyi.product.domain.importdata.ProductAttributeOptionImportRow;
import com.ruoyi.product.domain.importdata.ProductCategoryImportRow;
import com.ruoyi.product.domain.importdata.ProductImportResult;
import com.ruoyi.product.mapper.ProductConfigMapper;

/**
 * 商品分类与属性配置导入服务。
 */
@Service
public class ProductConfigImportService
{
    private static final long ROOT_PARENT_ID = 0L;

    private static final String STATUS_NORMAL = "0";

    private static final String YES = "Y";

    private static final String NO = "N";

    private static final String OPTION_SOURCE_NONE = "NONE";

    private static final String OPTION_SOURCE_ATTRIBUTE_OPTION = "ATTRIBUTE_OPTION";

    private static final String OPTION_SOURCE_SYS_DICT = "SYS_DICT";

    private static final Set<String> ATTRIBUTE_TYPES = Set.of("TEXT", "NUMBER", "BOOLEAN", "SINGLE_SELECT",
        "MULTI_SELECT", "DATE");

    private static final Set<String> OPTION_ATTRIBUTE_TYPES = Set.of("SINGLE_SELECT", "MULTI_SELECT");

    private static final Set<String> OPTION_SOURCES = Set.of("NONE", "ATTRIBUTE_OPTION", "SYS_DICT");

    @Autowired
    private ProductConfigMapper productConfigMapper;

    @Autowired
    private IProductConfigService productConfigService;

    public ProductImportResult previewCategories(List<ProductCategoryImportRow> rows, boolean updateSupport)
    {
        return processCategories(rows, updateSupport, true);
    }

    @Transactional
    public ProductImportResult importCategories(List<ProductCategoryImportRow> rows, boolean updateSupport)
    {
        ProductImportResult checkResult = processCategories(rows, updateSupport, true);
        if (!checkResult.isPassed())
        {
            return checkResult;
        }
        ProductImportResult importResult = processCategories(rows, updateSupport, false);
        assertActualPassed(importResult);
        return importResult;
    }

    public ProductImportResult previewAttributes(List<ProductAttributeImportRow> rows, boolean updateSupport)
    {
        return processAttributes(rows, updateSupport, true);
    }

    @Transactional
    public ProductImportResult importAttributes(List<ProductAttributeImportRow> rows, boolean updateSupport)
    {
        ProductImportResult checkResult = processAttributes(rows, updateSupport, true);
        if (!checkResult.isPassed())
        {
            return checkResult;
        }
        ProductImportResult importResult = processAttributes(rows, updateSupport, false);
        assertActualPassed(importResult);
        return importResult;
    }

    public ProductImportResult previewOptions(List<ProductAttributeOptionImportRow> rows, boolean updateSupport)
    {
        return processOptions(rows, updateSupport, true);
    }

    @Transactional
    public ProductImportResult importOptions(List<ProductAttributeOptionImportRow> rows, boolean updateSupport)
    {
        ProductImportResult checkResult = processOptions(rows, updateSupport, true);
        if (!checkResult.isPassed())
        {
            return checkResult;
        }
        ProductImportResult importResult = processOptions(rows, updateSupport, false);
        assertActualPassed(importResult);
        return importResult;
    }

    private ProductImportResult processCategories(List<ProductCategoryImportRow> rows, boolean updateSupport,
        boolean dryRun)
    {
        ProductImportResult result = initResult(rows);
        Map<String, ProductCategory> importedByCode = new HashMap<>();
        Set<String> rowCodes = new HashSet<>();
        Set<String> rowNames = new HashSet<>();
        if (rows == null || rows.isEmpty())
        {
            result.addError(1, "导入文件不能为空");
            return result;
        }
        for (int index = 0; index < rows.size(); index++)
        {
            int rowNum = index + 2;
            try
            {
                ProductCategoryImportRow row = rows.get(index);
                ProductCategory category = buildCategory(row);
                String categoryCode = category.getCategoryCode();
                if (!rowCodes.add(categoryCode))
                {
                    result.addError(rowNum, "分类编码在导入文件中重复：" + categoryCode);
                    continue;
                }
                ProductCategory existing = productConfigMapper.selectCategoryByCode(categoryCode);
                ParentResolveResult parentResult = resolveCategoryParent(row.getParentCategoryCode(), importedByCode);
                String parentCode = parentResult.getParentCode();
                String rowNameKey = parentCode + ":" + category.getCategoryName();
                if (!rowNames.add(rowNameKey))
                {
                    result.addError(rowNum, "同一父级下分类名称在导入文件中重复：" + category.getCategoryName());
                    continue;
                }
                if (existing == null)
                {
                    if (!dryRun)
                    {
                        category.setParentId(parentResult.getParentId());
                        productConfigService.insertCategory(category);
                        importedByCode.put(categoryCode, productConfigMapper.selectCategoryByCode(categoryCode));
                    }
                    else
                    {
                        category.setParentId(parentResult.getParentId());
                        importedByCode.put(categoryCode, category);
                    }
                    result.addCreate(rowNum, "分类 " + categoryCode + " 将新增");
                    continue;
                }
                if (!updateSupport)
                {
                    result.addError(rowNum, "分类编码已存在，且未启用更新：" + categoryCode);
                    continue;
                }
                validateCategoryUpdate(rowNum, result, existing, category, parentResult);
                if (hasRowError(result, rowNum))
                {
                    continue;
                }
                if (!dryRun)
                {
                    category.setCategoryId(existing.getCategoryId());
                    category.setParentId(existing.getParentId());
                    productConfigService.updateCategory(category);
                    importedByCode.put(categoryCode, productConfigMapper.selectCategoryByCode(categoryCode));
                }
                else
                {
                    category.setCategoryId(existing.getCategoryId());
                    category.setParentId(existing.getParentId());
                    importedByCode.put(categoryCode, category);
                }
                result.addUpdate(rowNum, "分类 " + categoryCode + " 将更新");
            }
            catch (Exception e)
            {
                handleRowException(result, rowNum, e, dryRun);
            }
        }
        return result;
    }

    private ProductImportResult processAttributes(List<ProductAttributeImportRow> rows, boolean updateSupport,
        boolean dryRun)
    {
        ProductImportResult result = initResult(rows);
        Set<String> rowCodes = new HashSet<>();
        if (rows == null || rows.isEmpty())
        {
            result.addError(1, "导入文件不能为空");
            return result;
        }
        for (int index = 0; index < rows.size(); index++)
        {
            int rowNum = index + 2;
            try
            {
                ProductAttribute attribute = buildAttribute(rows.get(index));
                String attributeCode = attribute.getAttributeCode();
                if (!rowCodes.add(attributeCode))
                {
                    result.addError(rowNum, "属性编码在导入文件中重复：" + attributeCode);
                    continue;
                }
                ProductAttribute existing = productConfigMapper.selectAttributeByCode(attributeCode);
                if (existing == null)
                {
                    if (!dryRun)
                    {
                        productConfigService.insertAttribute(attribute);
                    }
                    result.addCreate(rowNum, "属性 " + attributeCode + " 将新增");
                    continue;
                }
                if (!updateSupport)
                {
                    result.addError(rowNum, "属性编码已存在，且未启用更新：" + attributeCode);
                    continue;
                }
                if (productConfigMapper.countAttributeCategoryBindings(existing.getAttributeId()) > 0
                    && !Objects.equals(existing.getAttributeType(), attribute.getAttributeType()))
                {
                    result.addError(rowNum, "属性已被类目引用，不允许通过导入修改属性类型：" + attributeCode);
                    continue;
                }
                if (!dryRun)
                {
                    attribute.setAttributeId(existing.getAttributeId());
                    productConfigService.updateAttribute(attribute);
                }
                result.addUpdate(rowNum, "属性 " + attributeCode + " 将更新");
            }
            catch (Exception e)
            {
                handleRowException(result, rowNum, e, dryRun);
            }
        }
        return result;
    }

    private ProductImportResult processOptions(List<ProductAttributeOptionImportRow> rows, boolean updateSupport,
        boolean dryRun)
    {
        ProductImportResult result = initResult(rows);
        Set<String> rowCodes = new HashSet<>();
        if (rows == null || rows.isEmpty())
        {
            result.addError(1, "导入文件不能为空");
            return result;
        }
        for (int index = 0; index < rows.size(); index++)
        {
            int rowNum = index + 2;
            try
            {
                ProductAttributeOptionImportRow row = rows.get(index);
                String attributeCode = normalizeCode(row.getAttributeCode(), "属性编码不能为空");
                ProductAttribute attribute = productConfigMapper.selectAttributeByCode(attributeCode);
                if (attribute == null)
                {
                    result.addError(rowNum, "属性编码不存在：" + attributeCode);
                    continue;
                }
                if (!OPTION_SOURCE_ATTRIBUTE_OPTION.equals(attribute.getOptionSource()))
                {
                    result.addError(rowNum, "属性选项来源不是 ATTRIBUTE_OPTION：" + attributeCode);
                    continue;
                }
                ProductAttributeOption option = buildOption(row);
                String rowKey = attributeCode + ":" + option.getOptionCode();
                if (!rowCodes.add(rowKey))
                {
                    result.addError(rowNum, "同一属性下选项编码在导入文件中重复：" + option.getOptionCode());
                    continue;
                }
                ProductAttributeOption existing = findOptionByCode(attribute.getAttributeId(), option.getOptionCode());
                if (existing == null)
                {
                    if (!dryRun)
                    {
                        productConfigService.insertOption(attribute.getAttributeId(), option);
                    }
                    result.addCreate(rowNum, "属性选项 " + rowKey + " 将新增");
                    continue;
                }
                if (!updateSupport)
                {
                    result.addError(rowNum, "属性选项已存在，且未启用更新：" + rowKey);
                    continue;
                }
                if (!dryRun)
                {
                    productConfigService.updateOption(attribute.getAttributeId(), existing.getOptionId(), option);
                }
                result.addUpdate(rowNum, "属性选项 " + rowKey + " 将更新");
            }
            catch (Exception e)
            {
                handleRowException(result, rowNum, e, dryRun);
            }
        }
        return result;
    }

    private ProductImportResult initResult(List<?> rows)
    {
        ProductImportResult result = new ProductImportResult();
        result.setTotalCount(rows == null ? 0 : rows.size());
        return result;
    }

    private ProductCategory buildCategory(ProductCategoryImportRow row)
    {
        ProductCategory category = new ProductCategory();
        category.setCategoryCode(normalizeCode(row.getCategoryCode(), "分类编码不能为空"));
        category.setCategoryName(requireTrim(row.getCategoryName(), "分类名称不能为空"));
        category.setSortOrder(defaultInt(row.getSortOrder()));
        category.setStatus(normalizeStatus(row.getStatus()));
        category.setRemark(StringUtils.defaultString(row.getRemark()).trim());
        return category;
    }

    private ProductAttribute buildAttribute(ProductAttributeImportRow row)
    {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setAttributeCode(normalizeCode(row.getAttributeCode(), "属性编码不能为空"));
        attribute.setAttributeName(requireTrim(row.getAttributeName(), "属性名称不能为空"));
        attribute.setAttributeType(normalizeEnum(row.getAttributeType(), "属性类型不能为空"));
        if (!ATTRIBUTE_TYPES.contains(attribute.getAttributeType()))
        {
            throw new ServiceException("属性类型不正确：" + attribute.getAttributeType());
        }
        attribute.setOptionSource(normalizeEnum(row.getOptionSource(), "选项来源不能为空"));
        if (!OPTION_SOURCES.contains(attribute.getOptionSource()))
        {
            throw new ServiceException("选项来源不正确：" + attribute.getOptionSource());
        }
        attribute.setDictType(StringUtils.defaultString(row.getDictType()).trim());
        validateAttributeOptionSource(attribute);
        attribute.setUnit(StringUtils.defaultString(row.getUnit()).trim());
        attribute.setValuePrecision(defaultInt(row.getValuePrecision()));
        attribute.setStatus(normalizeStatus(row.getStatus()));
        attribute.setRemark(StringUtils.defaultString(row.getRemark()).trim());
        return attribute;
    }

    private void validateAttributeOptionSource(ProductAttribute attribute)
    {
        if (!OPTION_ATTRIBUTE_TYPES.contains(attribute.getAttributeType()))
        {
            if (!OPTION_SOURCE_NONE.equals(attribute.getOptionSource()))
            {
                throw new ServiceException("TEXT / NUMBER / BOOLEAN / DATE 属性的选项来源必须为 NONE");
            }
            attribute.setDictType("");
            return;
        }
        if (OPTION_SOURCE_NONE.equals(attribute.getOptionSource()))
        {
            throw new ServiceException("SINGLE_SELECT / MULTI_SELECT 属性必须选择 ATTRIBUTE_OPTION 或 SYS_DICT");
        }
        if (OPTION_SOURCE_SYS_DICT.equals(attribute.getOptionSource()) && StringUtils.isBlank(attribute.getDictType()))
        {
            throw new ServiceException("选项来源为 SYS_DICT 时，字典类型不能为空");
        }
        if (!OPTION_SOURCE_SYS_DICT.equals(attribute.getOptionSource()))
        {
            attribute.setDictType("");
        }
    }

    private ProductAttributeOption buildOption(ProductAttributeOptionImportRow row)
    {
        ProductAttributeOption option = new ProductAttributeOption();
        option.setOptionCode(normalizeCode(row.getOptionCode(), "选项编码不能为空"));
        option.setOptionLabel(requireTrim(row.getOptionLabel(), "选项名称不能为空"));
        option.setSortOrder(defaultInt(row.getSortOrder()));
        option.setDefaultFlag(normalizeYesNo(row.getDefaultFlag(), NO));
        option.setStatus(normalizeStatus(row.getStatus()));
        option.setRemark(StringUtils.defaultString(row.getRemark()).trim());
        return option;
    }

    private ParentResolveResult resolveCategoryParent(String rawParentCode, Map<String, ProductCategory> importedByCode)
    {
        String parentCode = normalizeOptionalCode(rawParentCode);
        if (StringUtils.isBlank(parentCode))
        {
            return new ParentResolveResult("", ROOT_PARENT_ID, null);
        }
        ProductCategory parent = productConfigMapper.selectCategoryByCode(parentCode);
        if (parent == null)
        {
            parent = importedByCode.get(parentCode);
        }
        if (parent == null)
        {
            throw new ServiceException("父级分类编码不存在，或位于当前行之后：" + parentCode);
        }
        return new ParentResolveResult(parentCode, parent.getCategoryId(), parent);
    }

    private void validateCategoryUpdate(Integer rowNum, ProductImportResult result, ProductCategory existing,
        ProductCategory importing, ParentResolveResult parentResult)
    {
        if (parentResult.getParentId() != null && !Objects.equals(existing.getParentId(), parentResult.getParentId()))
        {
            result.addError(rowNum, "第一版不支持通过导入移动商品分类：" + existing.getCategoryCode());
            return;
        }
    }

    private ProductAttributeOption findOptionByCode(Long attributeId, String optionCode)
    {
        for (ProductAttributeOption option : productConfigMapper.selectOptionList(attributeId))
        {
            if (optionCode.equals(option.getOptionCode()))
            {
                return option;
            }
        }
        return null;
    }

    private boolean hasRowError(ProductImportResult result, Integer rowNum)
    {
        return result.getMessages().stream()
            .anyMatch(message -> rowNum.equals(message.getRowNum()) && "ERROR".equals(message.getStatus()));
    }

    private void handleRowException(ProductImportResult result, Integer rowNum, Exception e, boolean dryRun)
    {
        String message = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
        result.addError(rowNum, message);
        if (!dryRun)
        {
            throw e instanceof RuntimeException ? (RuntimeException) e : new ServiceException(message);
        }
    }

    private void assertActualPassed(ProductImportResult result)
    {
        if (result.isPassed())
        {
            return;
        }
        String message = result.getMessages().stream()
            .filter(row -> "ERROR".equals(row.getStatus()))
            .map(row -> "第 " + row.getRowNum() + " 行：" + row.getMessage())
            .findFirst()
            .orElse("导入执行失败");
        throw new ServiceException(message);
    }

    private String normalizeCode(String code, String message)
    {
        return requireTrim(code, message).toLowerCase();
    }

    private String normalizeOptionalCode(String code)
    {
        return StringUtils.trimToEmpty(code).toLowerCase();
    }

    private String normalizeEnum(String code, String message)
    {
        return requireTrim(code, message).toUpperCase();
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

    private String normalizeStatus(String value)
    {
        return "1".equals(StringUtils.defaultIfBlank(value, STATUS_NORMAL).trim()) ? "1" : STATUS_NORMAL;
    }

    private int defaultInt(Integer value)
    {
        return value == null ? 0 : value;
    }

    private static class ParentResolveResult
    {
        private final String parentCode;
        private final Long parentId;
        private final ProductCategory parent;

        ParentResolveResult(String parentCode, Long parentId, ProductCategory parent)
        {
            this.parentCode = parentCode;
            this.parentId = parentId;
            this.parent = parent;
        }

        String getParentCode()
        {
            return parentCode;
        }

        Long getParentId()
        {
            return parentId;
        }

        ProductCategory getParent()
        {
            return parent;
        }
    }
}
