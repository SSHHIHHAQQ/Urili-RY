package com.ruoyi.buyer.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.github.pagehelper.Page;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.domain.BuyerPortalProduct;
import com.ruoyi.buyer.domain.BuyerPortalProductSku;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.service.IProductPortalDistributionService;
import com.ruoyi.system.domain.PortalLoginSession;

public class BuyerPortalProductServiceImplTest
{
    @Test
    public void selectVisibleProductListUsesOnSaleProductQueryAndIgnoresClientScopeFields()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.onSaleProductListResult.add(onSaleProduct(1L));
        BuyerPortalProductServiceImpl service = service(productService);
        ProductSpu request = new ProductSpu();
        request.setSellerId(99L);
        request.setSystemSpuCode("SPU-INTERNAL");
        request.setSystemSkuCode("SKU-INTERNAL");
        request.setSellerSpuCode("SELLER-SPU");
        request.setSellerSkuCode("SELLER-SKU");
        request.setProductName("椅子");
        request.setProductNameEn("Chair");
        request.setCategoryId(7L);
        request.setSpuStatus("DRAFT");
        request.setKeyword("chair");
        request.setSourceType("ADMIN_MANUAL");

        List<BuyerPortalProduct> result = service.selectVisibleProductList(buyerSession(), request);

        assertSame(productService.onSaleProductListResult, productService.returnedOnSaleProductListSource);
        assertEquals(1, result.size());
        assertEquals("椅子", productService.onSaleProductListQuery.getProductName());
        assertEquals("Chair", productService.onSaleProductListQuery.getProductNameEn());
        assertEquals(Long.valueOf(7L), productService.onSaleProductListQuery.getCategoryId());
        assertEquals("chair", productService.onSaleProductListQuery.getKeyword());
        assertEquals(null, productService.onSaleProductListQuery.getSellerId());
        assertEquals(null, productService.onSaleProductListQuery.getSystemSpuCode());
        assertEquals(null, productService.onSaleProductListQuery.getSystemSkuCode());
        assertEquals(null, productService.onSaleProductListQuery.getSellerSpuCode());
        assertEquals(null, productService.onSaleProductListQuery.getSellerSkuCode());
        assertEquals(null, productService.onSaleProductListQuery.getSpuStatus());
        assertEquals(null, productService.onSaleProductListQuery.getSourceType());
        assertEquals(Long.valueOf(99L), request.getSellerId());
        assertEquals("DRAFT", request.getSpuStatus());
    }

    @Test
    public void selectVisibleProductListPreservesPageHelperMetadata()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        Page<ProductSpu> page = new Page<>(3, 20, true);
        page.setTotal(45);
        page.setPages(3);
        page.add(onSaleProduct(1L));
        productService.onSaleProductListResult = page;
        BuyerPortalProductServiceImpl service = service(productService);

        List<BuyerPortalProduct> result = service.selectVisibleProductList(buyerSession(), new ProductSpu());

        Page<?> resultPage = (Page<?>) result;
        assertEquals(1, result.size());
        assertEquals(3, resultPage.getPageNum());
        assertEquals(20, resultPage.getPageSize());
        assertEquals(45L, resultPage.getTotal());
        assertEquals(3, resultPage.getPages());
    }

    @Test
    public void selectVisibleProductListRejectsNonBuyerSessionBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        BuyerPortalProductServiceImpl service = service(productService);
        PortalLoginSession sellerSession = buyerSession();
        sellerSession.setTerminal("seller");

        assertLoginException(() -> service.selectVisibleProductList(sellerSession, new ProductSpu()));

        assertEquals(null, productService.onSaleProductListQuery);
    }

    @Test
    public void selectVisibleProductListRejectsBlankTokenBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        BuyerPortalProductServiceImpl service = service(productService);
        PortalLoginSession session = buyerSession();
        session.setTokenId(" ");

        assertLoginException(() -> service.selectVisibleProductList(session, new ProductSpu()));

        assertEquals(null, productService.onSaleProductListQuery);
    }

    @Test
    public void selectVisibleProductListRejectsMissingAccountBindingBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        RecordingBuyerMapper buyerMapper = new RecordingBuyerMapper(false);
        BuyerPortalProductServiceImpl service = service(productService, buyerMapper(buyerMapper));

        assertLoginException(() -> service.selectVisibleProductList(buyerSession(), new ProductSpu()));

        assertEquals(Long.valueOf(11L), buyerMapper.buyerId);
        assertEquals(Long.valueOf(22L), buyerMapper.buyerAccountId);
        assertEquals(null, productService.onSaleProductListQuery);
    }

    @Test
    public void selectVisibleProductByIdRejectsMissingOrInvisibleProduct()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        BuyerPortalProductServiceImpl service = service(productService);

        assertProductException(() -> service.selectVisibleProductById(buyerSession(), 1L));

        assertEquals(Long.valueOf(1L), productService.onSaleProductByIdArg);
    }

    @Test
    public void selectVisibleProductDetailAndSkuRejectNonBuyerSessionBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        BuyerPortalProductServiceImpl service = service(productService);
        PortalLoginSession sellerSession = buyerSession();
        sellerSession.setTerminal("seller");

        assertLoginException(() -> service.selectVisibleProductById(sellerSession, 1L));
        assertLoginException(() -> service.selectVisibleSkuList(sellerSession, 1L));

        assertEquals(null, productService.onSaleProductByIdArg);
        assertEquals(null, productService.onSaleSkuListSpuId);
    }

    @Test
    public void selectVisibleProductByIdRejectsProductWithoutOnSaleSku()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        ProductSpu product = onSaleProduct(1L);
        product.setSkus(List.of(sku(2L, 1L, "READY", "20.00", "USD")));
        productService.onSaleProductByIdResult = product;
        BuyerPortalProductServiceImpl service = service(productService);

        assertProductException(() -> service.selectVisibleProductById(buyerSession(), 1L));
    }

    @Test
    public void selectVisibleProductByIdReturnsBuyerDtoWithoutSellerOrAdminFields()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        ProductSpu product = onSaleProduct(1L);
        product.setSystemSpuCode("SPU-INTERNAL");
        product.setSellerSpuCode("SELLER-SPU");
        product.setSellerId(99L);
        product.setSellerName("Seller A");
        product.setProductName("椅子");
        product.setProductNameEn("Chair");
        product.setSupplyPriceMin(new BigDecimal("10.00"));
        product.setSalePriceMin(new BigDecimal("20.00"));
        product.setWarehouseCount(2);
        product.setSkus(List.of(
            sku(2L, 1L, "ON_SALE", "20.00", "USD"),
            sku(3L, 1L, "OFF_SALE", "99.00", "USD")
        ));
        productService.onSaleProductByIdResult = product;
        BuyerPortalProductServiceImpl service = service(productService);

        BuyerPortalProduct result = service.selectVisibleProductById(buyerSession(), 1L);

        assertEquals(Long.valueOf(1L), result.getSpuId());
        assertEquals("椅子", result.getProductName());
        assertEquals("SPU-INTERNAL", result.getSystemSpuCode());
        assertEquals(new BigDecimal("20.00"), result.getSalePriceMin());
        assertEquals(new BigDecimal("20.00"), result.getSalePriceMax());
        assertEquals("USD", result.getCurrencySummary());
        assertEquals(Integer.valueOf(1), result.getSkuCount());
        assertEquals(Integer.valueOf(2), result.getWarehouseCount());
        assertEquals(1, result.getSkus().size());
        assertEquals(new BigDecimal("20.00"), result.getSkus().get(0).getSalePrice());
        assertEquals("SKU-INTERNAL", result.getSkus().get(0).getSystemSkuCode());
        assertEquals(Integer.valueOf(2), result.getSkus().get(0).getWarehouseCount());
        assertFalse(hasGetter(BuyerPortalProduct.class, "getSellerId"));
        assertFalse(hasGetter(BuyerPortalProduct.class, "getSellerSpuCode"));
        assertFalse(hasGetter(BuyerPortalProduct.class, "getSupplyPriceMin"));
        assertFalse(hasGetter(BuyerPortalProductSku.class, "getSellerId"));
        assertFalse(hasGetter(BuyerPortalProductSku.class, "getSellerSkuCode"));
        assertFalse(hasGetter(BuyerPortalProductSku.class, "getSupplyPrice"));
    }

    @Test
    public void selectVisibleSkuListVerifiesProductVisibilityBeforeReadingSkus()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.onSaleProductByIdResult = onSaleProduct(1L);
        productService.onSaleSkuListResult.add(sku(2L, 1L, "ON_SALE", "20.00", "USD"));
        BuyerPortalProductServiceImpl service = service(productService);

        List<BuyerPortalProductSku> result = service.selectVisibleSkuList(buyerSession(), 1L);

        assertEquals(Long.valueOf(1L), productService.onSaleProductByIdArg);
        assertEquals(Long.valueOf(1L), productService.onSaleSkuListSpuId);
        assertEquals(1, result.size());
        assertEquals("ON_SALE", result.get(0).getSkuStatus());
    }

    @Test
    public void selectVisibleSkuListRejectsInvisibleProductBeforeReadingSkus()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        BuyerPortalProductServiceImpl service = service(productService);

        assertProductException(() -> service.selectVisibleSkuList(buyerSession(), 1L));

        assertEquals(Long.valueOf(1L), productService.onSaleProductByIdArg);
        assertEquals(null, productService.onSaleSkuListSpuId);
    }

    private BuyerPortalProductServiceImpl service(IProductPortalDistributionService productService)
    {
        return service(productService, buyerMapper(new RecordingBuyerMapper(true)));
    }

    private BuyerPortalProductServiceImpl service(IProductPortalDistributionService productService, BuyerMapper buyerMapper)
    {
        BuyerPortalProductServiceImpl service = new BuyerPortalProductServiceImpl();
        setField(service, "productPortalDistributionService", productService);
        setField(service, "buyerMapper", buyerMapper);
        return service;
    }

    private BuyerMapper buyerMapper(RecordingBuyerMapper recording)
    {
        return (BuyerMapper) Proxy.newProxyInstance(BuyerMapper.class.getClassLoader(),
            new Class<?>[] { BuyerMapper.class }, recording);
    }

    private PortalLoginSession buyerSession()
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("buyer");
        session.setSubjectId(11L);
        session.setAccountId(22L);
        session.setTokenId("buyer_test_token");
        return session;
    }

    private ProductSpu onSaleProduct(Long spuId)
    {
        ProductSpu product = new ProductSpu();
        product.setSpuId(spuId);
        product.setSpuStatus("ON_SALE");
        product.setCategoryId(7L);
        product.setCategoryCode("CAT");
        product.setCategoryName("家具");
        product.setProductName("椅子");
        product.setProductNameEn("Chair");
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
        sku.setWarehouseCount(2);
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

    private void assertProductException(ThrowingRunnable runnable)
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

    private void assertLoginException(ThrowingRunnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (ServiceException e)
        {
            assertEquals("登录状态已失效", e.getMessage());
            assertEquals(Integer.valueOf(HttpStatus.UNAUTHORIZED), e.getCode());
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

    private static class RecordingProductDistributionService implements IProductPortalDistributionService
    {
        private List<ProductSpu> onSaleProductListResult = new ArrayList<>();
        private final List<ProductSku> onSaleSkuListResult = new ArrayList<>();

        private ProductSpu onSaleProductListQuery;
        private List<ProductSpu> returnedOnSaleProductListSource;
        private ProductSpu onSaleProductByIdResult;
        private Long onSaleProductByIdArg;
        private Long onSaleSkuListSpuId;

        @Override
        public List<ProductSpu> selectSellerProductList(ProductSpu query)
        {
            return new ArrayList<>();
        }

        @Override
        public ProductSpu selectSellerProductById(Long spuId, Long sellerId)
        {
            return null;
        }

        @Override
        public List<ProductSku> selectSellerSkuList(Long spuId, Long sellerId)
        {
            return new ArrayList<>();
        }

        @Override
        public List<ProductSpu> selectBuyerVisibleProductList(ProductSpu query)
        {
            onSaleProductListQuery = query;
            returnedOnSaleProductListSource = onSaleProductListResult;
            return onSaleProductListResult;
        }

        @Override
        public ProductSpu selectBuyerVisibleProductById(Long spuId)
        {
            onSaleProductByIdArg = spuId;
            return onSaleProductByIdResult;
        }

        @Override
        public List<ProductSku> selectBuyerVisibleSkuList(Long spuId)
        {
            onSaleSkuListSpuId = spuId;
            return onSaleSkuListResult;
        }
    }

    private static class RecordingBuyerMapper implements InvocationHandler
    {
        private final boolean accountExists;
        private Long buyerId;
        private Long buyerAccountId;

        private RecordingBuyerMapper(boolean accountExists)
        {
            this.accountExists = accountExists;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            if ("selectBuyerAccountByIdAndBuyerId".equals(method.getName()))
            {
                buyerId = (Long) args[0];
                buyerAccountId = (Long) args[1];
                return accountExists ? new BuyerAccount() : null;
            }
            if ("toString".equals(method.getName()))
            {
                return "RecordingBuyerMapper";
            }
            Class<?> returnType = method.getReturnType();
            if (Integer.TYPE.equals(returnType))
            {
                return 0;
            }
            if (Long.TYPE.equals(returnType))
            {
                return 0L;
            }
            if (Boolean.TYPE.equals(returnType))
            {
                return false;
            }
            if (List.class.isAssignableFrom(returnType))
            {
                return new ArrayList<>();
            }
            return null;
        }
    }
}
