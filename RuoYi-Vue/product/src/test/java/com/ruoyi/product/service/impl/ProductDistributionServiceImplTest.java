package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.ruoyi.product.domain.ProductSkuSourceBinding;
import com.ruoyi.product.mapper.ProductDistributionMapper;

public class ProductDistributionServiceImplTest
{
    @Test
    public void deleteUpstreamSkuPairingsFallsBackToExistingPairingConnectionsWhenReadModelScopeDisappears()
            throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        mapper.dimensionConnectionCodes = List.of();
        mapper.pairingConnectionCodes = List.of("CONN_OLD");
        ProductDistributionServiceImpl service = service(mapper.proxy());

        invokeDeleteUpstreamSkuPairings(service, binding("SYS-SKU-1", "DIM_REMOVED", "MASTER-1"));

        assertEquals("DIM_REMOVED", mapper.dimensionLookupKey);
        assertEquals("SYS-SKU-1", mapper.fallbackSystemSku);
        assertEquals("MASTER-1", mapper.fallbackMasterSku);
        assertEquals("SYS-SKU-1", mapper.deletedSystemSku);
        assertEquals(List.of("CONN_OLD"), mapper.deletedConnectionCodes);
    }

    @Test
    public void deleteUpstreamSkuPairingsKeepsEmptyScopeNoopWhenNoDimensionOrPairingConnectionsExist()
            throws Exception
    {
        RecordingProductDistributionMapper mapper = new RecordingProductDistributionMapper();
        mapper.dimensionConnectionCodes = List.of();
        mapper.pairingConnectionCodes = List.of();
        ProductDistributionServiceImpl service = service(mapper.proxy());

        invokeDeleteUpstreamSkuPairings(service, binding("SYS-SKU-1", "DIM_REMOVED", "MASTER-1"));

        assertEquals("DIM_REMOVED", mapper.dimensionLookupKey);
        assertEquals("SYS-SKU-1", mapper.fallbackSystemSku);
        assertEquals("MASTER-1", mapper.fallbackMasterSku);
        assertNull(mapper.deletedSystemSku);
        assertNull(mapper.deletedConnectionCodes);
    }

    private ProductDistributionServiceImpl service(ProductDistributionMapper mapper) throws Exception
    {
        ProductDistributionServiceImpl service = new ProductDistributionServiceImpl();
        setField(service, "productDistributionMapper", mapper);
        return service;
    }

    private void invokeDeleteUpstreamSkuPairings(ProductDistributionServiceImpl service,
            ProductSkuSourceBinding binding) throws Exception
    {
        Method method = ProductDistributionServiceImpl.class.getDeclaredMethod("deleteUpstreamSkuPairings",
                ProductSkuSourceBinding.class);
        method.setAccessible(true);
        method.invoke(service, binding);
    }

    private ProductSkuSourceBinding binding(String systemSkuCode, String sourceDimensionGroupKey, String masterSku)
    {
        ProductSkuSourceBinding binding = new ProductSkuSourceBinding();
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
        private List<String> dimensionConnectionCodes = new ArrayList<>();
        private List<String> pairingConnectionCodes = new ArrayList<>();
        private String dimensionLookupKey;
        private String fallbackSystemSku;
        private String fallbackMasterSku;
        private String deletedSystemSku;
        private List<String> deletedConnectionCodes;

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
                case "selectSourceConnectionCodesByDimensionGroup":
                    dimensionLookupKey = (String) args[0];
                    return dimensionConnectionCodes;
                case "selectUpstreamSkuPairingConnectionCodesBySystemSkuAndMasterSku":
                    fallbackSystemSku = (String) args[0];
                    fallbackMasterSku = (String) args[1];
                    return pairingConnectionCodes;
                case "deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes":
                    deletedSystemSku = (String) args[0];
                    deletedConnectionCodes = copyStringList(args[1]);
                    return deletedConnectionCodes.size();
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
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
