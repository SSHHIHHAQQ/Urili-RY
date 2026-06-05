package com.ruoyi.product.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.poi.ExcelSheet;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.product.domain.importdata.ProductAttributeImportRow;
import com.ruoyi.product.domain.importdata.ProductAttributeOptionImportRow;
import com.ruoyi.product.domain.importdata.ProductAttributeTypeSourceRuleRow;
import com.ruoyi.product.domain.importdata.ProductCategoryImportRow;
import com.ruoyi.product.domain.importdata.ProductImportFieldHelpRow;

/**
 * 商品配置导入模板生成服务。
 */
@Service
public class ProductImportTemplateService
{
    private static final String ATTRIBUTE_IMPORT_SHEET = "商品属性导入";

    private static final String ATTRIBUTE_TYPE_SOURCE_RULE_SHEET = "类型来源规则";

    private static final String ATTRIBUTE_OPTION_SOURCE_HIDDEN_SHEET = "_attribute_option_source";

    private static final int ATTRIBUTE_TEMPLATE_FIRST_DATA_ROW = 1;

    private static final int DEFAULT_EXCEL_UTIL_LAST_VALIDATION_ROW = 100;

    private static final int ATTRIBUTE_TEMPLATE_LAST_DATA_ROW = 1000;

    private static final int ATTRIBUTE_TYPE_COLUMN_INDEX = 2;

    private static final int OPTION_SOURCE_COLUMN_INDEX = 3;

