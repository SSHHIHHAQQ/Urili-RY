package com.ruoyi.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.product.domain.ProductAttributeValue;
import com.ruoyi.product.domain.ProductImage;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;

/**
 * 商城商品 SPU/SKU Mapper。
 */
public interface ProductDistributionMapper
{
    List<ProductSpu> selectProductList(ProductSpu query);

    ProductSpu selectProductById(@Param("spuId") Long spuId);

    int insertSpu(ProductSpu spu);

    int updateSpu(ProductSpu spu);

    int updateSpuStatus(@Param("spuId") Long spuId, @Param("status") String status, @Param("updateBy") String updateBy);

    int countSystemSpuCode(@Param("systemSpuCode") String systemSpuCode);

    int countSellerSpuCode(@Param("sellerId") Long sellerId, @Param("sellerSpuCode") String sellerSpuCode,
        @Param("excludeSpuId") Long excludeSpuId);

    List<ProductSku> selectSkuListBySpuId(@Param("spuId") Long spuId);

    ProductSku selectSkuById(@Param("skuId") Long skuId);

    int insertSku(ProductSku sku);

    int updateSku(ProductSku sku);

    int updateSkuStatus(@Param("skuId") Long skuId, @Param("status") String status, @Param("updateBy") String updateBy);

    int deleteSkusBySpuId(@Param("spuId") Long spuId, @Param("updateBy") String updateBy);

    int countSystemSkuCode(@Param("systemSkuCode") String systemSkuCode);

    int countSellerSkuCode(@Param("sellerId") Long sellerId, @Param("sellerSkuCode") String sellerSkuCode,
        @Param("excludeSkuId") Long excludeSkuId);

    int countOnSaleSkusBySpuId(@Param("spuId") Long spuId);

    List<ProductAttributeValue> selectAttributeValuesBySpuId(@Param("spuId") Long spuId);

    int insertAttributeValue(ProductAttributeValue value);

    int deleteAttributeValuesBySpuId(@Param("spuId") Long spuId);

    List<ProductImage> selectImagesBySpuId(@Param("spuId") Long spuId);

    int insertImage(ProductImage image);

    int deleteImagesBySpuId(@Param("spuId") Long spuId);
}
