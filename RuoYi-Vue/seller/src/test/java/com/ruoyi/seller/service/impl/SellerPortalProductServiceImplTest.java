package com.ruoyi.seller.service.impl;

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
import com.github.pagehelper.page.PageMethod;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.service.IProductPortalDistributionService;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.domain.SellerPortalProduct;
import com.ruoyi.seller.domain.SellerPortalProductSku;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.system.domain.PortalLoginSession;

public class SellerPortalProductServiceImplTest
{
    @Test
    public void selectOwnProductListUsesSessionScopeAndIgnoresClientSellerId()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        ProductSpu product = product(1L, 11L, "SELLER-SPU-1");
        product.setSkus(List.of(sku(99L, 1L, 99L, "ROGUE-SKU")));
        productService.productListResult.add(product);
        productService.skuListResult.add(sku(2L, 1L, 11L, "SELLER-SKU-1"));
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
        assertEquals(Long.valueOf(1L), productService.skuListSpuId);
        assertEquals(Long.valueOf(11L), productService.skuListSellerId);
        assertEquals(1, result.get(0).getSkus().size());
        assertEquals("SELLER-SKU-1", result.get(0).getSkus().get(0).getSellerSkuCode());
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
    public void selectOwnProductListDoesNotApplyPageHelperToSessionAccountLookup()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        RecordingSellerMapper sellerMapper = new RecordingSellerMapper(true);
        SellerPortalProductServiceImpl service = service(productService, sellerMapper(sellerMapper));
        Page<ProductSpu> activePage = new Page<>(1, 20, true);

