package com.ruoyi.product.service;

import java.util.List;
import com.ruoyi.product.domain.ProductCenterProduct;
import com.ruoyi.product.domain.ProductCenterQuery;
import com.ruoyi.product.domain.ProductCenterSku;

/**
 * Buyer-facing product center service.
 */
public interface IProductCenterService
{
    List<ProductCenterProduct> selectProductList(ProductCenterQuery query);

    ProductCenterProduct selectProductById(Long spuId);

    List<ProductCenterSku> selectSkuList(Long spuId);
}
