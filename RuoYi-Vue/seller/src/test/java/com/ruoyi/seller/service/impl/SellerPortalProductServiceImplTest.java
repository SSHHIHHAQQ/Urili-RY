package com.ruoyi.seller.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.github.pagehelper.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.service.IProductDistributionService;
import com.ruoyi.seller.domain.SellerPortalProduct;
import com.ruoyi.seller.domain.SellerPortalProductSku;
import com.ruoyi.system.domain.PortalLoginSession;

public class SellerPortalProductServiceImplTest
{
    @Test
    public void selectOwnProductListUsesSessionScopeAndIgnoresClientSellerId()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.productListResult.add(product(1L, 11L, "SELLER-SPU-1"));
        SellerPortalProductServiceImpl service = service(productService);
        PortalLoginSession session = session(11L, 22L);
        ProductSpu request = new ProductSpu();
        request.setSellerId(99L);
        request.setSystemSpuCode("SPU-INTERNAL");
        request.setSystemSkuCode("SKU-INTERNAL");
        request.setSellerSpuCode("SELLER-SPU-1");
        request.setSellerSkuCode("SELLER-SKU-1");
        request.setProductName("桌子");
        request.setProductNameEn("Table");
        request.setCategoryId(7L);
        request.setSpuStatus("ON_SALE");
        request.setKeyword("table");
        request.setSourceType("ADMIN_MANUAL");

        List<SellerPortalProduct> result = service.selectOwnProductList(session, request);

