package com.ruoyi.seller.service.impl;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.mapper.SellerPortalDeptMapper;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.service.support.PartnerSupport;

public class SellerPortalDeptServiceImplTest
{
    @Test
    public void updateDeptRejectsDeptOutsideSellerBeforeMutating()
    {
        RecordingSellerPortalDeptMapper deptMapper = new RecordingSellerPortalDeptMapper();
        SellerPortalDeptServiceImpl service = service(sellerService(seller(11L)), deptMapper.proxy());
        PortalDept payload = dept(31L, 0L, "Sales");

        assertServiceException(() -> service.updateDept(11L, payload));

        assertEquals(1, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedSellerId);
        assertEquals(Long.valueOf(31L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.updateDeptCallCount);
        assertEquals(0, deptMapper.updateAncestorsCallCount);
        assertEquals(0, deptMapper.deleteDeptCallCount);
    }

    @Test
    public void updateDeptRejectsParentOutsideSellerBeforeMutating()
    {
        RecordingSellerPortalDeptMapper deptMapper = new RecordingSellerPortalDeptMapper()
                .withDept(11L, dept(31L, 0L, "Sales"));
        SellerPortalDeptServiceImpl service = service(sellerService(seller(11L)), deptMapper.proxy());
        PortalDept payload = dept(31L, 40L, "Sales");

        assertServiceException(() -> service.updateDept(11L, payload));

        assertEquals(2, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedSellerId);
        assertEquals(Long.valueOf(40L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.updateDeptCallCount);
        assertEquals(0, deptMapper.updateAncestorsCallCount);
    }

    @Test
    public void insertDeptRejectsParentOutsideSellerBeforeMutating()
    {
        RecordingSellerPortalDeptMapper deptMapper = new RecordingSellerPortalDeptMapper();
        SellerPortalDeptServiceImpl service = service(sellerService(seller(11L)), deptMapper.proxy());
        PortalDept payload = dept(null, 40L, "Sales");

        assertServiceException(() -> service.insertDept(11L, payload));

        assertEquals(1, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedSellerId);
        assertEquals(Long.valueOf(40L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.insertDeptCallCount);
    }

    @Test
    public void deleteDeptRejectsDeptOutsideSellerBeforeMutating()
    {
        RecordingSellerPortalDeptMapper deptMapper = new RecordingSellerPortalDeptMapper();
        SellerPortalDeptServiceImpl service = service(sellerService(seller(11L)), deptMapper.proxy());

        assertServiceException(() -> service.deleteDeptById(11L, 31L));

        assertEquals(1, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedSellerId);
        assertEquals(Long.valueOf(31L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.hasChildCallCount);
        assertEquals(0, deptMapper.checkAccountCallCount);
        assertEquals(0, deptMapper.deleteDeptCallCount);
    }

    private SellerPortalDeptServiceImpl service(ISellerService sellerService, SellerPortalDeptMapper deptMapper)
    {
        SellerPortalDeptServiceImpl service = new SellerPortalDeptServiceImpl();
        setField(service, "sellerService", sellerService);
        setField(service, "deptMapper", deptMapper);
        return service;
    }

    private Seller seller(Long sellerId)
    {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setStatus(PartnerSupport.STATUS_NORMAL);
        return seller;
    }

    private PortalDept dept(Long deptId, Long parentId, String deptName)
    {
        PortalDept dept = new PortalDept();
        dept.setDeptId(deptId);
        dept.setParentId(parentId);
        dept.setAncestors("0");
        dept.setDeptName(deptName);
        dept.setOrderNum(0);
        dept.setStatus(PartnerSupport.STATUS_NORMAL);
        return dept;
    }

    private ISellerService sellerService(Seller seller)
    {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("selectSellerById".equals(methodName))
            {
                Long sellerId = (Long) args[0];
                return seller != null && sellerId.equals(seller.getSellerId()) ? seller : null;
            }
            if ("toString".equals(methodName))
            {
                return "SellerPortalDeptServiceImplTestSellerService";
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        };
        return (ISellerService) Proxy.newProxyInstance(
            ISellerService.class.getClassLoader(), new Class<?>[] { ISellerService.class }, handler);
    }

    private void assertServiceException(ThrowingRunnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (ServiceException e)
        {
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    private Object defaultValue(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (returnType == boolean.class)
        {
            return false;
        }
        if (returnType == byte.class)
        {
            return (byte) 0;
        }
        if (returnType == short.class)
        {
            return (short) 0;
        }
        if (returnType == int.class)
        {
            return 0;
        }
        if (returnType == long.class)
        {
            return 0L;
        }
        if (returnType == float.class)
        {
            return 0F;
        }
        if (returnType == double.class)
        {
            return 0D;
        }
        if (returnType == char.class)
        {
            return '\0';
        }
        return null;
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

    private class RecordingSellerPortalDeptMapper
    {
        private final Map<String, PortalDept> deptByScope = new HashMap<>();

        private int selectDeptByIdCallCount;

        private Long selectedSellerId;

        private Long selectedDeptId;

        private int insertDeptCallCount;

        private int updateDeptCallCount;

        private int updateAncestorsCallCount;

        private int hasChildCallCount;

        private int checkAccountCallCount;

        private int deleteDeptCallCount;

        private RecordingSellerPortalDeptMapper withDept(Long sellerId, PortalDept dept)
        {
            dept.setSubjectId(sellerId);
            deptByScope.put(key(sellerId, dept.getDeptId()), dept);
            return this;
        }

        private SellerPortalDeptMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectSellerDeptById".equals(methodName))
                {
                    selectDeptByIdCallCount++;
                    selectedSellerId = (Long) args[0];
                    selectedDeptId = (Long) args[1];
                    return deptByScope.get(key(selectedSellerId, selectedDeptId));
                }
                if ("checkSellerDeptNameUnique".equals(methodName))
                {
                    return null;
                }
                if ("insertSellerDept".equals(methodName))
                {
                    insertDeptCallCount++;
                    return 1;
                }
                if ("updateSellerDept".equals(methodName))
                {
                    updateDeptCallCount++;
                    return 1;
                }
                if ("updateSellerDeptAncestors".equals(methodName))
                {
                    updateAncestorsCallCount++;
                    return 1;
                }
                if ("hasChildByDeptId".equals(methodName))
                {
                    hasChildCallCount++;
                    return 0;
                }
                if ("checkDeptExistAccount".equals(methodName))
                {
                    checkAccountCallCount++;
                    return 0;
                }
                if ("deleteSellerDeptById".equals(methodName))
                {
                    deleteDeptCallCount++;
                    return 1;
                }
                if ("toString".equals(methodName))
                {
                    return "SellerPortalDeptServiceImplTestDeptMapper";
                }
                if ("hashCode".equals(methodName))
                {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(methodName))
                {
                    return proxy == args[0];
                }
                return defaultValue(method.getReturnType());
            };
            return (SellerPortalDeptMapper) Proxy.newProxyInstance(
                SellerPortalDeptMapper.class.getClassLoader(), new Class<?>[] { SellerPortalDeptMapper.class }, handler);
        }

        private String key(Long sellerId, Long deptId)
        {
            return sellerId + ":" + deptId;
        }
    }
}
