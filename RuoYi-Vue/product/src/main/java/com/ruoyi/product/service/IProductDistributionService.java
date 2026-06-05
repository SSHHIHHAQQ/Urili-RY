package com.ruoyi.product.service;

import java.util.List;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.domain.ProductSku;

/**
 * 商城商品 SPU/SKU 服务。
 */
public interface IProductDistributionService
{
    List<ProductSpu> selectProductList(ProductSpu query);

    List<ProductSpu> selectOnSaleProductList(ProductSpu query);

    ProductSpu selectProductById(Long spuId);

    ProductSpu selectOnSaleProductById(Long spuId);

    int insertProduct(ProductSpu product);

    int updateProduct(ProductSpu product);

    int updateSpuStatus(Long spuId, String status);

    int updateSkuStatus(Long spuId, Long skuId, String status);

    List<ProductSku> selectSkuList(Long spuId);

    List<ProductSku> selectOnSaleSkuList(Long spuId);
}
