package com.ruoyi.seller.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.product.service.IProductPortalSchemaService;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Seller terminal read-only product endpoints.
 */
@RestController
@RequestMapping("/seller/product")
public class SellerPortalProductSchemaController extends BaseController
{
    @Autowired
    private IProductPortalSchemaService productPortalSchemaService;

    @GetMapping("/categories")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:category:list")
    @PortalLog(terminal = "seller", title = "Seller product categories", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult categories()
    {
        PortalSessionContext.requireSession("seller");
        return success(productPortalSchemaService.selectPortalCategories());
    }

    @GetMapping("/categories/{categoryId}/schema")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:schema:query")
    @PortalLog(terminal = "seller", title = "Seller product schema", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult schema(@PathVariable("categoryId") Long categoryId)
    {
        PortalSessionContext.requireSession("seller");
        return success(productPortalSchemaService.selectPortalSchema(categoryId));
    }
}
