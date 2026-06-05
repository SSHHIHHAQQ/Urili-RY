package com.ruoyi.buyer.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.buyer.domain.BuyerPortalProductSku;
import com.ruoyi.buyer.service.IBuyerPortalProductService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Buyer terminal distribution product browsing endpoints.
 */
@RestController
@RequestMapping("/buyer/product/distribution-products")
public class BuyerPortalProductDistributionController extends BaseController
{
    @Autowired
    private IBuyerPortalProductService buyerPortalProductService;

    @GetMapping("/list")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:distribution:list")
    @PortalLog(terminal = "buyer", title = "买家端商城商品列表", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public TableDataInfo list(ProductSpu query)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        startPage();
        return getDataTable(buyerPortalProductService.selectVisibleProductList(session, query));
    }

    @GetMapping("/{spuId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:distribution:query")
    @PortalLog(terminal = "buyer", title = "买家端商城商品详情", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult get(@PathVariable("spuId") Long spuId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(buyerPortalProductService.selectVisibleProductById(session, spuId));
    }

    @GetMapping("/{spuId}/skus")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:distribution:query")
    @PortalLog(terminal = "buyer", title = "买家端商城商品 SKU", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    public AjaxResult skus(@PathVariable("spuId") Long spuId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        List<BuyerPortalProductSku> skus = buyerPortalProductService.selectVisibleSkuList(session, spuId);
        return success(skus);
    }
}
