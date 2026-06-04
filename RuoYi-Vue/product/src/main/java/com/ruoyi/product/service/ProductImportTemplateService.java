package com.ruoyi.product.service;

import java.util.Collections;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.poi.ExcelSheet;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.product.domain.importdata.ProductAttributeImportRow;
import com.ruoyi.product.domain.importdata.ProductAttributeOptionImportRow;
import com.ruoyi.product.domain.importdata.ProductCategoryImportRow;
import com.ruoyi.product.domain.importdata.ProductImportFieldHelpRow;

/**
 * 商品配置导入模板生成服务。
 */
@Service
public class ProductImportTemplateService
{
    public void exportCategoryTemplate(HttpServletResponse response)
    {
        ExcelUtil.exportMultiSheet(response, List.of(
            new ExcelSheet<ProductCategoryImportRow>("商品分类导入", Collections.emptyList(),
                ProductCategoryImportRow.class),
            new ExcelSheet<ProductCategoryImportRow>("填写示例", categoryExamples(), ProductCategoryImportRow.class),
            new ExcelSheet<ProductImportFieldHelpRow>("字段说明", categoryHelpRows(), ProductImportFieldHelpRow.class)));
    }

    public void exportAttributeTemplate(HttpServletResponse response)
    {
        ExcelUtil.exportMultiSheet(response, List.of(
            new ExcelSheet<ProductAttributeImportRow>("商品属性导入", Collections.emptyList(),
                ProductAttributeImportRow.class),
            new ExcelSheet<ProductAttributeImportRow>("填写示例", attributeExamples(), ProductAttributeImportRow.class),
            new ExcelSheet<ProductImportFieldHelpRow>("字段说明", attributeHelpRows(), ProductImportFieldHelpRow.class)));
    }

    public void exportAttributeOptionTemplate(HttpServletResponse response)
    {
        ExcelUtil.exportMultiSheet(response, List.of(
            new ExcelSheet<ProductAttributeOptionImportRow>("商品属性选项导入", Collections.emptyList(),
                ProductAttributeOptionImportRow.class),
            new ExcelSheet<ProductAttributeOptionImportRow>("填写示例", attributeOptionExamples(),
                ProductAttributeOptionImportRow.class),
            new ExcelSheet<ProductImportFieldHelpRow>("字段说明", attributeOptionHelpRows(),
                ProductImportFieldHelpRow.class)));
    }

    private List<ProductCategoryImportRow> categoryExamples()
    {
        return List.of(
            categoryRow("apparel", "服装", "", 10, "0", "顶级分类；有子级后系统自动判定为不可发布"),
            categoryRow("apparel_tshirt", "T恤", "apparel", 10, "0", "叶子类目；无子级时系统自动判定为可发布"),
            categoryRow("electronics", "电器", "", 20, "0", "顶级分类；是否可发布不需要人工填写"),
            categoryRow("electronics_battery", "带电池电器", "electronics", 10, "0",
                "父级编码必须在前面行或系统中已存在"));
    }

    private List<ProductAttributeImportRow> attributeExamples()
    {
        return List.of(
            attributeRow("washable", "是否可水洗", "BOOLEAN", "NONE", "", "", 0, "0", "布尔属性，不需要自定义选项"),
            attributeRow("battery_included", "是否带电池", "BOOLEAN", "NONE", "", "", 0, "0",
                "电器类常见布尔属性"),
            attributeRow("material", "材质", "SINGLE_SELECT", "ATTRIBUTE_OPTION", "", "", 0, "0",
                "需要再导入属性选项"),
            attributeRow("size", "尺码", "SINGLE_SELECT", "ATTRIBUTE_OPTION", "", "", 0, "0",
                "服装类常见选择属性"));
    }

    private List<ProductAttributeOptionImportRow> attributeOptionExamples()
    {
        return List.of(
            attributeOptionRow("material", "cotton", "棉", 10, "N", "0", "属性 material 必须已存在"),
            attributeOptionRow("material", "polyester", "聚酯纤维", 20, "N", "0", "同一属性下 optionCode 唯一"),
            attributeOptionRow("size", "s", "S", 10, "N", "0", "属性 size 必须已存在"),
            attributeOptionRow("size", "m", "M", 20, "N", "0", "默认项只能填 Y 或 N"));
    }

    private List<ProductImportFieldHelpRow> categoryHelpRows()
    {
        String template = "商品分类导入";
        return List.of(
            helpRow(template, "分类编码", "是", "业务 code，建议小写英文、数字、下划线；全局唯一", "apparel_tshirt",
                "填写中文、空格或和已有编码重复"),
            helpRow(template, "分类名称", "是", "展示名称；同一父级下不建议重复", "T恤", "同一父级下重复名称"),
            helpRow(template, "父级分类编码", "否", "顶级分类留空；子分类填写父级分类编码", "apparel",
                "父级不存在，或父级行写在子级后面"),
            helpRow(template, "排序", "否", "数字越小越靠前；不填按 0 处理", "10", "填写非数字文本"),
            helpRow(template, "状态", "是", "填写 正常 / 停用", "正常", "填写“启用”等非模板下拉值"),
            helpRow(template, "备注", "否", "内部说明，不参与编码唯一性判断", "叶子类目", "写入需要系统判断的业务规则"));
    }

