package com.ruoyi.product.controller;

import java.util.List;
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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.service.IProductConfigService;

/**
 * 管理端商品分类配置。
 */
@RestController
@RequestMapping("/product/admin/categories")
public class AdminProductCategoryController extends BaseController
{
    @Autowired
    private IProductConfigService productConfigService;

    @PreAuthorize("@ss.hasPermi('product:category:list')")
    @GetMapping("/list")
    public AjaxResult list(ProductCategory query)
    {
        List<ProductCategory> list = productConfigService.selectCategoryList(query);
        return success(list);
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
}
