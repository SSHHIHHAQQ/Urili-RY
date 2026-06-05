package com.ruoyi.product.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.product.domain.ProductAttribute;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.importdata.ProductAttributeImportRow;
import com.ruoyi.product.domain.importdata.ProductAttributeOptionImportRow;
import com.ruoyi.product.domain.importdata.ProductImportResult;
import com.ruoyi.product.service.ProductConfigImportService;
import com.ruoyi.product.service.IProductConfigService;
import com.ruoyi.product.service.ProductImportTemplateService;

/**
 * 管理端商品属性配置。
 */
@RestController
@RequestMapping("/product/admin/attributes")
public class AdminProductAttributeController extends BaseController
{
    @Autowired
    private IProductConfigService productConfigService;

    @Autowired
    private ProductConfigImportService productConfigImportService;

    @Autowired
    private ProductImportTemplateService productImportTemplateService;

    @PreAuthorize("@ss.hasPermi('product:attribute:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductAttribute query)
    {
        startPage();
        List<ProductAttribute> list = productConfigService.selectAttributeList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:list')")
    @GetMapping("/options")
    public AjaxResult attributeOptions(ProductAttribute query)
    {
        startPage();
        return success(productConfigService.selectEnabledAttributeList(query));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:query')")
    @GetMapping("/{attributeId}")
    public AjaxResult get(@PathVariable("attributeId") Long attributeId)
    {
        return success(productConfigService.selectAttributeById(attributeId));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:add')")
    @Log(title = "商品属性", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ProductAttribute attribute)
    {
        return toAjax(productConfigService.insertAttribute(attribute));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:edit')")
    @Log(title = "商品属性", businessType = BusinessType.UPDATE)
    @PutMapping("/{attributeId}")
    public AjaxResult edit(@PathVariable("attributeId") Long attributeId,
        @Validated @RequestBody ProductAttribute attribute)
    {
        attribute.setAttributeId(attributeId);
        return toAjax(productConfigService.updateAttribute(attribute));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:remove')")
    @Log(title = "商品属性", businessType = BusinessType.DELETE)
    @DeleteMapping("/{attributeId}")
    public AjaxResult remove(@PathVariable("attributeId") Long attributeId)
    {
        return toAjax(productConfigService.deleteAttributeById(attributeId));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:query')")
    @GetMapping("/{attributeId}/options")
    public AjaxResult optionList(@PathVariable("attributeId") Long attributeId)
    {
        return success(productConfigService.selectOptionList(attributeId));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:edit')")
    @Log(title = "商品属性选项", businessType = BusinessType.INSERT)
    @PostMapping("/{attributeId}/options")
    public AjaxResult addOption(@PathVariable("attributeId") Long attributeId,
        @Validated @RequestBody ProductAttributeOption option)
    {
        return toAjax(productConfigService.insertOption(attributeId, option));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:edit')")
    @Log(title = "商品属性选项", businessType = BusinessType.UPDATE)
    @PutMapping("/{attributeId}/options/{optionId}")
    public AjaxResult editOption(@PathVariable("attributeId") Long attributeId,
        @PathVariable("optionId") Long optionId, @Validated @RequestBody ProductAttributeOption option)
    {
        return toAjax(productConfigService.updateOption(attributeId, optionId, option));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:edit')")
    @Log(title = "商品属性选项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{attributeId}/options/{optionId}")
    public AjaxResult removeOption(@PathVariable("attributeId") Long attributeId,
        @PathVariable("optionId") Long optionId)
    {
        return toAjax(productConfigService.deleteOptionById(attributeId, optionId));
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:add')")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        productImportTemplateService.exportAttributeTemplate(response);
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:add')")
    @PostMapping("/importPreview")
    public AjaxResult importPreview(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductAttributeImportRow> util = new ExcelUtil<>(ProductAttributeImportRow.class);
        ProductImportResult result = productConfigImportService.previewAttributes(util.importExcel(file.getInputStream()),
            updateSupport);
        return importResult(result, "商品属性导入校验通过", "商品属性导入校验未通过");
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:add')")
    @Log(title = "商品属性", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductAttributeImportRow> util = new ExcelUtil<>(ProductAttributeImportRow.class);
        ProductImportResult result = productConfigImportService.importAttributes(util.importExcel(file.getInputStream()),
            updateSupport);
        return importResult(result, "商品属性导入完成", "商品属性导入校验未通过");
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:edit')")
    @PostMapping("/options/importTemplate")
    public void optionImportTemplate(HttpServletResponse response)
    {
        productImportTemplateService.exportAttributeOptionTemplate(response);
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:edit')")
    @PostMapping("/options/importPreview")
    public AjaxResult optionImportPreview(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductAttributeOptionImportRow> util = new ExcelUtil<>(ProductAttributeOptionImportRow.class);
        ProductImportResult result = productConfigImportService.previewOptions(util.importExcel(file.getInputStream()),
            updateSupport);
        return importResult(result, "商品属性选项导入校验通过", "商品属性选项导入校验未通过");
    }

    @PreAuthorize("@ss.hasPermi('product:attribute:edit')")
    @Log(title = "商品属性选项", businessType = BusinessType.IMPORT)
    @PostMapping("/options/importData")
    public AjaxResult optionImportData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductAttributeOptionImportRow> util = new ExcelUtil<>(ProductAttributeOptionImportRow.class);
        ProductImportResult result = productConfigImportService.importOptions(util.importExcel(file.getInputStream()),
            updateSupport);
        return importResult(result, "商品属性选项导入完成", "商品属性选项导入校验未通过");
    }

    private AjaxResult importResult(ProductImportResult result, String successText, String failureText)
    {
        if (result.isPassed())
        {
            return AjaxResult.success(successText, result);
        }
        return AjaxResult.warn(failureText, result);
    }
}
