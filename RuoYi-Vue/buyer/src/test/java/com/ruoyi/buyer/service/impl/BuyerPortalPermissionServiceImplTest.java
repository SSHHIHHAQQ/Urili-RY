package com.ruoyi.buyer.service.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.mapper.BuyerPortalPermissionMapper;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.support.PartnerSupport;

public class BuyerPortalPermissionServiceImplTest
{
    @Test
    public void assignAccountRolesKeepsBuyerAccountAndRoleScope()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(2);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { 101L, null, 102L, 101L, 0L });

        assertEquals(2, rows);
        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.countedBuyerId);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.countedRoleIds);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.deletedBuyerId);
        assertEquals(Long.valueOf(22L), permissionMapper.deletedAccountId);
        assertEquals(1, permissionMapper.batchAccountRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.batchedBuyerId);
        assertEquals(Long.valueOf(22L), permissionMapper.batchedAccountId);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.batchedRoleIds);
    }

    @Test
    public void assignAccountRolesClearsRolesWhenNoRoleSelected()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(0);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { null, 0L });

        assertEquals(1, rows);
        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.deletedBuyerId);
        assertEquals(Long.valueOf(22L), permissionMapper.deletedAccountId);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsAccountFromAnotherBuyerBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 99L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L }));

        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsRolesFromAnotherBuyerBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    private BuyerPortalPermissionServiceImpl service(IBuyerService buyerService, BuyerMapper buyerMapper,
            BuyerPortalPermissionMapper permissionMapper)
    {
        BuyerPortalPermissionServiceImpl service = new BuyerPortalPermissionServiceImpl();
        setField(service, "buyerService", buyerService);
        setField(service, "buyerMapper", buyerMapper);
        setField(service, "permissionMapper", permissionMapper);
        return service;
    }

    private Buyer buyer(Long buyerId)
    {
        Buyer buyer = new Buyer();
        buyer.setBuyerId(buyerId);
        buyer.setStatus(PartnerSupport.STATUS_NORMAL);
        return buyer;
    }

    private BuyerAccount account(Long accountId, Long buyerId)
    {
        BuyerAccount account = new BuyerAccount();
        account.setBuyerAccountId(accountId);
        account.setAccountId(accountId);
        account.setBuyerId(buyerId);
        account.setStatus(PartnerSupport.STATUS_NORMAL);
        return account;
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
                return "BuyerPortalPermissionServiceImplTestBuyerService";
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

    private BuyerMapper buyerMapper(BuyerAccount... accounts)
    {
        Map<Long, BuyerAccount> accountById = new HashMap<>();
        for (BuyerAccount account : accounts)
        {
            accountById.put(account.getBuyerAccountId(), account);
        }
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("selectBuyerAccountById".equals(methodName))
            {
                return accountById.get((Long) args[0]);
            }
            if ("toString".equals(methodName))
            {
                return "BuyerPortalPermissionServiceImplTestBuyerMapper";
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
        return (BuyerMapper) Proxy.newProxyInstance(
            BuyerMapper.class.getClassLoader(), new Class<?>[] { BuyerMapper.class }, handler);
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

    private class RecordingBuyerPortalPermissionMapper
    {
        private final int validRoleCount;

        private int countRolesCallCount;

        private Long countedBuyerId;

        private Long[] countedRoleIds;

        private int deleteAccountRolesCallCount;

        private Long deletedBuyerId;

        private Long deletedAccountId;

        private int batchAccountRolesCallCount;

        private Long batchedBuyerId;

        private Long batchedAccountId;

        private Long[] batchedRoleIds;

        private RecordingBuyerPortalPermissionMapper(int validRoleCount)
        {
            this.validRoleCount = validRoleCount;
        }

        private BuyerPortalPermissionMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("countBuyerRolesByIds".equals(methodName))
                {
                    countRolesCallCount++;
                    countedBuyerId = (Long) args[0];
                    countedRoleIds = (Long[]) args[1];
                    return validRoleCount;
                }
                if ("deleteBuyerAccountRoles".equals(methodName))
                {
                    deleteAccountRolesCallCount++;
                    deletedBuyerId = (Long) args[0];
                    deletedAccountId = (Long) args[1];
                    return 1;
                }
                if ("batchBuyerAccountRoles".equals(methodName))
                {
                    batchAccountRolesCallCount++;
                    batchedBuyerId = (Long) args[0];
                    batchedAccountId = (Long) args[1];
                    batchedRoleIds = (Long[]) args[2];
                    return batchedRoleIds.length;
                }
                if ("toString".equals(methodName))
                {
                    return "BuyerPortalPermissionServiceImplTestPermissionMapper";
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
            return (BuyerPortalPermissionMapper) Proxy.newProxyInstance(
                BuyerPortalPermissionMapper.class.getClassLoader(),
                new Class<?>[] { BuyerPortalPermissionMapper.class }, handler);
        }
    }
}
