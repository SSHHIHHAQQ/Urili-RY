package com.ruoyi.seller.controller;

import java.util.List;
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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.seller.domain.SellerPortalProductSku;
import com.ruoyi.seller.service.ISellerPortalProductService;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Seller terminal own distribution product read-only endpoints.
 */
@RestController
@RequestMapping("/seller/product/distribution-products")
public class SellerPortalProductDistributionController extends BaseController
{
    @Autowired
    private ISellerPortalProductService sellerPortalProductService;

    @GetMapping("/list")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:distribution:list")
    @PortalLog(terminal = "seller", title = "卖家端我的商城商品列表", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public TableDataInfo list(ProductSpu query)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        startPage();
        return getDataTable(sellerPortalProductService.selectOwnProductList(session, query));
    }

    @GetMapping("/{spuId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:distribution:query")
    @PortalLog(terminal = "seller", title = "卖家端我的商城商品详情", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult get(@PathVariable("spuId") Long spuId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(sellerPortalProductService.selectOwnProductById(session, spuId));
    }

    @GetMapping("/{spuId}/skus")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:distribution:query")
    @PortalLog(terminal = "seller", title = "卖家端我的商城商品 SKU", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult skus(@PathVariable("spuId") Long spuId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        List<SellerPortalProductSku> skus = sellerPortalProductService.selectOwnSkuList(session, spuId);
        return success(skus);
    }
}
