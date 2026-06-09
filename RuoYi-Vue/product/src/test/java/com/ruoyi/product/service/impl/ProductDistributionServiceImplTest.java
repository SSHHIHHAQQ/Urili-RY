package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.SourceOfficialWarehouseOption;
import com.ruoyi.integration.domain.SourceProductBindingSnapshot;
import com.ruoyi.integration.domain.SourceSkuPairingProjection;
import com.ruoyi.integration.service.ISourceSkuPairingProjectionService;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSkuSourceBinding;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.mapper.ProductDistributionMapper;
import com.ruoyi.product.mapper.ProductReviewMapper;

public class ProductDistributionServiceImplTest
{
    @Test
    public void deleteUpstreamSkuPairingsFallsBackToExistingPairingConnectionsWhenReadModelScopeDisappears()
            throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        RecordingSourceSkuPairingProjectionService projectionService = new RecordingSourceSkuPairingProjectionService();
        projectionService.dimensionConnectionCodes = List.of();
        projectionService.pairingConnectionCodes = List.of("CONN_OLD");
        ProductDistributionServiceImpl service = service(mapper.proxy(), projectionService);

        invokeDeleteUpstreamSkuPairings(service, binding("SYS-SKU-1", "DIM_REMOVED", "MASTER-1"));