    private static final int STATUS_COLUMN_INDEX = 7;

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
        exportMultiSheet(response, List.of(
            new ExcelSheet<ProductAttributeImportRow>(ATTRIBUTE_IMPORT_SHEET, Collections.emptyList(),
                ProductAttributeImportRow.class),
            new ExcelSheet<ProductAttributeImportRow>("填写示例", attributeExamples(), ProductAttributeImportRow.class),
            new ExcelSheet<ProductImportFieldHelpRow>("字段说明", attributeHelpRows(), ProductImportFieldHelpRow.class),
            new ExcelSheet<ProductAttributeTypeSourceRuleRow>(ATTRIBUTE_TYPE_SOURCE_RULE_SHEET,
                attributeTypeSourceRuleRows(), ProductAttributeTypeSourceRuleRow.class)),
            this::applyAttributeOptionSourceValidation);
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
            categoryRow("apparel", "服装", "", 10, "0", "顶级分类；有子级后自动作为分组"),
            categoryRow("apparel_tshirt", "T恤", "apparel", 10, "0", "叶子类目；卖家可选择发布商品"),
            categoryRow("electronics", "电器", "", 20, "0", "顶级分类；无需填写发布状态"),
            categoryRow("electronics_battery", "带电池电器", "electronics", 10, "0",
                "父级编码必须在前面行或系统中已存在"));
    }

    private List<ProductAttributeImportRow> attributeExamples()
    {
        return List.of(
            attributeRow("title_keywords", "标题关键词", "TEXT", "NONE", "", "", 0, "0",
                "文本属性，选项来源只能填 NONE"),
            attributeRow("washable", "是否可水洗", "BOOLEAN", "NONE", "", "", 0, "0", "布尔属性，不需要自定义选项"),
            attributeRow("battery_included", "是否带电池", "BOOLEAN", "NONE", "", "", 0, "0",
                "电器类常见布尔属性"),
            attributeRow("clothing_length_cm", "衣长", "NUMBER", "NONE", "", "cm", 1, "0",
                "数字属性，可配置单位和小数位数"),
            attributeRow("package_weight_g", "包装重量", "NUMBER", "NONE", "", "g", 0, "0",
                "数字属性，整数值可把小数位数填 0"),
            attributeRow("release_date", "上市日期", "DATE", "NONE", "", "", 0, "0",
                "日期属性，选项来源只能填 NONE"),
            attributeRow("material", "材质", "SINGLE_SELECT", "ATTRIBUTE_OPTION", "", "", 0, "0",
                "单选属性，选项值需要再导入属性选项"),
            attributeRow("color_family", "颜色分类", "MULTI_SELECT", "ATTRIBUTE_OPTION", "", "", 0, "0",
                "多选属性，选项值需要再导入属性选项"),
            attributeRow("origin_country", "原产国", "SINGLE_SELECT", "SYS_DICT", "product_origin_country", "", 0,
                "0", "复用若依字典，不需要导入属性选项"),
            attributeRow("applicable_season", "适用季节", "MULTI_SELECT", "SYS_DICT", "product_season", "", 0,
                "0", "多选属性也可以复用若依字典"));
    }

    private List<ProductAttributeTypeSourceRuleRow> attributeTypeSourceRuleRows()
    {
        return List.of(
            typeSourceRuleRow("TEXT", "文本", "NONE", "固定填 NONE，不能维护选项来源", "留空", "留空", "填 0 或留空",
                "不导入属性选项", "标题关键词 / TEXT / NONE"),
            typeSourceRuleRow("NUMBER", "数字", "NONE", "固定填 NONE，不能维护选项来源", "留空", "可填 cm、kg、g、pcs 等单位",
                "填 0-8，表示保留几位小数", "不导入属性选项", "衣长 / NUMBER / NONE / cm / 1"),
            typeSourceRuleRow("BOOLEAN", "布尔", "NONE", "固定填 NONE，系统按 是 / 否 使用", "留空", "留空", "填 0 或留空",
                "不导入属性选项", "是否可水洗 / BOOLEAN / NONE"),
            typeSourceRuleRow("DATE", "日期", "NONE", "固定填 NONE，不能维护选项来源", "留空", "留空", "填 0 或留空",
                "不导入属性选项", "上市日期 / DATE / NONE"),
            typeSourceRuleRow("SINGLE_SELECT", "单选", "ATTRIBUTE_OPTION / SYS_DICT",
                "必须二选一：自定义选项填 ATTRIBUTE_OPTION，复用若依字典填 SYS_DICT", "选项来源为 SYS_DICT 时必填字典类型",
                "留空", "填 0 或留空", "ATTRIBUTE_OPTION 要在商品属性选项模板中导入选项；SYS_DICT 不导入属性选项",
                "材质 / SINGLE_SELECT / ATTRIBUTE_OPTION"),
            typeSourceRuleRow("MULTI_SELECT", "多选", "ATTRIBUTE_OPTION / SYS_DICT",
                "必须二选一：自定义选项填 ATTRIBUTE_OPTION，复用若依字典填 SYS_DICT", "选项来源为 SYS_DICT 时必填字典类型",
                "留空", "填 0 或留空", "ATTRIBUTE_OPTION 要在商品属性选项模板中导入选项；SYS_DICT 不导入属性选项",
                "颜色分类 / MULTI_SELECT / ATTRIBUTE_OPTION"));
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
            helpRow(template, "选项来源", "是",
                "与属性类型联动：TEXT / NUMBER / BOOLEAN / DATE 固定填 NONE；SINGLE_SELECT / MULTI_SELECT 填 ATTRIBUTE_OPTION 或 SYS_DICT",
                "ATTRIBUTE_OPTION", "文本、数字、布尔、日期属性填写 ATTRIBUTE_OPTION 或 SYS_DICT"),
            helpRow(template, "字典类型", "按条件",
                "属性类型为 SINGLE_SELECT / MULTI_SELECT 且选项来源为 SYS_DICT 时必填，必须是若依字典类型编码",
                "product_material", "选项来源不是 SYS_DICT 仍填写字典类型，或填写不存在的字典类型"),
            helpRow(template, "单位", "按条件", "仅 NUMBER 属性可填单位，例如 cm、kg、g；其他属性必须留空", "cm",
                "TEXT / BOOLEAN / DATE / SINGLE_SELECT / MULTI_SELECT 属性填写单位"),
            helpRow(template, "小数位数", "按条件", "仅 NUMBER 属性可填，表示保留几位小数；范围 0-8，其他属性填 0 或留空", "2",
                "非 NUMBER 属性填写非 0 精度，或 NUMBER 属性填写 9 以上"),
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

    private ProductAttributeTypeSourceRuleRow typeSourceRuleRow(String attributeType, String typeDescription,
        String allowedOptionSources, String optionSourceRule, String dictTypeRule, String unitRule,
        String precisionRule, String optionValueRule, String example)
    {
        ProductAttributeTypeSourceRuleRow row = new ProductAttributeTypeSourceRuleRow();
        row.setAttributeType(attributeType);
        row.setTypeDescription(typeDescription);
        row.setAllowedOptionSources(allowedOptionSources);
        row.setOptionSourceRule(optionSourceRule);
        row.setDictTypeRule(dictTypeRule);
        row.setUnitRule(unitRule);
        row.setPrecisionRule(precisionRule);
        row.setOptionValueRule(optionValueRule);
        row.setExample(example);
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void exportMultiSheet(HttpServletResponse response, List<ExcelSheet<?>> sheets,
        Consumer<SXSSFWorkbook> workbookCustomizer)
    {
        SXSSFWorkbook wb = new SXSSFWorkbook(500);
        try
        {
            for (ExcelSheet<?> excelSheet : sheets)
            {
                ExcelUtil util = new ExcelUtil(excelSheet.getClazz());
                util.initWithWorkbook(wb, excelSheet.getList(), excelSheet.getSheetName(), excelSheet.getTitle());
                util.writeSheet();
            }
            if (workbookCustomizer != null)
            {
                workbookCustomizer.accept(wb);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            wb.write(response.getOutputStream());
        }
        catch (IOException e)
        {
            throw new ServiceException("导出商品配置模板失败");
        }
        finally
        {
            IOUtils.closeQuietly(wb);
        }
    }

    private void applyAttributeOptionSourceValidation(SXSSFWorkbook wb)
    {
        Sheet attributeSheet = wb.getSheet(ATTRIBUTE_IMPORT_SHEET);
        if (attributeSheet == null)
        {
            return;
        }
        Sheet hiddenSheet = wb.createSheet(ATTRIBUTE_OPTION_SOURCE_HIDDEN_SHEET);
        addOptionSourceRuleName(wb, hiddenSheet, 0, "TYPE_TEXT", "NONE");
        addOptionSourceRuleName(wb, hiddenSheet, 1, "TYPE_NUMBER", "NONE");
        addOptionSourceRuleName(wb, hiddenSheet, 2, "TYPE_BOOLEAN", "NONE");
        addOptionSourceRuleName(wb, hiddenSheet, 3, "TYPE_DATE", "NONE");
        addOptionSourceRuleName(wb, hiddenSheet, 4, "TYPE_SINGLE_SELECT", "ATTRIBUTE_OPTION", "SYS_DICT");
        addOptionSourceRuleName(wb, hiddenSheet, 5, "TYPE_MULTI_SELECT", "ATTRIBUTE_OPTION", "SYS_DICT");
        wb.setSheetHidden(wb.getSheetIndex(hiddenSheet), true);
        addExplicitListValidation(attributeSheet,
            new String[] { "TEXT", "NUMBER", "BOOLEAN", "SINGLE_SELECT", "MULTI_SELECT", "DATE" },
            "必填。先选择属性类型，再按类型联动填写选项来源。", DEFAULT_EXCEL_UTIL_LAST_VALIDATION_ROW + 1,
            ATTRIBUTE_TEMPLATE_LAST_DATA_ROW, ATTRIBUTE_TYPE_COLUMN_INDEX);
        addExplicitListValidation(attributeSheet, new String[] { "正常", "停用" }, "填写 正常 / 停用",
            DEFAULT_EXCEL_UTIL_LAST_VALIDATION_ROW + 1, ATTRIBUTE_TEMPLATE_LAST_DATA_ROW, STATUS_COLUMN_INDEX);
        DataValidationHelper helper = attributeSheet.getDataValidationHelper();
        for (int rowIndex = ATTRIBUTE_TEMPLATE_FIRST_DATA_ROW; rowIndex <= ATTRIBUTE_TEMPLATE_LAST_DATA_ROW; rowIndex++)
        {
            String formula = "INDIRECT(\"TYPE_\"&$" + CellReference.convertNumToColString(ATTRIBUTE_TYPE_COLUMN_INDEX)
                + "$" + (rowIndex + 1) + ")";
            DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
            CellRangeAddressList range = new CellRangeAddressList(rowIndex, rowIndex, OPTION_SOURCE_COLUMN_INDEX,
                OPTION_SOURCE_COLUMN_INDEX);
            DataValidation validation = helper.createValidation(constraint, range);
            validation.createPromptBox("选项来源",
                "先选择属性类型。本列会按属性类型限制：文本/数字/布尔/日期只能 NONE；单选/多选可选 ATTRIBUTE_OPTION 或 SYS_DICT。");
            validation.setShowPromptBox(true);
            if (validation instanceof XSSFDataValidation)
            {
                validation.setSuppressDropDownArrow(true);
                validation.setShowErrorBox(true);
            }
            else
            {
                validation.setSuppressDropDownArrow(false);
            }
            attributeSheet.addValidationData(validation);
        }
    }

    private void addExplicitListValidation(Sheet sheet, String[] values, String prompt, int firstRow, int lastRow,
        int columnIndex)
    {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList range = new CellRangeAddressList(firstRow, lastRow, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, range);
        validation.createPromptBox("", prompt);
        validation.setShowPromptBox(true);
        if (validation instanceof XSSFDataValidation)
        {
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
        }
        else
        {
            validation.setSuppressDropDownArrow(false);
        }
        sheet.addValidationData(validation);
    }

    private void addOptionSourceRuleName(SXSSFWorkbook wb, Sheet hiddenSheet, int rowIndex, String nameName,
        String... optionSources)
    {
        Row row = hiddenSheet.createRow(rowIndex);
        row.createCell(0).setCellValue(nameName);
        for (int index = 0; index < optionSources.length; index++)
        {
            row.createCell(index + 1).setCellValue(optionSources[index]);
        }
        Name name = wb.createName();
        name.setNameName(nameName);
        String startColumn = CellReference.convertNumToColString(1);
        String endColumn = CellReference.convertNumToColString(optionSources.length);
        int excelRowNum = rowIndex + 1;
        name.setRefersToFormula("'" + ATTRIBUTE_OPTION_SOURCE_HIDDEN_SHEET + "'!$" + startColumn + "$"
            + excelRowNum + ":$" + endColumn + "$" + excelRowNum);
    }
}
