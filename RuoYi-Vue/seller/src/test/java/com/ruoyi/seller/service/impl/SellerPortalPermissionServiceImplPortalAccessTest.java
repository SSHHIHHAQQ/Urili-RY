package com.ruoyi.seller.service.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.mapper.SellerPortalPermissionMapper;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalPermissionInfo;
import com.ruoyi.system.service.support.PartnerSupport;

public class SellerPortalPermissionServiceImplPortalAccessTest
{
    @Test
    public void selectPortalPermissionInfoRejectsLockedSellerAccountSession()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        account.setLockStatus(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                new RecordingSellerPortalPermissionMapper().proxy());

        assertServiceException("卖家端账号已锁定", () -> service.selectPortalPermissionInfo(session(11L, 22L)));
    }

    @Test
    public void selectPortalPermissionInfoRejectsMalformedSellerSessionBeforeLookup()
    {
        SellerPortalPermissionServiceImpl service = service(failOnLookup(ISellerService.class, "seller service"),
                failOnLookup(SellerMapper.class, "seller mapper"), new RecordingSellerPortalPermissionMapper().proxy());

        assertUnauthorized(() -> service.selectPortalPermissionInfo(null));

        PortalLoginSession wrongTerminal = session(11L, 22L);
        wrongTerminal.setTerminal("buyer");
        assertUnauthorized(() -> service.selectPortalPermissionInfo(wrongTerminal));

        PortalLoginSession missingSubject = session(11L, 22L);
        missingSubject.setSubjectId(null);
        assertUnauthorized(() -> service.selectPortalPermissionInfo(missingSubject));

        PortalLoginSession missingAccount = session(11L, 22L);
        missingAccount.setAccountId(null);
        assertUnauthorized(() -> service.selectPortalPermissionInfo(missingAccount));

        PortalLoginSession missingToken = session(11L, 22L);
        missingToken.setTokenId(null);
        assertUnauthorized(() -> service.selectPortalPermissionInfo(missingToken));

        PortalLoginSession blankToken = session(11L, 22L);
        blankToken.setTokenId(" ");
        assertUnauthorized(() -> service.selectPortalPermissionInfo(blankToken));
    }

    @Test
    public void selectPortalPermissionInfoRejectsOfflineSellerSession()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerMapper sellerMapper = recordingSellerMapper(0, account);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper.proxy(),
                new RecordingSellerPortalPermissionMapper().proxy());

        assertUnauthorized(() -> service.selectPortalPermissionInfo(session(11L, 22L)));
        assertOnlineSellerSessionLookup(sellerMapper);
    }

    @Test
    public void selectPortalPermissionInfoReturnsSellerRolesPermissionsAndChecksOnlineSession()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        account.setUserName("seller_owner");
        account.setNickName("Seller Owner");
        RecordingSellerMapper sellerMapper = recordingSellerMapper(1, account);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper()
                .withRoleKeys("seller_owner", "seller_staff")
                .withPermissions("seller:product:list,seller:order:list", "seller:order:detail");
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper.proxy(),
                permissionMapper.proxy());
        PortalLoginSession session = session(11L, 22L);
        session.setSubjectNo("S0001");

        PortalPermissionInfo info = service.selectPortalPermissionInfo(session);

        assertEquals("seller", info.getTerminal());
        assertEquals(Long.valueOf(11L), info.getSubjectId());
        assertEquals("S0001", info.getSubjectNo());
        assertEquals(Long.valueOf(22L), info.getAccountId());
        assertEquals("seller_owner", info.getUserName());
        assertEquals("Seller Owner", info.getNickName());
        assertArrayEquals(new String[] { "seller_owner", "seller_staff" }, info.getRoles().toArray(new String[0]));
        assertArrayEquals(new String[] { "seller:product:list", "seller:order:list", "seller:order:detail" },
                info.getPermissions().toArray(new String[0]));
        assertOnlineSellerSessionLookup(sellerMapper);
        assertSellerPermissionLookup(permissionMapper);
    }

    @Test
    public void selectPortalPermissionInfoTrimsSellerPermissionsAndRejectsPollutedPrefixes()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerMapper sellerMapper = recordingSellerMapper(1, account);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper()
                .withRoleKeys("seller_owner")
                .withPermissions(" seller:product:list, seller:order:list ", "seller:order:detail,, ");
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper.proxy(),
                permissionMapper.proxy());

        PortalPermissionInfo info = service.selectPortalPermissionInfo(session(11L, 22L));

        assertArrayEquals(new String[] { "seller:product:list", "seller:order:list", "seller:order:detail" },
                info.getPermissions().toArray(new String[0]));

        for (String pollutedPermission : new String[] { "buyer:account:list", "seller:admin:list", "*:*:*" })
        {
            RecordingSellerPortalPermissionMapper pollutedMapper = new RecordingSellerPortalPermissionMapper()
                    .withPermissions("seller:product:list", pollutedPermission);
            SellerPortalPermissionServiceImpl pollutedService = service(sellerService(seller),
                    recordingSellerMapper(1, account).proxy(), pollutedMapper.proxy());
            assertServiceException("卖家端权限配置异常",
                    () -> pollutedService.selectPortalPermissionInfo(session(11L, 22L)));
        }
    }

    @Test
    public void selectPermissionsRejectsMalformedSellerSessionBeforeLookup()
    {
        SellerPortalPermissionServiceImpl service = service(failOnLookup(ISellerService.class, "seller service"),
                failOnLookup(SellerMapper.class, "seller mapper"), new RecordingSellerPortalPermissionMapper().proxy());

        PortalLoginSession wrongTerminal = session(11L, 22L);
        wrongTerminal.setTerminal("buyer");
        assertUnauthorized(() -> service.selectPermissions(wrongTerminal));

        PortalLoginSession blankToken = session(11L, 22L);
        blankToken.setTokenId(" ");
        assertUnauthorized(() -> service.selectPermissions(blankToken));
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

    private PortalLoginSession session(Long sellerId, Long accountId)
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("seller");
        session.setSubjectId(sellerId);
        session.setAccountId(accountId);
        session.setTokenId("seller_test_token");
        return session;
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
                return "SellerPortalPermissionServiceImplPortalAccessTestSellerService";
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
        return recordingSellerMapper(1, accounts).proxy();
    }

    private RecordingSellerMapper recordingSellerMapper(int onlineSessionCount, SellerAccount... accounts)
    {
        return new RecordingSellerMapper(onlineSessionCount, accounts);
    }

    private void assertOnlineSellerSessionLookup(RecordingSellerMapper sellerMapper)
    {
        assertEquals(1, sellerMapper.countOnlineSessionCallCount);
        assertEquals(Long.valueOf(11L), sellerMapper.countedSellerId);
        assertEquals(Long.valueOf(22L), sellerMapper.countedSellerAccountId);
        assertEquals("seller_test_token", sellerMapper.countedTokenId);
    }

    private void assertSellerPermissionLookup(RecordingSellerPortalPermissionMapper permissionMapper)
    {
        assertEquals(1, permissionMapper.selectRoleKeysCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedRoleKeysSellerId);
        assertEquals(Long.valueOf(22L), permissionMapper.selectedRoleKeysAccountId);
        assertEquals(1, permissionMapper.selectPermissionsCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedPermissionsSellerId);
        assertEquals(Long.valueOf(22L), permissionMapper.selectedPermissionsAccountId);
    }

    private class RecordingSellerMapper
    {
        private final int onlineSessionCount;

        private final Map<Long, SellerAccount> accountById = new HashMap<>();

        private int countOnlineSessionCallCount;

        private Long countedSellerId;

        private Long countedSellerAccountId;

        private String countedTokenId;

        private RecordingSellerMapper(int onlineSessionCount, SellerAccount... accounts)
        {
            this.onlineSessionCount = onlineSessionCount;
            for (SellerAccount account : accounts)
            {
                accountById.put(account.getSellerAccountId(), account);
            }
        }

        private SellerMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectSellerAccountById".equals(methodName))
                {
                    return accountById.get((Long) args[0]);
                }
                if ("selectSellerAccountByIdAndSellerId".equals(methodName))
                {
                    Long sellerId = (Long) args[0];
                    SellerAccount account = accountById.get((Long) args[1]);
                    return account != null && sellerId.equals(account.getSellerId()) ? account : null;
                }
                if ("countOnlineSellerSession".equals(methodName))
                {
                    countOnlineSessionCallCount++;
                    countedSellerId = (Long) args[0];
                    countedSellerAccountId = (Long) args[1];
                    countedTokenId = (String) args[2];
                    return onlineSessionCount;
                }
                if ("toString".equals(methodName))
                {
                    return "SellerPortalPermissionServiceImplPortalAccessTestSellerMapper";
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
    }

    @SuppressWarnings("unchecked")
    private <T> T failOnLookup(Class<T> type, String label)
    {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("toString".equals(methodName))
            {
                return "SellerPortalPermissionServiceImplPortalAccessTest" + label;
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            throw new AssertionError(label + " must not be called for malformed seller session");
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private void assertServiceException(String message, ThrowingRunnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (ServiceException e)
        {
            assertEquals(message, e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    private void assertUnauthorized(ThrowingRunnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (ServiceException e)
        {
            assertEquals(Integer.valueOf(HttpStatus.UNAUTHORIZED), e.getCode());
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
        private List<String> roleKeys = Collections.emptyList();

        private List<String> permissions = Collections.emptyList();

        private int selectRoleKeysCallCount;

        private Long selectedRoleKeysSellerId;

        private Long selectedRoleKeysAccountId;

        private int selectPermissionsCallCount;

        private Long selectedPermissionsSellerId;

        private Long selectedPermissionsAccountId;

        private RecordingSellerPortalPermissionMapper withRoleKeys(String... roleKeys)
        {
            this.roleKeys = Arrays.asList(roleKeys);
            return this;
        }

        private RecordingSellerPortalPermissionMapper withPermissions(String... permissions)
        {
            this.permissions = Arrays.asList(permissions);
            return this;
        }

        private SellerPortalPermissionMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectSellerAccountRoleKeys".equals(methodName))
                {
                    selectRoleKeysCallCount++;
                    selectedRoleKeysSellerId = (Long) args[0];
                    selectedRoleKeysAccountId = (Long) args[1];
                    return roleKeys;
                }
                if ("selectSellerAccountPermissions".equals(methodName))
                {
                    selectPermissionsCallCount++;
                    selectedPermissionsSellerId = (Long) args[0];
                    selectedPermissionsAccountId = (Long) args[1];
                    return permissions;
                }
                if ("toString".equals(methodName))
                {
                    return "SellerPortalPermissionServiceImplPortalAccessTestPermissionMapper";
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
