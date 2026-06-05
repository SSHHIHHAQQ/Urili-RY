package com.ruoyi.seller.service.impl;

import static org.junit.Assert.assertArrayEquals;
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
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.mapper.SellerPortalPermissionMapper;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.service.support.PartnerSupport;

public class SellerPortalPermissionServiceImplTest
{
    @Test
    public void assignAccountRolesKeepsSellerAccountAndRoleScope()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(2);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { 101L, null, 102L, 101L, 0L });

        assertEquals(2, rows);
        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.countedSellerId);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.countedRoleIds);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(Long.valueOf(22L), permissionMapper.deletedAccountId);
        assertEquals(1, permissionMapper.batchAccountRolesCallCount);
        assertEquals(Long.valueOf(22L), permissionMapper.batchedAccountId);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.batchedRoleIds);
    }

    @Test
    public void assignAccountRolesClearsRolesWhenNoRoleSelected()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(0);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { null, 0L });

        assertEquals(1, rows);
        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(Long.valueOf(22L), permissionMapper.deletedAccountId);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsAccountFromAnotherSellerBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 99L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L }));

        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsRolesFromAnotherSellerBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    private SellerPortalPermissionServiceImpl service(ISellerService sellerService, SellerMapper sellerMapper,
            SellerPortalPermissionMapper permissionMapper)
    {
        SellerPortalPermissionServiceImpl service = new SellerPortalPermissionServiceImpl();
        setField(service, "sellerService", sellerService);
        setField(service, "sellerMapper", sellerMapper);
        setField(service, "permissionMapper", permissionMapper);
        return service;
    }

    private Seller seller(Long sellerId)
    {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setStatus(PartnerSupport.STATUS_NORMAL);
        return seller;
    }

    private SellerAccount account(Long accountId, Long sellerId)
    {
        SellerAccount account = new SellerAccount();
        account.setSellerAccountId(accountId);
        account.setAccountId(accountId);
        account.setSellerId(sellerId);
        account.setStatus(PartnerSupport.STATUS_NORMAL);
        return account;
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
                return "SellerPortalPermissionServiceImplTestSellerService";
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

    private SellerMapper sellerMapper(SellerAccount... accounts)
    {
        Map<Long, SellerAccount> accountById = new HashMap<>();
        for (SellerAccount account : accounts)
        {
            accountById.put(account.getSellerAccountId(), account);
        }
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("selectSellerAccountById".equals(methodName))
            {
                return accountById.get((Long) args[0]);
            }
            if ("toString".equals(methodName))
            {
                return "SellerPortalPermissionServiceImplTestSellerMapper";
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
        return (SellerMapper) Proxy.newProxyInstance(
            SellerMapper.class.getClassLoader(), new Class<?>[] { SellerMapper.class }, handler);
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

    private class RecordingSellerPortalPermissionMapper
    {
        private final int validRoleCount;

        private int countRolesCallCount;

        private Long countedSellerId;

        private Long[] countedRoleIds;

        private int deleteAccountRolesCallCount;

        private Long deletedAccountId;

        private int batchAccountRolesCallCount;

        private Long batchedAccountId;

        private Long[] batchedRoleIds;

        private RecordingSellerPortalPermissionMapper(int validRoleCount)
        {
            this.validRoleCount = validRoleCount;
        }

        private SellerPortalPermissionMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("countSellerRolesByIds".equals(methodName))
                {
                    countRolesCallCount++;
                    countedSellerId = (Long) args[0];
                    countedRoleIds = (Long[]) args[1];
                    return validRoleCount;
                }
                if ("deleteSellerAccountRoles".equals(methodName))
                {
                    deleteAccountRolesCallCount++;
                    deletedAccountId = (Long) args[0];
                    return 1;
                }
                if ("batchSellerAccountRoles".equals(methodName))
                {
                    batchAccountRolesCallCount++;
                    batchedAccountId = (Long) args[0];
                    batchedRoleIds = (Long[]) args[1];
                    return batchedRoleIds.length;
                }
                if ("toString".equals(methodName))
                {
                    return "SellerPortalPermissionServiceImplTestPermissionMapper";
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
            return (SellerPortalPermissionMapper) Proxy.newProxyInstance(
                SellerPortalPermissionMapper.class.getClassLoader(),
                new Class<?>[] { SellerPortalPermissionMapper.class }, handler);
        }
    }
}
