package com.ruoyi.product.service;

import java.util.List;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;

/**
 * Read-only product distribution surface for seller/buyer portals.
 */
public interface IProductPortalDistributionService
{
    List<ProductSpu> selectSellerProductList(ProductSpu query);

    ProductSpu selectSellerProductById(Long spuId, Long sellerId);

    List<ProductSku> selectSellerSkuList(Long spuId, Long sellerId);

    List<ProductSpu> selectBuyerVisibleProductList(ProductSpu query);

    ProductSpu selectBuyerVisibleProductById(Long spuId);

    List<ProductSku> selectBuyerVisibleSkuList(Long spuId);
}