        assertSame(productService.productListResult, productService.returnedProductListSource);
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(11L), productService.productListQuery.getSellerId());
        assertEquals("SELLER-SPU-1", productService.productListQuery.getSellerSpuCode());
        assertEquals("SELLER-SKU-1", productService.productListQuery.getSellerSkuCode());
        assertEquals("桌子", productService.productListQuery.getProductName());
        assertEquals("Table", productService.productListQuery.getProductNameEn());
        assertEquals(Long.valueOf(7L), productService.productListQuery.getCategoryId());
        assertEquals("ON_SALE", productService.productListQuery.getSpuStatus());
        assertEquals("table", productService.productListQuery.getKeyword());
        assertEquals(null, productService.productListQuery.getSystemSpuCode());
        assertEquals(null, productService.productListQuery.getSystemSkuCode());
        assertEquals(null, productService.productListQuery.getSourceType());
        assertEquals(Long.valueOf(99L), request.getSellerId());
    }

    @Test
    public void selectOwnProductListPreservesPageHelperMetadata()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        Page<ProductSpu> page = new Page<>(2, 10, true);
        page.setTotal(25);
        page.setPages(3);
        page.add(product(1L, 11L, "SELLER-SPU-1"));
        productService.productListResult = page;
        SellerPortalProductServiceImpl service = service(productService);

        List<SellerPortalProduct> result = service.selectOwnProductList(session(11L, 22L), new ProductSpu());

        Page<?> resultPage = (Page<?>) result;
        assertEquals(1, result.size());
        assertEquals(2, resultPage.getPageNum());
        assertEquals(10, resultPage.getPageSize());
        assertEquals(25L, resultPage.getTotal());
        assertEquals(3, resultPage.getPages());
    }

    @Test
    public void selectOwnProductByIdRejectsProductFromAnotherSeller()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.productByIdResult = product(1L, 99L, "SELLER-SPU-OTHER");
        SellerPortalProductServiceImpl service = service(productService);

        assertServiceException(() -> service.selectOwnProductById(session(11L, 22L), 1L));
    }

    @Test
    public void selectOwnProductByIdReturnsPortalDtoWithoutAdminScopeFields()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        ProductSpu product = product(1L, 11L, "SELLER-SPU-1");
        product.setSystemSpuCode("SPU-INTERNAL");
        product.setSellerName("Seller A");
        product.setProductName("桌子");
        product.setProductNameEn("Table");
        product.setSupplyPriceMin(new BigDecimal("10.00"));
        product.setSalePriceMin(new BigDecimal("20.00"));
        product.setSkus(List.of(sku(2L, 1L, 11L, "SELLER-SKU-1")));
        productService.productByIdResult = product;
        SellerPortalProductServiceImpl service = service(productService);

        SellerPortalProduct result = service.selectOwnProductById(session(11L, 22L), 1L);

        assertEquals(Long.valueOf(1L), result.getSpuId());
        assertEquals("SELLER-SPU-1", result.getSellerSpuCode());
        assertEquals("桌子", result.getProductName());
        assertEquals(new BigDecimal("10.00"), result.getSupplyPriceMin());
        assertEquals(new BigDecimal("20.00"), result.getSalePriceMin());
        assertEquals(1, result.getSkus().size());
        assertEquals("SELLER-SKU-1", result.getSkus().get(0).getSellerSkuCode());
        assertFalse(hasGetter(SellerPortalProduct.class, "getSellerId"));
        assertFalse(hasGetter(SellerPortalProduct.class, "getSystemSpuCode"));
        assertFalse(hasGetter(SellerPortalProductSku.class, "getSellerId"));
        assertFalse(hasGetter(SellerPortalProductSku.class, "getSystemSkuCode"));
    }

    @Test
    public void selectOwnSkuListVerifiesOwnershipBeforeReadingSkus()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.productByIdResult = product(1L, 11L, "SELLER-SPU-1");
        productService.skuListResult.add(sku(2L, 1L, 11L, "SELLER-SKU-1"));
        SellerPortalProductServiceImpl service = service(productService);

        List<SellerPortalProductSku> result = service.selectOwnSkuList(session(11L, 22L), 1L);

        assertEquals(Long.valueOf(1L), productService.productByIdArg);
        assertEquals(Long.valueOf(1L), productService.skuListSpuId);
        assertEquals(1, result.size());
        assertEquals("SELLER-SKU-1", result.get(0).getSellerSkuCode());
    }

    @Test
    public void selectOwnSkuListRejectsOtherSellerBeforeReadingSkus()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.productByIdResult = product(1L, 99L, "SELLER-SPU-OTHER");
        SellerPortalProductServiceImpl service = service(productService);

        assertServiceException(() -> service.selectOwnSkuList(session(11L, 22L), 1L));

        assertEquals(null, productService.skuListSpuId);
    }

    private SellerPortalProductServiceImpl service(IProductDistributionService productService)
    {
        SellerPortalProductServiceImpl service = new SellerPortalProductServiceImpl();
        setField(service, "productDistributionService", productService);
        return service;
    }

    private PortalLoginSession session(Long sellerId, Long accountId)
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("seller");
        session.setSubjectId(sellerId);
        session.setAccountId(accountId);
        session.setTokenId("seller_test_token");
        return session;
    }

    private ProductSpu product(Long spuId, Long sellerId, String sellerSpuCode)
    {
        ProductSpu product = new ProductSpu();
        product.setSpuId(spuId);
        product.setSellerId(sellerId);
        product.setSellerSpuCode(sellerSpuCode);
        product.setSpuStatus("ON_SALE");
        return product;
    }

    private ProductSku sku(Long skuId, Long spuId, Long sellerId, String sellerSkuCode)
    {
        ProductSku sku = new ProductSku();
        sku.setSkuId(skuId);
        sku.setSpuId(spuId);
        sku.setSellerId(sellerId);
        sku.setSystemSkuCode("SKU-INTERNAL");
        sku.setSellerSkuCode(sellerSkuCode);
        sku.setSkuStatus("ON_SALE");
        return sku;
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

    private void assertServiceException(ThrowingRunnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (ServiceException e)
        {
            assertEquals("商城商品不存在", e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
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
            throw new IllegalStateException("Unable to set field " + fieldName, e);
        }
    }

    private interface ThrowingRunnable
    {
        void run();
    }

    private static class RecordingProductDistributionService implements IProductDistributionService
    {
        private final List<ProductSku> skuListResult = new ArrayList<>();

        private List<ProductSpu> productListResult = new ArrayList<>();
        private ProductSpu productListQuery;
        private List<ProductSpu> returnedProductListSource;
        private ProductSpu productByIdResult;
        private Long productByIdArg;
        private Long skuListSpuId;

        @Override
        public List<ProductSpu> selectProductList(ProductSpu query)
        {
            productListQuery = query;
            returnedProductListSource = productListResult;
            return productListResult;
        }

        @Override
        public ProductSpu selectProductById(Long spuId)
        {
            productByIdArg = spuId;
            return productByIdResult;
        }

        @Override
        public int insertProduct(ProductSpu product)
        {
            return 0;
        }

        @Override
        public int updateProduct(ProductSpu product)
        {
            return 0;
        }

        @Override
        public int updateSpuStatus(Long spuId, String status)
        {
            return 0;
        }

        @Override
        public int updateSkuStatus(Long spuId, Long skuId, String status)
        {
            return 0;
        }

        @Override
        public List<ProductSku> selectSkuList(Long spuId)
        {
            skuListSpuId = spuId;
            return skuListResult;
        }
    }
}
