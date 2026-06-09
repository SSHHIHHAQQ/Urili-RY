package com.ruoyi.product.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.product.domain.ProductCenterQuery;
import com.ruoyi.product.domain.ProductCenterSku;
import com.ruoyi.product.service.IProductCenterService;

/**
 * Admin-side buyer-facing product center.
 */
@RestController
@RequestMapping("/product/admin/product-center")
public class AdminProductCenterController extends BaseController
{
    @Autowired
    private IProductCenterService productCenterService;

    @PreAuthorize("@ss.hasPermi('product:center:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductCenterQuery query)
    {
        startPage();
        return getDataTable(productCenterService.selectProductList(query));
    }

    @PreAuthorize("@ss.hasPermi('product:center:query')")
    @GetMapping("/{spuId}")
    public AjaxResult get(@PathVariable("spuId") Long spuId)
    {
        return success(productCenterService.selectProductById(spuId));
    }

    @PreAuthorize("@ss.hasPermi('product:center:query')")
    @GetMapping("/{spuId}/skus")
    public AjaxResult skus(@PathVariable("spuId") Long spuId)
    {
        List<ProductCenterSku> skus = productCenterService.selectSkuList(spuId);
        return success(skus);
    }
}
