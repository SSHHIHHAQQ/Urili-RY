package com.ruoyi.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import com.ruoyi.product.domain.ProductAttributeValue;
import com.ruoyi.product.domain.ProductImage;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSkuSourceBinding;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.domain.ProductSpuWarehouse;

/**
 * 商城商品 SPU/SKU Mapper。
 */
public interface ProductDistributionMapper
{
    List<ProductSpu> selectProductList(ProductSpu query);

    List<ProductSpu> selectOnSaleProductList(ProductSpu query);

    ProductSpu selectProductById(@Param("spuId") Long spuId);

    ProductSpu selectOnSaleProductById(@Param("spuId") Long spuId);

    int insertSpu(ProductSpu spu);

    int updateSpu(ProductSpu spu);

    int updateSpuStatus(@Param("spuId") Long spuId, @Param("status") String status, @Param("updateBy") String updateBy);

    int updateSpuControlStatus(@Param("spuId") Long spuId, @Param("controlStatus") String controlStatus,
        @Param("reason") String reason, @Param("updateBy") String updateBy);

    int countSystemSpuCode(@Param("systemSpuCode") String systemSpuCode);

    int countSellerSpuCode(@Param("sellerId") Long sellerId, @Param("sellerSpuCode") String sellerSpuCode,
        @Param("excludeSpuId") Long excludeSpuId);

    List<ProductSku> selectSkuPageList(ProductSku query);

    List<ProductSku> selectSkuListBySpuId(@Param("spuId") Long spuId);

    List<ProductSku> selectOnSaleSkuListBySpuId(@Param("spuId") Long spuId);

    ProductSku selectSkuById(@Param("skuId") Long skuId);

    int insertSku(ProductSku sku);

    int updateSku(ProductSku sku);

    int updateSkuStatus(@Param("skuId") Long skuId, @Param("status") String status, @Param("updateBy") String updateBy);

    int updateSkuControlStatus(@Param("skuId") Long skuId, @Param("controlStatus") String controlStatus,
        @Param("reason") String reason, @Param("updateBy") String updateBy);

    int updateSkuSalePrice(@Param("skuId") Long skuId, @Param("salePrice") BigDecimal salePrice,
        @Param("updateBy") String updateBy);

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

    List<ProductSpuWarehouse> selectWarehousesBySpuId(@Param("spuId") Long spuId);

    int insertSpuWarehouse(ProductSpuWarehouse warehouse);

    int deleteWarehousesBySpuId(@Param("spuId") Long spuId);

    ProductSkuSourceBinding selectSourceBindingSnapshot(@Param("sourceDimensionGroupKey") String sourceDimensionGroupKey);

    ProductSkuSourceBinding selectActiveSourceBindingBySkuId(@Param("skuId") Long skuId);

    ProductSkuSourceBinding selectActiveSourceBindingBySourceSkuGroupKey(@Param("sourceSkuGroupKey") String sourceSkuGroupKey);

    List<ProductSpuWarehouse> selectOfficialWarehousesBySourceDimensionGroup(
        @Param("sourceDimensionGroupKey") String sourceDimensionGroupKey);

    List<String> selectSourceConnectionCodesByDimensionGroup(
        @Param("sourceDimensionGroupKey") String sourceDimensionGroupKey);

    List<String> selectUpstreamSkuPairingConnectionCodesBySystemSkuAndMasterSku(@Param("systemSku") String systemSku,
        @Param("masterSku") String masterSku);

    int insertSourceBinding(ProductSkuSourceBinding binding);

    int updateActiveSourceBinding(ProductSkuSourceBinding binding);

    int markSourceBindingReplaced(@Param("bindingId") Long bindingId, @Param("reason") String reason,
        @Param("updateBy") String updateBy);

    int releaseActiveSourceBindingBySkuId(@Param("skuId") Long skuId, @Param("reason") String reason,
        @Param("updateBy") String updateBy);

    int lockActiveSourceBindingBySkuId(@Param("skuId") Long skuId, @Param("lockedBy") String lockedBy);

    int deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes(@Param("systemSku") String systemSku,
        @Param("connectionCodes") List<String> connectionCodes);

    int upsertUpstreamSkuPairingsForBinding(ProductSkuSourceBinding binding);
}
