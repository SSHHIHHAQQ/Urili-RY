package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.github.pagehelper.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductCenterProduct;
import com.ruoyi.product.domain.ProductCenterQuery;
import com.ruoyi.product.domain.ProductCenterSku;
import com.ruoyi.product.domain.ProductCenterWarehouse;
import com.ruoyi.product.domain.ProductDistributionOperationLog;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSkuSalePriceUpdateRequest;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.domain.ProductSpuWarehouse;
import com.ruoyi.product.service.IProductDistributionService;

public class ProductCenterServiceImplTest
{
    @Test
    public void selectProductListUsesOnSaleQueryAndMapsBuyerVisibleCodes()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.onSaleProductListResult.add(onSaleProduct(1L));
        ProductCenterServiceImpl service = service(productService);
        ProductCenterQuery query = new ProductCenterQuery();
        query.setKeyword("chair");
        query.setProductName("椅子");
        query.setProductNameEn("Chair");
        query.setSystemSpuCode("SPU-001");
        query.setSystemSkuCode("SKU-001");
        query.setCategoryId(7L);

        List<ProductCenterProduct> result = service.selectProductList(query);

        assertEquals("chair", productService.onSaleProductListQuery.getKeyword());
        assertEquals("椅子", productService.onSaleProductListQuery.getProductName());
        assertEquals("Chair", productService.onSaleProductListQuery.getProductNameEn());
        assertEquals("SPU-001", productService.onSaleProductListQuery.getSystemSpuCode());
        assertEquals("SKU-001", productService.onSaleProductListQuery.getSystemSkuCode());
        assertEquals(Long.valueOf(7L), productService.onSaleProductListQuery.getCategoryId());
        assertEquals(1, result.size());
        assertEquals("SPU-INTERNAL", result.get(0).getSystemSpuCode());
        assertEquals(List.of("SKU-INTERNAL"), result.get(0).getVisibleSystemSkuCodes());
    }

    @Test
    public void selectProductListPreservesPageHelperMetadata()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        Page<ProductSpu> page = new Page<>(2, 20, true);
        page.setTotal(21);
        page.setPages(2);
        page.add(onSaleProduct(1L));
        productService.onSaleProductListResult = page;
        ProductCenterServiceImpl service = service(productService);

        List<ProductCenterProduct> result = service.selectProductList(new ProductCenterQuery());

        Page<?> resultPage = (Page<?>) result;
        assertEquals(1, result.size());
        assertEquals(2, resultPage.getPageNum());
        assertEquals(20, resultPage.getPageSize());
        assertEquals(21L, resultPage.getTotal());
        assertEquals(2, resultPage.getPages());
    }

    @Test
    public void selectProductByIdReturnsBuyerFacingDtoWithoutSellerOrCostFields()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        ProductSpu product = onSaleProduct(1L);
        product.setSellerId(99L);
        product.setSellerSpuCode("SELLER-SPU");
        product.setSupplyPriceMin(new BigDecimal("10.00"));
        product.setSalePriceMin(new BigDecimal("20.00"));
        product.setSalePriceMax(new BigDecimal("25.00"));
        product.setWarehouses(List.of(warehouse(3L, "WH-001", "美西仓", "official", "USD")));
        productService.onSaleProductByIdResult = product;
        ProductCenterServiceImpl service = service(productService);

        ProductCenterProduct result = service.selectProductById(1L);

        assertEquals(Long.valueOf(1L), result.getSpuId());
        assertEquals("椅子", result.getProductName());
        assertEquals("SPU-INTERNAL", result.getSystemSpuCode());
        assertEquals(new BigDecimal("20.00"), result.getSalePriceMin());
        assertEquals(new BigDecimal("25.00"), result.getSalePriceMax());
        assertEquals(1, result.getSkus().size());
        assertEquals("SKU-INTERNAL", result.getSkus().get(0).getSystemSkuCode());
        assertEquals(new BigDecimal("20.00"), result.getSkus().get(0).getSalePrice());
        assertEquals(1, result.getWarehouses().size());
        assertEquals("美西仓", result.getWarehouses().get(0).getWarehouseName());
        assertEquals("平台官方仓发货", result.getWarehouses().get(0).getDeliveryText());
        assertFalse(hasGetter(ProductCenterProduct.class, "getSellerId"));
        assertFalse(hasGetter(ProductCenterProduct.class, "getSellerSpuCode"));
        assertFalse(hasGetter(ProductCenterProduct.class, "getSupplyPriceMin"));
        assertFalse(hasGetter(ProductCenterProduct.class, "getControlReason"));
        assertFalse(hasGetter(ProductCenterWarehouse.class, "getStockText"));
        assertFalse(hasGetter(ProductCenterSku.class, "getSellerId"));
        assertFalse(hasGetter(ProductCenterSku.class, "getSellerSkuCode"));
        assertFalse(hasGetter(ProductCenterSku.class, "getSupplyPrice"));
    }

    @Test
    public void selectProductByIdRejectsProductWithoutOnSaleSku()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        ProductSpu product = onSaleProduct(1L);
        product.setSkus(List.of(sku(2L, 1L, "READY", "20.00", "USD")));
        productService.onSaleProductByIdResult = product;
        ProductCenterServiceImpl service = service(productService);

        try
        {
            service.selectProductById(1L);
        }
        catch (ServiceException e)
        {
            assertEquals("商品不存在", e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSkuListVerifiesProductVisibilityBeforeReadingSkus()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.onSaleProductByIdResult = onSaleProduct(1L);
        productService.onSaleSkuListResult.add(sku(2L, 1L, "ON_SALE", "20.00", "USD"));
        ProductCenterServiceImpl service = service(productService);

        List<ProductCenterSku> result = service.selectSkuList(1L);

        assertEquals(Long.valueOf(1L), productService.onSaleProductByIdArg);
        assertEquals(Long.valueOf(1L), productService.onSaleSkuListSpuId);
        assertEquals(1, result.size());
        assertEquals("ON_SALE", result.get(0).getSkuStatus());
    }

    private ProductCenterServiceImpl service(IProductDistributionService productService)
    {
        ProductCenterServiceImpl service = new ProductCenterServiceImpl();
        setField(service, "productDistributionService", productService);
        return service;
    }

    private ProductSpu onSaleProduct(Long spuId)
    {
        ProductSpu product = new ProductSpu();
        product.setSpuId(spuId);
        product.setSystemSpuCode("SPU-INTERNAL");
        product.setSpuStatus("ON_SALE");
        product.setCategoryId(7L);
        product.setCategoryName("家具");
        product.setProductName("椅子");
        product.setProductNameEn("Chair");
        product.setCurrencySummary("USD");
        product.setAvailableStock(8L);
        product.setWarehouseCount(1);
        product.setSkus(List.of(sku(2L, spuId, "ON_SALE", "20.00", "USD")));
        return product;
    }

    private ProductSku sku(Long skuId, Long spuId, String status, String salePrice, String currencyCode)
    {
        ProductSku sku = new ProductSku();
        sku.setSkuId(skuId);
        sku.setSpuId(spuId);
        sku.setSellerId(99L);
        sku.setSystemSkuCode("SKU-INTERNAL");
        sku.setSellerSkuCode("SELLER-SKU");
        sku.setSkuStatus(status);
        sku.setSalePrice(new BigDecimal(salePrice));
        sku.setSupplyPrice(new BigDecimal("10.00"));
        sku.setCurrencyCode(currencyCode);
        return sku;
    }

    private ProductSpuWarehouse warehouse(Long warehouseId, String code, String name, String kind, String currency)
    {
        ProductSpuWarehouse warehouse = new ProductSpuWarehouse();
        warehouse.setId(warehouseId);
        warehouse.setWarehouseId(warehouseId);
        warehouse.setWarehouseCode(code);
        warehouse.setWarehouseName(name);
        warehouse.setWarehouseKind(kind);
        warehouse.setSettlementCurrency(currency);
        return warehouse;
    }

    private boolean hasGetter(Class<?> type, String getterName)
    {
        try
        {
            type.getMethod(getterName);
            return true;
        }
        catch (NoSuchMethodException e)
        {
            return false;
        }
    }

    private void setField(Object target, String fieldName, Object value)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }

    private static class RecordingProductDistributionService implements IProductDistributionService
    {
        private ProductSpu onSaleProductListQuery;
        private List<ProductSpu> onSaleProductListResult = new ArrayList<>();
        private Long onSaleProductByIdArg;
        private ProductSpu onSaleProductByIdResult;
        private Long onSaleSkuListSpuId;
        private List<ProductSku> onSaleSkuListResult = new ArrayList<>();

        @Override
        public List<ProductSpu> selectOnSaleProductList(ProductSpu query)
        {
            onSaleProductListQuery = query;
            return onSaleProductListResult;
        }

        @Override
        public ProductSpu selectOnSaleProductById(Long spuId)
        {
            onSaleProductByIdArg = spuId;
            return onSaleProductByIdResult;
        }

        @Override
        public List<ProductSku> selectOnSaleSkuList(Long spuId)
        {
            onSaleSkuListSpuId = spuId;
            return onSaleSkuListResult;
        }

        @Override public List<ProductSpu> selectProductList(ProductSpu query) { throw new UnsupportedOperationException(); }
        @Override public ProductSpu selectProductById(Long spuId) { throw new UnsupportedOperationException(); }
        @Override public ProductSpu selectProductById(Long spuId, Long sellerId) { throw new UnsupportedOperationException(); }
        @Override public int insertProduct(ProductSpu product) { throw new UnsupportedOperationException(); }
        @Override public int updateProduct(ProductSpu product) { throw new UnsupportedOperationException(); }
        @Override public int deleteDraftProduct(Long spuId) { throw new UnsupportedOperationException(); }
        @Override public ProductSpu prepareReviewedProductUpdate(ProductSpu product) { throw new UnsupportedOperationException(); }
        @Override public int applyReviewedProductUpdate(ProductSpu product) { throw new UnsupportedOperationException(); }
        @Override public int updateSpuStatus(Long spuId, String status, String reason) { throw new UnsupportedOperationException(); }
        @Override public int updateSkuStatus(Long spuId, Long skuId, String status, String reason) { throw new UnsupportedOperationException(); }
        @Override public int batchUpdateSpuStatus(List<Long> spuIds, String status, boolean syncSkuStatus, String reason) { throw new UnsupportedOperationException(); }
        @Override public int batchUpdateSkuStatus(List<Long> skuIds, String status, String reason) { throw new UnsupportedOperationException(); }
        @Override public int batchUpdateSpuControlStatus(List<Long> spuIds, String controlStatus, String reason) { throw new UnsupportedOperationException(); }
        @Override public int batchUpdateSkuControlStatus(List<Long> skuIds, String controlStatus, String reason) { throw new UnsupportedOperationException(); }
        @Override public int batchUpdateSkuSalePrice(ProductSkuSalePriceUpdateRequest request) { throw new UnsupportedOperationException(); }
        @Override public List<ProductDistributionOperationLog> selectOperationLogList(ProductDistributionOperationLog query) { throw new UnsupportedOperationException(); }
        @Override public List<ProductSku> selectSkuPageList(ProductSku query) { throw new UnsupportedOperationException(); }
        @Override public List<ProductSku> selectSkuList(Long spuId) { throw new UnsupportedOperationException(); }
        @Override public List<ProductSku> selectSkuList(Long spuId, Long sellerId) { throw new UnsupportedOperationException(); }
    }
}
