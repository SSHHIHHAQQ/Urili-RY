package com.ruoyi.buyer.service.impl;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.mapper.BuyerPortalDeptMapper;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.service.support.PartnerSupport;

public class BuyerPortalDeptServiceImplTest
{
    @Test
    public void updateDeptRejectsDeptOutsideBuyerBeforeMutating()
    {
        RecordingBuyerPortalDeptMapper deptMapper = new RecordingBuyerPortalDeptMapper();
        BuyerPortalDeptServiceImpl service = service(buyerService(buyer(11L)), deptMapper.proxy());
        PortalDept payload = dept(31L, 0L, "Procurement");

        assertServiceException(() -> service.updateDept(11L, payload));

        assertEquals(1, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedBuyerId);
        assertEquals(Long.valueOf(31L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.updateDeptCallCount);
        assertEquals(0, deptMapper.updateAncestorsCallCount);
        assertEquals(0, deptMapper.deleteDeptCallCount);
    }

    @Test
    public void updateDeptRejectsParentOutsideBuyerBeforeMutating()
    {
        RecordingBuyerPortalDeptMapper deptMapper = new RecordingBuyerPortalDeptMapper()
                .withDept(11L, dept(31L, 0L, "Procurement"));
        BuyerPortalDeptServiceImpl service = service(buyerService(buyer(11L)), deptMapper.proxy());
        PortalDept payload = dept(31L, 40L, "Procurement");

        assertServiceException(() -> service.updateDept(11L, payload));

        assertEquals(2, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedBuyerId);
        assertEquals(Long.valueOf(40L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.updateDeptCallCount);
        assertEquals(0, deptMapper.updateAncestorsCallCount);
    }

    @Test
    public void insertDeptRejectsParentOutsideBuyerBeforeMutating()
    {
        RecordingBuyerPortalDeptMapper deptMapper = new RecordingBuyerPortalDeptMapper();
        BuyerPortalDeptServiceImpl service = service(buyerService(buyer(11L)), deptMapper.proxy());
        PortalDept payload = dept(null, 40L, "Procurement");

        assertServiceException(() -> service.insertDept(11L, payload));

        assertEquals(1, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedBuyerId);
        assertEquals(Long.valueOf(40L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.insertDeptCallCount);
    }

    @Test
    public void deleteDeptRejectsDeptOutsideBuyerBeforeMutating()
    {
        RecordingBuyerPortalDeptMapper deptMapper = new RecordingBuyerPortalDeptMapper();
        BuyerPortalDeptServiceImpl service = service(buyerService(buyer(11L)), deptMapper.proxy());

        assertServiceException(() -> service.deleteDeptById(11L, 31L));

        assertEquals(1, deptMapper.selectDeptByIdCallCount);
        assertEquals(Long.valueOf(11L), deptMapper.selectedBuyerId);
        assertEquals(Long.valueOf(31L), deptMapper.selectedDeptId);
        assertEquals(0, deptMapper.hasChildCallCount);
        assertEquals(0, deptMapper.checkAccountCallCount);
        assertEquals(0, deptMapper.deleteDeptCallCount);
    }

    private BuyerPortalDeptServiceImpl service(IBuyerService buyerService, BuyerPortalDeptMapper deptMapper)
    {
        BuyerPortalDeptServiceImpl service = new BuyerPortalDeptServiceImpl();
        setField(service, "buyerService", buyerService);
        setField(service, "deptMapper", deptMapper);
        return service;
    }

    private Buyer buyer(Long buyerId)
    {
        Buyer buyer = new Buyer();
        buyer.setBuyerId(buyerId);
        buyer.setStatus(PartnerSupport.STATUS_NORMAL);
        return buyer;
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

    private IBuyerService buyerService(Buyer buyer)
    {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("selectBuyerById".equals(methodName))
            {
                Long buyerId = (Long) args[0];
                return buyer != null && buyerId.equals(buyer.getBuyerId()) ? buyer : null;
            }
            if ("toString".equals(methodName))
            {
                return "BuyerPortalDeptServiceImplTestBuyerService";
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
        return (IBuyerService) Proxy.newProxyInstance(
            IBuyerService.class.getClassLoader(), new Class<?>[] { IBuyerService.class }, handler);
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

    private class RecordingBuyerPortalDeptMapper
    {
        private final Map<String, PortalDept> deptByScope = new HashMap<>();

        private int selectDeptByIdCallCount;

        private Long selectedBuyerId;

        private Long selectedDeptId;

        private int insertDeptCallCount;

        private int updateDeptCallCount;

        private int updateAncestorsCallCount;

        private int hasChildCallCount;

        private int checkAccountCallCount;

        private int deleteDeptCallCount;

        private RecordingBuyerPortalDeptMapper withDept(Long buyerId, PortalDept dept)
        {
            dept.setSubjectId(buyerId);
            deptByScope.put(key(buyerId, dept.getDeptId()), dept);
            return this;
        }

        private BuyerPortalDeptMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectBuyerDeptById".equals(methodName))
                {
                    selectDeptByIdCallCount++;
                    selectedBuyerId = (Long) args[0];
                    selectedDeptId = (Long) args[1];
                    return deptByScope.get(key(selectedBuyerId, selectedDeptId));
                }
                if ("checkBuyerDeptNameUnique".equals(methodName))
                {
                    return null;
                }
                if ("insertBuyerDept".equals(methodName))
                {
                    insertDeptCallCount++;
                    return 1;
                }
                if ("updateBuyerDept".equals(methodName))
                {
                    updateDeptCallCount++;
                    return 1;
                }
                if ("updateBuyerDeptAncestors".equals(methodName))
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
                if ("deleteBuyerDeptById".equals(methodName))
                {
                    deleteDeptCallCount++;
                    return 1;
                }
                if ("toString".equals(methodName))
                {
                    return "BuyerPortalDeptServiceImplTestDeptMapper";
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
            return (BuyerPortalDeptMapper) Proxy.newProxyInstance(
                BuyerPortalDeptMapper.class.getClassLoader(), new Class<?>[] { BuyerPortalDeptMapper.class }, handler);
        }

        private String key(Long buyerId, Long deptId)
        {
            return buyerId + ":" + deptId;
        }
    }
}
