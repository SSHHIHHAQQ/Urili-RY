package com.ruoyi.buyer.controller;

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
 * Buyer terminal read-only product endpoints.
 */
@RestController
@RequestMapping("/buyer/product")
public class BuyerPortalProductSchemaController extends BaseController
{
    @Autowired
    private IProductPortalSchemaService productPortalSchemaService;

    @GetMapping("/categories")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:category:list")
    @PortalLog(terminal = "buyer", title = "Buyer product categories", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult categories()
    {
        PortalSessionContext.requireSession("buyer");
        return success(productPortalSchemaService.selectPortalCategories());
    }

    @GetMapping("/categories/{categoryId}/schema")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:schema:query")
    @PortalLog(terminal = "buyer", title = "Buyer product schema", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult schema(@PathVariable("categoryId") Long categoryId)
    {
        PortalSessionContext.requireSession("buyer");
        return success(productPortalSchemaService.selectPortalSchema(categoryId));
    }
}
