package com.ruoyi.product.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.product.domain.ProductAttribute;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;
import com.ruoyi.product.domain.ProductConfigChangeLog;
import com.ruoyi.product.mapper.ProductConfigChangeLogMapper;

/**
 * 商品配置修改记录服务。
 */
@Service
public class ProductConfigChangeLogService
{
    public static final String BIZ_CATEGORY = "CATEGORY";
    public static final String BIZ_ATTRIBUTE = "ATTRIBUTE";
    public static final String BIZ_ATTRIBUTE_OPTION = "ATTRIBUTE_OPTION";
    public static final String BIZ_CATEGORY_ATTRIBUTE_RULE = "CATEGORY_ATTRIBUTE_RULE";

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_ENABLE = "ENABLE";
    public static final String ACTION_DISABLE = "DISABLE";
    public static final String ACTION_DELETE = "DELETE";

    private static final Map<String, String> FIELD_LABELS = buildFieldLabels();

    @Autowired
    private ProductConfigChangeLogMapper changeLogMapper;

    public List<ProductConfigChangeLog> selectChangeLogList(ProductConfigChangeLog query)
    {
        return changeLogMapper.selectChangeLogList(query);
    }

    public void recordCategory(String actionType, ProductCategory before, ProductCategory after)
    {
        record(BIZ_CATEGORY, actionType, categoryId(before, after), categoryCode(before, after),
            categoryName(before, after), categorySnapshot(before), categorySnapshot(after));
    }

    public void recordAttribute(String actionType, ProductAttribute before, ProductAttribute after)
    {
        record(BIZ_ATTRIBUTE, actionType, attributeId(before, after), attributeCode(before, after),
            attributeName(before, after), attributeSnapshot(before), attributeSnapshot(after));
    }

    public void recordOption(String actionType, ProductAttributeOption before, ProductAttributeOption after)
    {
        record(BIZ_ATTRIBUTE_OPTION, actionType, optionId(before, after), optionCode(before, after),
            optionName(before, after), optionSnapshot(before), optionSnapshot(after));
    }

    public void recordCategoryAttribute(String actionType, ProductCategoryAttribute before,
        ProductCategoryAttribute after)
    {
        record(BIZ_CATEGORY_ATTRIBUTE_RULE, actionType, categoryAttributeId(before, after),
            categoryAttributeCode(before, after), categoryAttributeName(before, after),
            categoryAttributeSnapshot(before), categoryAttributeSnapshot(after));
    }

    private void record(String bizType, String actionType, Long bizId, String bizCode, String bizName,
        Map<String, Object> beforeSnapshot, Map<String, Object> afterSnapshot)
    {
        if (bizId == null)
        {
            return;
        }
        List<Map<String, Object>> diff = diff(beforeSnapshot, afterSnapshot);
        ProductConfigChangeLog changeLog = new ProductConfigChangeLog();
        changeLog.setBizType(bizType);
        changeLog.setBizId(bizId);
        changeLog.setBizCode(StringUtils.defaultString(bizCode));
        changeLog.setBizName(StringUtils.defaultString(bizName));
        changeLog.setActionType(actionType);
        changeLog.setActionSource(ProductConfigChangeContext.getActionSource());
        changeLog.setOperatorName(currentUsername());
        changeLog.setChangeSummary(buildSummary(actionType, bizName, diff));
        changeLog.setBeforeJson(snapshotJson(beforeSnapshot));
        changeLog.setAfterJson(snapshotJson(afterSnapshot));
        changeLog.setDiffJson(JSON.toJSONString(diff));
        changeLog.setRemark("");
        changeLogMapper.insertChangeLog(changeLog);
    }

    private List<Map<String, Object>> diff(Map<String, Object> beforeSnapshot, Map<String, Object> afterSnapshot)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        if (beforeSnapshot != null)
        {
            fields.addAll(beforeSnapshot.keySet());
        }
        if (afterSnapshot != null)
        {
            fields.addAll(afterSnapshot.keySet());
        }
        for (String field : fields)
        {
            Object beforeValue = beforeSnapshot == null ? null : beforeSnapshot.get(field);
            Object afterValue = afterSnapshot == null ? null : afterSnapshot.get(field);
            if (Objects.equals(beforeValue, afterValue))
            {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            item.put("fieldLabel", fieldLabel(field));
            item.put("beforeValue", displayValue(field, beforeValue));
            item.put("afterValue", displayValue(field, afterValue));
            result.add(item);
        }
        return result;
    }

