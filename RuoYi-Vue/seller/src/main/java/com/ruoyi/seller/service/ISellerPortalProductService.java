package com.ruoyi.seller.service;

import java.util.List;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.seller.domain.SellerPortalProduct;
import com.ruoyi.seller.domain.SellerPortalProductSku;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * Seller terminal own product read-only service.
 */
public interface ISellerPortalProductService
{
    List<SellerPortalProduct> selectOwnProductList(PortalLoginSession session, ProductSpu query);

    SellerPortalProduct selectOwnProductById(PortalLoginSession session, Long spuId);

    List<SellerPortalProductSku> selectOwnSkuList(PortalLoginSession session, Long spuId);
}