        assertEquals("DIM_REMOVED", projectionService.dimensionLookupKey);
        assertEquals("SYS-SKU-1", projectionService.fallbackSystemSku);
        assertEquals("MASTER-1", projectionService.fallbackMasterSku);
        assertEquals("SYS-SKU-1", projectionService.deletedSystemSku);
        assertEquals(List.of("CONN_OLD"), projectionService.deletedConnectionCodes);
    }

    @Test
    public void deleteUpstreamSkuPairingsKeepsEmptyScopeNoopWhenNoDimensionOrPairingConnectionsExist()
            throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        RecordingSourceSkuPairingProjectionService projectionService = new RecordingSourceSkuPairingProjectionService();
        projectionService.dimensionConnectionCodes = List.of();
        projectionService.pairingConnectionCodes = List.of();
        ProductDistributionServiceImpl service = service(mapper.proxy(), projectionService);

        invokeDeleteUpstreamSkuPairings(service, binding("SYS-SKU-1", "DIM_REMOVED", "MASTER-1"));

        assertEquals("DIM_REMOVED", projectionService.dimensionLookupKey);
        assertEquals("SYS-SKU-1", projectionService.fallbackSystemSku);
        assertEquals("MASTER-1", projectionService.fallbackMasterSku);
        assertNull(projectionService.deletedSystemSku);
        assertNull(projectionService.deletedConnectionCodes);
    }

    @Test
    public void selectSkuListWithSellerScopePushesSellerIdToMapper() throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        ProductSpu product = new ProductSpu();
        product.setSpuId(1L);
        product.setSellerId(11L);
        mapper.scopedProductByIdResult = product;
        mapper.skuListResult = List.of(new ProductSku());
        ProductDistributionServiceImpl service = service(mapper.proxy());

        List<ProductSku> result = service.selectSkuList(1L, 11L);

        assertSame(mapper.skuListResult, result);
        assertNull(mapper.productByIdArg);
        assertEquals(Long.valueOf(1L), mapper.scopedProductByIdSpuId);
        assertEquals(Long.valueOf(11L), mapper.scopedProductByIdSellerId);
        assertEquals(Long.valueOf(1L), mapper.scopedSkuListSpuId);
        assertEquals(Long.valueOf(11L), mapper.scopedSkuListSellerId);
    }

    @Test
    public void selectSkuListWithSellerScopeRejectsOtherSellerAtProductQueryBeforeReadingSkus() throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        ProductDistributionServiceImpl service = service(mapper.proxy());

        try
        {
            service.selectSkuList(1L, 11L);
        }
        catch (ServiceException e)
        {
            assertEquals("商城商品不存在", e.getMessage());
            assertEquals(Long.valueOf(1L), mapper.scopedProductByIdSpuId);
            assertEquals(Long.valueOf(11L), mapper.scopedProductByIdSellerId);
            assertNull(mapper.scopedSkuListSpuId);
            assertNull(mapper.scopedSkuListSellerId);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectProductByIdWithSellerScopePushesSellerIdToProductAndSkuQueries() throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        ProductSpu product = new ProductSpu();
        product.setSpuId(1L);
        product.setSellerId(11L);
        mapper.scopedProductByIdResult = product;
        mapper.skuListResult = List.of(new ProductSku());
        ProductDistributionServiceImpl service = service(mapper.proxy());

        ProductSpu result = service.selectProductById(1L, 11L);

        assertSame(product, result);
        assertSame(mapper.skuListResult, result.getSkus());
        assertNull(mapper.productByIdArg);
        assertEquals(Long.valueOf(1L), mapper.scopedProductByIdSpuId);
        assertEquals(Long.valueOf(11L), mapper.scopedProductByIdSellerId);
        assertEquals(Long.valueOf(1L), mapper.scopedSkuListSpuId);
        assertEquals(Long.valueOf(11L), mapper.scopedSkuListSellerId);
    }

    @Test
    public void officialWarehouseSaveRejectsWhenSourceBindingIsNotPersisted() throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        ProductDistributionServiceImpl service = service(mapper.proxy());
        ProductSpu product = new ProductSpu();
        product.setWarehouseKind("official");
        ProductSku sku = new ProductSku();
        sku.setSkuId(1L);
        sku.setSourceDimensionGroupKey("DIM_NEW");
        mapper.activeSourceBindingsBySkuIdResult = List.of(binding(1L, "SKU-1", "DIM_OLD", "MASTER-1"));

        try
        {
            invokeEnsureOfficialSourceBindingsSaved(service, product, List.of(sku));
        }
        catch (ServiceException e)
        {
            assertEquals("官方仓 SKU 来源绑定保存失败，请重新配对来源 SKU", e.getMessage());
            assertEquals(List.of(1L), mapper.activeSourceBindingSkuIds);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void officialWarehouseSaveAcceptsPersistedSourceBinding() throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        ProductDistributionServiceImpl service = service(mapper.proxy());
        ProductSpu product = new ProductSpu();
        product.setWarehouseKind("official");
        ProductSku sku = new ProductSku();
        sku.setSkuId(1L);
        sku.setSourceDimensionGroupKey("DIM_NEW");
        mapper.activeSourceBindingsBySkuIdResult = List.of(binding(1L, "SKU-1", "DIM_NEW", "MASTER-1"));

        invokeEnsureOfficialSourceBindingsSaved(service, product, List.of(sku));

        assertEquals(List.of(1L), mapper.activeSourceBindingSkuIds);
    }

    private ProductDistributionServiceImpl service(ProductDistributionMapper mapper) throws Exception
    {
        return service(mapper, new RecordingSourceSkuPairingProjectionService());
    }

    private ProductDistributionServiceImpl service(ProductDistributionMapper mapper,
            ISourceSkuPairingProjectionService projectionService) throws Exception
    {
        ProductDistributionServiceImpl service = new ProductDistributionServiceImpl();
        setField(service, "productDistributionMapper", mapper);
        setField(service, "productReviewMapper", noReviewMapper());
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("sourceSkuPairingProjectionService", projectionService);
        setField(service, "sourceSkuPairingProjectionService",
                beanFactory.getBeanProvider(ISourceSkuPairingProjectionService.class));
        return service;
    }

    private ProductReviewMapper noReviewMapper()
    {
        return (ProductReviewMapper) Proxy.newProxyInstance(
                ProductReviewMapper.class.getClassLoader(),
                new Class<?>[] { ProductReviewMapper.class },
                (proxy, method, args) -> {
                    switch (method.getName())
                    {
                        case "selectLatestReviewsBySpuIds":
                            return new ArrayList<>();
                        case "countPendingReviewByKey":
                            return 0;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private void invokeDeleteUpstreamSkuPairings(ProductDistributionServiceImpl service,
            ProductSkuSourceBinding binding) throws Exception
    {
        Method method = ProductDistributionServiceImpl.class.getDeclaredMethod("deleteUpstreamSkuPairings",
                ProductSkuSourceBinding.class);
        method.setAccessible(true);
        method.invoke(service, binding);
    }

    private void invokeEnsureOfficialSourceBindingsSaved(ProductDistributionServiceImpl service, ProductSpu product,
            List<ProductSku> skus) throws Exception
    {
        Method method = ProductDistributionServiceImpl.class.getDeclaredMethod("ensureOfficialSourceBindingsSaved",
                ProductSpu.class, List.class);
        method.setAccessible(true);
        try
        {
            method.invoke(service, product, skus);
        }
        catch (InvocationTargetException e)
        {
            if (e.getCause() instanceof ServiceException)
            {
                throw (ServiceException) e.getCause();
            }
            throw e;
        }
    }

    private ProductSkuSourceBinding binding(String systemSkuCode, String sourceDimensionGroupKey, String masterSku)
    {
        return binding(null, systemSkuCode, sourceDimensionGroupKey, masterSku);
    }

    private ProductSkuSourceBinding binding(Long skuId, String systemSkuCode, String sourceDimensionGroupKey,
            String masterSku)
    {
        ProductSkuSourceBinding binding = new ProductSkuSourceBinding();
        binding.setSkuId(skuId);
        binding.setSystemSkuCode(systemSkuCode);
        binding.setSourceDimensionGroupKey(sourceDimensionGroupKey);
        binding.setMasterSku(masterSku);
        return binding;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class RecordingProductDistributionMapper implements InvocationHandler
    {
        private ProductSpu productByIdResult;
        private Long productByIdArg;
        private ProductSpu scopedProductByIdResult;
        private Long scopedProductByIdSpuId;
        private Long scopedProductByIdSellerId;
        private List<ProductSku> skuListResult = new ArrayList<>();
        private Long scopedSkuListSpuId;
        private Long scopedSkuListSellerId;
        private ProductSkuSourceBinding activeSourceBindingBySkuIdResult;
        private List<ProductSkuSourceBinding> activeSourceBindingsBySkuIdResult = new ArrayList<>();
        private Long activeSourceBindingSkuId;
        private List<Long> activeSourceBindingSkuIds = new ArrayList<>();

        private ProductDistributionMapper proxy()
        {
            return (ProductDistributionMapper) Proxy.newProxyInstance(
                    ProductDistributionMapper.class.getClassLoader(),
                    new Class<?>[] { ProductDistributionMapper.class },
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            switch (method.getName())
            {
                case "selectProductById":
                    productByIdArg = (Long) args[0];
                    return productByIdResult;
                case "selectProductByIdAndSellerId":
                    scopedProductByIdSpuId = (Long) args[0];
                    scopedProductByIdSellerId = (Long) args[1];
                    return scopedProductByIdResult;
                case "selectSkuListBySpuIdAndSellerId":
                    scopedSkuListSpuId = (Long) args[0];
                    scopedSkuListSellerId = (Long) args[1];
                    return skuListResult;
                case "selectActiveSourceBindingBySkuId":
                    activeSourceBindingSkuId = (Long) args[0];
                    return activeSourceBindingBySkuIdResult;
                case "selectActiveSourceBindingsBySkuIds":
                    activeSourceBindingSkuIds = copyLongList(args[0]);
                    return activeSourceBindingsBySkuIdResult;
                case "selectActiveSourceBindingsBySourceSkuGroupKeys":
                    return new ArrayList<>();
                case "selectWarehousesBySpuId":
                case "selectAttributeValuesBySpuId":
                case "selectImagesBySpuId":
                    return new ArrayList<>();
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }

        private List<Long> copyLongList(Object value)
        {
            List<Long> result = new ArrayList<>();
            for (Object item : (List<?>) value)
            {
                result.add((Long) item);
            }
            return result;
        }
    }

    private static class RecordingSourceSkuPairingProjectionService implements ISourceSkuPairingProjectionService
    {
        private List<String> dimensionConnectionCodes = new ArrayList<>();
        private List<String> pairingConnectionCodes = new ArrayList<>();
        private SourceProductBindingSnapshot sourceBindingSnapshot;
        private String sourceBindingLookupKey;
        private String dimensionLookupKey;
        private String fallbackSystemSku;
        private String fallbackMasterSku;
        private String deletedSystemSku;
        private List<String> deletedConnectionCodes;

        @Override
        public List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroup(
                String sourceDimensionGroupKey)
        {
            return new ArrayList<>();
        }

        @Override
        public List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroups(
            List<String> sourceDimensionGroupKeys)
        {
            return new ArrayList<>();
        }

        @Override
        public SourceProductBindingSnapshot selectOfficialSourceBindingSnapshot(String sourceDimensionGroupKey)
        {
            sourceBindingLookupKey = sourceDimensionGroupKey;
            return sourceBindingSnapshot;
        }

        @Override
        public List<SourceProductBindingSnapshot> selectOfficialSourceBindingSnapshots(List<String> sourceDimensionGroupKeys)
        {
            if (sourceBindingSnapshot == null)
            {
                return new ArrayList<>();
            }
            sourceBindingLookupKey = sourceDimensionGroupKeys == null ? null : String.join(",", sourceDimensionGroupKeys);
            return List.of(sourceBindingSnapshot);
        }

        @Override
        public List<String> selectSourceConnectionCodesByDimensionGroup(String sourceDimensionGroupKey)
        {
            dimensionLookupKey = sourceDimensionGroupKey;
            return dimensionConnectionCodes;
        }

        @Override
        public List<String> selectPairingConnectionCodesBySystemSkuAndMasterSku(String systemSku, String masterSku)
        {
            fallbackSystemSku = systemSku;
            fallbackMasterSku = masterSku;
            return pairingConnectionCodes;
        }

        @Override
        public int deletePairingsBySystemSkuAndConnectionCodes(String systemSku, List<String> connectionCodes)
        {
            deletedSystemSku = systemSku;
            deletedConnectionCodes = copyStringList(connectionCodes);
            return deletedConnectionCodes.size();
        }

        @Override
        public int upsertPairingsForProjection(SourceSkuPairingProjection projection)
        {
            return 1;
        }

        private List<String> copyStringList(Object value)
        {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value)
            {
                result.add((String) item);
            }
            return result;
        }
    }
}
