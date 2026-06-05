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
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.importdata.ProductCategoryImportRow;
import com.ruoyi.product.domain.importdata.ProductImportResult;
import com.ruoyi.product.service.ProductConfigImportService;
import com.ruoyi.product.service.IProductConfigService;
import com.ruoyi.product.service.ProductImportTemplateService;

/**
 * 管理端商品分类配置。
 */
@RestController
@RequestMapping("/product/admin/categories")
public class AdminProductCategoryController extends BaseController
{
    @Autowired
    private IProductConfigService productConfigService;

    @Autowired
    private ProductConfigImportService productConfigImportService;

    @Autowired
    private ProductImportTemplateService productImportTemplateService;

    @PreAuthorize("@ss.hasPermi('product:category:list')")
    @GetMapping("/list")
    public AjaxResult list(ProductCategory query)
    {
        List<ProductCategory> list = productConfigService.selectCategoryList(query);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('product:category:list')")
    @GetMapping("/children")
    public AjaxResult children(ProductCategory query)
    {
        if (query.getParentId() == null)
        {
            query.setParentId(0L);
        }
        return success(productConfigService.selectCategoryList(query));
    }

    @PreAuthorize("@ss.hasPermi('product:category:list')")
    @GetMapping("/search")
    public TableDataInfo search(ProductCategory query)
    {
        startPage();
        List<ProductCategory> list = productConfigService.selectCategoryList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('product:category:list')")
    @GetMapping("/options")
    public AjaxResult options(ProductCategory query)
    {
        startPage();
        return success(productConfigService.selectCategoryList(query));
    }

    @PreAuthorize("@ss.hasPermi('product:category:list')")
    @GetMapping("/path/{categoryId}")
    public AjaxResult path(@PathVariable("categoryId") Long categoryId)
    {
        return success(productConfigService.selectCategoryPath(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('product:category:query')")
    @GetMapping("/{categoryId}")
    public AjaxResult get(@PathVariable("categoryId") Long categoryId)
    {
        return success(productConfigService.selectCategoryById(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('product:category:add')")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ProductCategory category)
    {
        return toAjax(productConfigService.insertCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('product:category:edit')")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PutMapping("/{categoryId}")
    public AjaxResult edit(@PathVariable("categoryId") Long categoryId,
        @Validated @RequestBody ProductCategory category)
    {
        category.setCategoryId(categoryId);
        return toAjax(productConfigService.updateCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('product:category:remove')")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryId}")
    public AjaxResult remove(@PathVariable("categoryId") Long categoryId)
    {
        return toAjax(productConfigService.deleteCategoryById(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('product:category:add')")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        productImportTemplateService.exportCategoryTemplate(response);
    }

    @PreAuthorize("@ss.hasPermi('product:category:add')")
    @PostMapping("/importPreview")
    public AjaxResult importPreview(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductCategoryImportRow> util = new ExcelUtil<>(ProductCategoryImportRow.class);
        ProductImportResult result = productConfigImportService.previewCategories(util.importExcel(file.getInputStream()),
            updateSupport);
        return importResult(result, "商品分类导入校验通过");
    }

    @PreAuthorize("@ss.hasPermi('product:category:add')")
    @Log(title = "商品分类", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ProductCategoryImportRow> util = new ExcelUtil<>(ProductCategoryImportRow.class);
        ProductImportResult result = productConfigImportService.importCategories(util.importExcel(file.getInputStream()),
            updateSupport);
        return importResult(result, "商品分类导入完成");
    }

    private AjaxResult importResult(ProductImportResult result, String successText)
    {
        if (result.isPassed())
        {
            return AjaxResult.success(successText, result);
        }
        return AjaxResult.warn("商品分类导入校验未通过", result);
    }
}
