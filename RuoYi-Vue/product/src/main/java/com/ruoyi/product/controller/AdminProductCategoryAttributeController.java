package com.ruoyi.product.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.product.domain.ProductCategoryAttribute;
import com.ruoyi.product.service.IProductConfigService;

/**
 * 管理端类目属性配置。
 */
@RestController
@RequestMapping("/product/admin/category-attributes")
public class AdminProductCategoryAttributeController extends BaseController
{
    @Autowired
    private IProductConfigService productConfigService;

    @PreAuthorize("@ss.hasPermi('product:categoryAttribute:list')")
    @GetMapping("/list/{categoryId}")
    public AjaxResult list(@PathVariable("categoryId") Long categoryId)
    {
        return success(productConfigService.selectDirectCategoryAttributeList(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('product:categoryAttribute:preview')")
    @GetMapping("/schema/{categoryId}")
    public AjaxResult schema(@PathVariable("categoryId") Long categoryId)
    {
        return success(productConfigService.previewCategorySchema(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('product:categoryAttribute:edit')")
    @Log(title = "类目属性配置", businessType = BusinessType.UPDATE)
    @PostMapping
    public AjaxResult save(@Validated @RequestBody ProductCategoryAttribute categoryAttribute)
    {
        return toAjax(productConfigService.saveCategoryAttribute(categoryAttribute));
    }

    @PreAuthorize("@ss.hasPermi('product:categoryAttribute:edit')")
    @Log(title = "类目属性配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryAttributeId}")
    public AjaxResult remove(@PathVariable("categoryAttributeId") Long categoryAttributeId)
    {
        return toAjax(productConfigService.deleteCategoryAttributeById(categoryAttributeId));
    }
}