        PageMethod.setLocalPage(activePage);
        try
        {
            service.selectOwnProductList(session(11L, 22L), new ProductSpu());

            assertFalse(sellerMapper.pageActiveDuringAccountLookup);
            assertSame(activePage, productService.pageDuringProductList);
            assertSame(activePage, PageMethod.getLocalPage());
        }
        finally
        {
            PageMethod.clearPage();
        }
    }

    @Test
    public void selectOwnProductListRejectsNonSellerSessionBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        SellerPortalProductServiceImpl service = service(productService);
        PortalLoginSession buyerSession = session(11L, 22L);
        buyerSession.setTerminal("buyer");

        assertLoginException(() -> service.selectOwnProductList(buyerSession, new ProductSpu()));

        assertEquals(null, productService.productListQuery);
    }

    @Test
    public void selectOwnProductListRejectsBlankTokenBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        SellerPortalProductServiceImpl service = service(productService);
        PortalLoginSession session = session(11L, 22L);
        session.setTokenId(" ");

        assertLoginException(() -> service.selectOwnProductList(session, new ProductSpu()));

        assertEquals(null, productService.productListQuery);
    }

    @Test
    public void selectOwnProductListRejectsMissingAccountBindingBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        RecordingSellerMapper sellerMapper = new RecordingSellerMapper(false);
        SellerPortalProductServiceImpl service = service(productService, sellerMapper(sellerMapper));

        assertLoginException(() -> service.selectOwnProductList(session(11L, 22L), new ProductSpu()));

        assertEquals(Long.valueOf(11L), sellerMapper.sellerId);
        assertEquals(Long.valueOf(22L), sellerMapper.sellerAccountId);
        assertEquals(null, productService.productListQuery);
    }

    @Test
    public void selectOwnProductByIdRejectsProductFromAnotherSeller()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.productByIdResult = product(1L, 99L, "SELLER-SPU-OTHER");
        SellerPortalProductServiceImpl service = service(productService);

        assertServiceException(() -> service.selectOwnProductById(session(11L, 22L), 1L));

        assertEquals(Long.valueOf(1L), productService.productByIdArg);
        assertEquals(Long.valueOf(11L), productService.productByIdSellerId);
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
        product.setSkus(List.of(sku(99L, 1L, 99L, "ROGUE-SKU")));
        productService.skuListResult.add(sku(2L, 1L, 11L, "SELLER-SKU-1"));
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
        assertEquals(Long.valueOf(1L), productService.skuListSpuId);
        assertEquals(Long.valueOf(11L), productService.skuListSellerId);
        assertEquals(Long.valueOf(11L), productService.productByIdSellerId);
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
        assertEquals(Long.valueOf(11L), productService.productByIdSellerId);
        assertEquals(Long.valueOf(1L), productService.skuListSpuId);
        assertEquals(Long.valueOf(11L), productService.skuListSellerId);
        assertEquals(1, result.size());
        assertEquals("SELLER-SKU-1", result.get(0).getSellerSkuCode());
    }

    @Test
    public void selectOwnProductDetailAndSkuRejectNonSellerSessionBeforeQueryingProducts()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        SellerPortalProductServiceImpl service = service(productService);
        PortalLoginSession buyerSession = session(11L, 22L);
        buyerSession.setTerminal("buyer");

        assertLoginException(() -> service.selectOwnProductById(buyerSession, 1L));
        assertLoginException(() -> service.selectOwnSkuList(buyerSession, 1L));

        assertEquals(null, productService.productByIdArg);
        assertEquals(null, productService.skuListSpuId);
        assertEquals(null, productService.skuListSellerId);
    }

    @Test
    public void selectOwnSkuListRejectsOtherSellerBeforeReadingSkus()
    {
        RecordingProductDistributionService productService = new RecordingProductDistributionService();
        productService.productByIdResult = product(1L, 99L, "SELLER-SPU-OTHER");
        SellerPortalProductServiceImpl service = service(productService);

        assertServiceException(() -> service.selectOwnSkuList(session(11L, 22L), 1L));

        assertEquals(null, productService.skuListSpuId);
        assertEquals(null, productService.skuListSellerId);
    }

    private SellerPortalProductServiceImpl service(IProductPortalDistributionService productService)
    {
        return service(productService, sellerMapper(new RecordingSellerMapper(true)));
    }

    private SellerPortalProductServiceImpl service(IProductPortalDistributionService productService, SellerMapper sellerMapper)
    {
        SellerPortalProductServiceImpl service = new SellerPortalProductServiceImpl();
        setField(service, "productPortalDistributionService", productService);
        setField(service, "sellerMapper", sellerMapper);
        return service;
    }

    private SellerMapper sellerMapper(RecordingSellerMapper recording)
    {
        return (SellerMapper) Proxy.newProxyInstance(SellerMapper.class.getClassLoader(),
            new Class<?>[] { SellerMapper.class }, recording);
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
        private final List<ProductSku> skuListResult = new ArrayList<>();

        private List<ProductSpu> productListResult = new ArrayList<>();
        private ProductSpu productListQuery;
        private List<ProductSpu> returnedProductListSource;
        private Page<?> pageDuringProductList;
        private ProductSpu productByIdResult;
        private Long productByIdArg;
        private Long productByIdSellerId;
        private Long skuListSpuId;
        private Long skuListSellerId;

        @Override
        public List<ProductSpu> selectSellerProductList(ProductSpu query)
        {
            productListQuery = query;
            pageDuringProductList = PageMethod.getLocalPage();
            returnedProductListSource = productListResult;
            return productListResult;
        }

        @Override
        public ProductSpu selectSellerProductById(Long spuId, Long sellerId)
        {
            productByIdArg = spuId;
            productByIdSellerId = sellerId;
            return productByIdResult;
        }

        @Override
        public List<ProductSku> selectSellerSkuList(Long spuId, Long sellerId)
        {
            skuListSpuId = spuId;
            skuListSellerId = sellerId;
            return skuListResult;
        }

        @Override
        public List<ProductSpu> selectBuyerVisibleProductList(ProductSpu query)
        {
            return new ArrayList<>();
        }

        @Override
        public ProductSpu selectBuyerVisibleProductById(Long spuId)
        {
            return null;
        }

        @Override
        public List<ProductSku> selectBuyerVisibleSkuList(Long spuId)
        {
            return new ArrayList<>();
        }
    }

    private static class RecordingSellerMapper implements InvocationHandler
    {
        private final boolean accountExists;
        private Long sellerId;
        private Long sellerAccountId;
        private boolean pageActiveDuringAccountLookup;

        private RecordingSellerMapper(boolean accountExists)
        {
            this.accountExists = accountExists;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            if ("selectSellerAccountByIdAndSellerId".equals(method.getName()))
            {
                sellerId = (Long) args[0];
                sellerAccountId = (Long) args[1];
                pageActiveDuringAccountLookup = PageMethod.getLocalPage() != null;
                return accountExists ? new SellerAccount() : null;
            }
            if ("toString".equals(method.getName()))
            {
                return "RecordingSellerMapper";
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
