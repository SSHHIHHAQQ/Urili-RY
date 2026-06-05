package com.ruoyi.buyer.service;

import java.util.List;
import com.ruoyi.buyer.domain.BuyerPortalProduct;
import com.ruoyi.buyer.domain.BuyerPortalProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * Buyer terminal product browsing facade.
 */
public interface IBuyerPortalProductService
{
    List<BuyerPortalProduct> selectVisibleProductList(PortalLoginSession session, ProductSpu query);

    BuyerPortalProduct selectVisibleProductById(PortalLoginSession session, Long spuId);

    List<BuyerPortalProductSku> selectVisibleSkuList(PortalLoginSession session, Long spuId);
}