    private List<ProductImportFieldHelpRow> attributeHelpRows()
    {
        String template = "商品属性导入";
        return List.of(
            helpRow(template, "属性编码", "是", "业务 code，建议小写英文、数字、下划线；全局唯一", "washable",
                "填写中文、空格或和已有编码重复"),
            helpRow(template, "属性名称", "是", "展示名称", "是否可水洗", "同一含义重复维护多个属性"),
            helpRow(template, "属性类型", "是", "TEXT / NUMBER / BOOLEAN / SINGLE_SELECT / MULTI_SELECT / DATE",
                "BOOLEAN", "填写中文类型，例如“单选”"),
            helpRow(template, "选项来源", "是", "无选项填 NONE；自定义选项填 ATTRIBUTE_OPTION；若依字典填 SYS_DICT",
                "ATTRIBUTE_OPTION", "选择型属性填 NONE，导致后续没有选项"),
            helpRow(template, "字典类型", "按条件", "选项来源为 SYS_DICT 时必填，其他来源留空", "product_material",
                "选项来源不是 SYS_DICT 仍填写字典类型"),
            helpRow(template, "单位", "否", "数值类属性可填单位，其他类型通常留空", "cm", "布尔或日期属性填写单位"),
            helpRow(template, "数值精度", "否", "数字属性的小数位；不填按 0 处理", "2", "填写非数字文本"),
            helpRow(template, "状态", "是", "填写 正常 / 停用", "正常", "填写“启用”等非模板下拉值"),
            helpRow(template, "备注", "否", "内部说明，不参与编码唯一性判断", "用于服装类目", "写入需要系统判断的业务规则"));
    }

    private List<ProductImportFieldHelpRow> attributeOptionHelpRows()
    {
        String template = "商品属性选项导入";
        return List.of(
            helpRow(template, "属性编码", "是", "必须是已存在属性，且该属性选项来源为 ATTRIBUTE_OPTION", "material",
                "属性不存在或属性来源为 NONE / SYS_DICT"),
            helpRow(template, "选项编码", "是", "同一个属性下唯一；建议小写英文、数字、下划线", "cotton",
                "同一属性下重复 optionCode"),
            helpRow(template, "选项名称", "是", "展示名称", "棉", "只填编码不填展示名称"),
            helpRow(template, "排序", "否", "数字越小越靠前；不填按 0 处理", "10", "填写非数字文本"),
            helpRow(template, "默认项", "是", "填写 是 / 否", "否", "填写 Y / N 等内部 code"),
            helpRow(template, "状态", "是", "填写 正常 / 停用", "正常", "填写“启用”等非模板下拉值"),
            helpRow(template, "备注", "否", "内部说明，不参与编码唯一性判断", "常用材质", "写入需要系统判断的业务规则"));
    }

    private ProductCategoryImportRow categoryRow(String code, String name, String parentCode, Integer sortOrder,
        String status, String remark)
    {
        ProductCategoryImportRow row = new ProductCategoryImportRow();
        row.setCategoryCode(code);
        row.setCategoryName(name);
        row.setParentCategoryCode(parentCode);
        row.setSortOrder(sortOrder);
        row.setStatus(status);
        row.setRemark(remark);
        return row;
    }

    private ProductAttributeImportRow attributeRow(String code, String name, String type, String optionSource,
        String dictType, String unit, Integer valuePrecision, String status, String remark)
    {
        ProductAttributeImportRow row = new ProductAttributeImportRow();
        row.setAttributeCode(code);
        row.setAttributeName(name);
        row.setAttributeType(type);
        row.setOptionSource(optionSource);
        row.setDictType(dictType);
        row.setUnit(unit);
        row.setValuePrecision(valuePrecision);
        row.setStatus(status);
        row.setRemark(remark);
        return row;
    }

    private ProductAttributeOptionImportRow attributeOptionRow(String attributeCode, String optionCode,
        String optionLabel, Integer sortOrder, String defaultFlag, String status, String remark)
    {
        ProductAttributeOptionImportRow row = new ProductAttributeOptionImportRow();
        row.setAttributeCode(attributeCode);
        row.setOptionCode(optionCode);
        row.setOptionLabel(optionLabel);
        row.setSortOrder(sortOrder);
        row.setDefaultFlag(defaultFlag);
        row.setStatus(status);
        row.setRemark(remark);
        return row;
    }

    private ProductImportFieldHelpRow helpRow(String templateName, String fieldName, String required, String rule,
        String example, String commonMistake)
    {
        ProductImportFieldHelpRow row = new ProductImportFieldHelpRow();
        row.setTemplateName(templateName);
        row.setFieldName(fieldName);
        row.setRequired(required);
        row.setRule(rule);
        row.setExample(example);
        row.setCommonMistake(commonMistake);
        return row;
    }
}