    private String buildSummary(String actionType, String bizName, List<Map<String, Object>> diff)
    {
        String actionLabel = actionLabel(actionType);
        String name = StringUtils.defaultIfBlank(bizName, "当前对象");
        if (ACTION_CREATE.equals(actionType) || ACTION_DELETE.equals(actionType))
        {
            return actionLabel + "：" + name;
        }
        if (diff == null || diff.isEmpty())
        {
            return actionLabel + "：" + name + "，无字段变化";
        }
        String fields = diff.stream()
            .map(item -> String.valueOf(item.get("fieldLabel")))
            .limit(8)
            .collect(Collectors.joining("、"));
        if (diff.size() > 8)
        {
            fields = fields + "等";
        }
        return actionLabel + "：" + fields;
    }

    private String snapshotJson(Map<String, Object> snapshot)
    {
        return snapshot == null ? null : JSON.toJSONString(snapshot);
    }

    private Map<String, Object> categorySnapshot(ProductCategory category)
    {
        if (category == null)
        {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("parentId", category.getParentId());
        snapshot.put("categoryCode", category.getCategoryCode());
        snapshot.put("categoryName", category.getCategoryName());
        snapshot.put("categoryLevel", category.getCategoryLevel());
        snapshot.put("sortOrder", category.getSortOrder());
        snapshot.put("status", category.getStatus());
        snapshot.put("remark", category.getRemark());
        return snapshot;
    }

    private Map<String, Object> attributeSnapshot(ProductAttribute attribute)
    {
        if (attribute == null)
        {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("attributeCode", attribute.getAttributeCode());
        snapshot.put("attributeName", attribute.getAttributeName());
        snapshot.put("attributeType", attribute.getAttributeType());
        snapshot.put("optionSource", attribute.getOptionSource());
        snapshot.put("dictType", attribute.getDictType());
        snapshot.put("unit", attribute.getUnit());
        snapshot.put("valuePrecision", attribute.getValuePrecision());
        snapshot.put("status", attribute.getStatus());
        snapshot.put("remark", attribute.getRemark());
        return snapshot;
    }

    private Map<String, Object> optionSnapshot(ProductAttributeOption option)
    {
        if (option == null)
        {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("attributeId", option.getAttributeId());
        snapshot.put("optionCode", option.getOptionCode());
        snapshot.put("optionLabel", option.getOptionLabel());
        snapshot.put("sortOrder", option.getSortOrder());
        snapshot.put("defaultFlag", option.getDefaultFlag());
        snapshot.put("status", option.getStatus());
        snapshot.put("remark", option.getRemark());
        return snapshot;
    }

    private Map<String, Object> categoryAttributeSnapshot(ProductCategoryAttribute rule)
    {
        if (rule == null)
        {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("categoryId", rule.getCategoryId());
        snapshot.put("attributeId", rule.getAttributeId());
        snapshot.put("ruleMode", rule.getRuleMode());
        snapshot.put("requiredFlag", rule.getRequiredFlag());
        snapshot.put("visibleFlag", rule.getVisibleFlag());
        snapshot.put("editableFlag", rule.getEditableFlag());
        snapshot.put("filterableFlag", rule.getFilterableFlag());
        snapshot.put("groupCode", rule.getGroupCode());
        snapshot.put("sortOrder", rule.getSortOrder());
        snapshot.put("placeholder", rule.getPlaceholder());
        snapshot.put("helpText", rule.getHelpText());
        snapshot.put("validationRule", rule.getValidationRule());
        snapshot.put("status", rule.getStatus());
        snapshot.put("remark", rule.getRemark());
        return snapshot;
    }

    private Object displayValue(String field, Object value)
    {
        if (value == null)
        {
            return "";
        }
        String text = String.valueOf(value);
        if (StringUtils.isBlank(text))
        {
            return "";
        }
        if ("status".equals(field))
        {
            return "0".equals(text) ? "正常" : "停用";
        }
        if ("defaultFlag".equals(field) || "requiredFlag".equals(field) || "visibleFlag".equals(field)
            || "editableFlag".equals(field) || "filterableFlag".equals(field))
        {
            return "Y".equals(text) ? "是" : "否";
        }
        if ("ruleMode".equals(field))
        {
            return switch (text)
            {
                case "ADD" -> "新增";
                case "OVERRIDE" -> "覆盖";
                case "DISABLE" -> "停用继承";
                default -> text;
            };
        }
        if ("attributeType".equals(field))
        {
            return switch (text)
            {
                case "TEXT" -> "文本";
                case "NUMBER" -> "数字";
                case "BOOLEAN" -> "是否";
                case "SINGLE_SELECT" -> "单选";
                case "MULTI_SELECT" -> "多选";
                case "DATE" -> "日期";
                default -> text;
            };
        }
        if ("optionSource".equals(field))
        {
            return switch (text)
            {
                case "NONE" -> "无选项";
                case "ATTRIBUTE_OPTION" -> "属性自定义选项";
                case "SYS_DICT" -> "若依字典";
                default -> text;
            };
        }
        return value;
    }

    private String actionLabel(String actionType)
    {
        return switch (actionType)
        {
            case ACTION_CREATE -> "新增";
            case ACTION_UPDATE -> "修改";
            case ACTION_ENABLE -> "启用";
            case ACTION_DISABLE -> "停用";
            case ACTION_DELETE -> "删除";
            default -> actionType;
        };
    }

    private String fieldLabel(String field)
    {
        return FIELD_LABELS.getOrDefault(field, field);
    }

    private String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    private Long categoryId(ProductCategory before, ProductCategory after)
    {
        return after != null ? after.getCategoryId() : before == null ? null : before.getCategoryId();
    }

    private String categoryCode(ProductCategory before, ProductCategory after)
    {
        return after != null ? after.getCategoryCode() : before == null ? "" : before.getCategoryCode();
    }

    private String categoryName(ProductCategory before, ProductCategory after)
    {
        return after != null ? after.getCategoryName() : before == null ? "" : before.getCategoryName();
    }

    private Long attributeId(ProductAttribute before, ProductAttribute after)
    {
        return after != null ? after.getAttributeId() : before == null ? null : before.getAttributeId();
    }

    private String attributeCode(ProductAttribute before, ProductAttribute after)
    {
        return after != null ? after.getAttributeCode() : before == null ? "" : before.getAttributeCode();
    }

    private String attributeName(ProductAttribute before, ProductAttribute after)
    {
        return after != null ? after.getAttributeName() : before == null ? "" : before.getAttributeName();
    }

    private Long optionId(ProductAttributeOption before, ProductAttributeOption after)
    {
        return after != null ? after.getOptionId() : before == null ? null : before.getOptionId();
    }

    private String optionCode(ProductAttributeOption before, ProductAttributeOption after)
    {
        return after != null ? after.getOptionCode() : before == null ? "" : before.getOptionCode();
    }

    private String optionName(ProductAttributeOption before, ProductAttributeOption after)
    {
        return after != null ? after.getOptionLabel() : before == null ? "" : before.getOptionLabel();
    }

    private Long categoryAttributeId(ProductCategoryAttribute before, ProductCategoryAttribute after)
    {
        return after != null ? after.getCategoryAttributeId() : before == null ? null : before.getCategoryAttributeId();
    }

    private String categoryAttributeCode(ProductCategoryAttribute before, ProductCategoryAttribute after)
    {
        ProductCategoryAttribute source = after != null ? after : before;
        if (source == null)
        {
            return "";
        }
        return source.getCategoryId() + ":" + StringUtils.defaultIfBlank(source.getAttributeCode(),
            String.valueOf(source.getAttributeId()));
    }

    private String categoryAttributeName(ProductCategoryAttribute before, ProductCategoryAttribute after)
    {
        ProductCategoryAttribute source = after != null ? after : before;
        if (source == null)
        {
            return "";
        }
        return StringUtils.defaultString(source.getCategoryName()) + " / "
            + StringUtils.defaultString(source.getAttributeName());
    }

    private static Map<String, String> buildFieldLabels()
    {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("parentId", "上级分类");
        labels.put("categoryCode", "分类编码");
        labels.put("categoryName", "分类名称");
        labels.put("categoryLevel", "层级");
        labels.put("sortOrder", "排序");
        labels.put("status", "状态");
        labels.put("remark", "备注");
        labels.put("attributeCode", "属性编码");
        labels.put("attributeName", "属性名称");
        labels.put("attributeType", "属性类型");
        labels.put("optionSource", "选项来源");
        labels.put("dictType", "字典类型");
        labels.put("unit", "单位");
        labels.put("valuePrecision", "小数位数");
        labels.put("attributeId", "商品属性");
        labels.put("optionCode", "选项编码");
        labels.put("optionLabel", "选项名称");
        labels.put("defaultFlag", "默认");
        labels.put("categoryId", "商品分类");
        labels.put("ruleMode", "规则模式");
        labels.put("requiredFlag", "必填");
        labels.put("visibleFlag", "展示");
        labels.put("editableFlag", "可编辑");
        labels.put("filterableFlag", "可筛选");
        labels.put("groupCode", "属性分组");
        labels.put("placeholder", "占位提示");
        labels.put("helpText", "帮助文案");
        labels.put("validationRule", "校验规则");
        return labels;
    }
}
