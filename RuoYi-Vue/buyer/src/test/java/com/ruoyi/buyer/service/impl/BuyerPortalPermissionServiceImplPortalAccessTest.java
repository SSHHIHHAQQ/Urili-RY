package com.ruoyi.buyer.service.impl;

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
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.mapper.BuyerPortalPermissionMapper;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalPermissionInfo;
import com.ruoyi.system.service.support.PartnerSupport;

public class BuyerPortalPermissionServiceImplPortalAccessTest
{
    @Test
    public void selectPortalPermissionInfoRejectsLockedBuyerAccountSession()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        account.setLockStatus(PartnerSupport.ACCOUNT_LOCK_STATUS_LOCKED);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                new RecordingBuyerPortalPermissionMapper().proxy());

        assertServiceException("买家端账号已锁定", () -> service.selectPortalPermissionInfo(session(11L, 22L)));
    }

    @Test
    public void selectPortalPermissionInfoRejectsMalformedBuyerSessionBeforeLookup()
    {
        BuyerPortalPermissionServiceImpl service = service(failOnLookup(IBuyerService.class, "buyer service"),
                failOnLookup(BuyerMapper.class, "buyer mapper"), new RecordingBuyerPortalPermissionMapper().proxy());

        assertUnauthorized(() -> service.selectPortalPermissionInfo(null));

        PortalLoginSession wrongTerminal = session(11L, 22L);
        wrongTerminal.setTerminal("seller");
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
    public void selectPortalPermissionInfoRejectsOfflineBuyerSession()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerMapper buyerMapper = recordingBuyerMapper(0, account);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper.proxy(),
                new RecordingBuyerPortalPermissionMapper().proxy());

        assertUnauthorized(() -> service.selectPortalPermissionInfo(session(11L, 22L)));
        assertOnlineBuyerSessionLookup(buyerMapper);
    }

    @Test
    public void selectPortalPermissionInfoReturnsBuyerRolesPermissionsAndChecksOnlineSession()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        account.setUserName("buyer_owner");
        account.setNickName("Buyer Owner");
        RecordingBuyerMapper buyerMapper = recordingBuyerMapper(1, account);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper()
                .withRoleKeys("buyer_owner", "buyer_staff")
                .withPermissions("buyer:product:list,buyer:order:list", "buyer:order:detail");
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper.proxy(),
                permissionMapper.proxy());
        PortalLoginSession session = session(11L, 22L);
        session.setSubjectNo("B0001");

        PortalPermissionInfo info = service.selectPortalPermissionInfo(session);

        assertEquals("buyer", info.getTerminal());
        assertEquals(Long.valueOf(11L), info.getSubjectId());
        assertEquals("B0001", info.getSubjectNo());
        assertEquals(Long.valueOf(22L), info.getAccountId());
        assertEquals("buyer_owner", info.getUserName());
        assertEquals("Buyer Owner", info.getNickName());
        assertArrayEquals(new String[] { "buyer_owner", "buyer_staff" }, info.getRoles().toArray(new String[0]));
        assertArrayEquals(new String[] { "buyer:product:list", "buyer:order:list", "buyer:order:detail" },
                info.getPermissions().toArray(new String[0]));
        assertOnlineBuyerSessionLookup(buyerMapper);
        assertBuyerPermissionLookup(permissionMapper);
    }

    @Test
    public void selectPermissionsRejectsMalformedBuyerSessionBeforeLookup()
    {
        BuyerPortalPermissionServiceImpl service = service(failOnLookup(IBuyerService.class, "buyer service"),
                failOnLookup(BuyerMapper.class, "buyer mapper"), new RecordingBuyerPortalPermissionMapper().proxy());

        PortalLoginSession wrongTerminal = session(11L, 22L);
        wrongTerminal.setTerminal("seller");
        assertUnauthorized(() -> service.selectPermissions(wrongTerminal));

        PortalLoginSession blankToken = session(11L, 22L);
        blankToken.setTokenId(" ");
        assertUnauthorized(() -> service.selectPermissions(blankToken));
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

    private PortalLoginSession session(Long buyerId, Long accountId)
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("buyer");
        session.setSubjectId(buyerId);
        session.setAccountId(accountId);
        session.setTokenId("buyer_test_token");
        return session;
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
                return "BuyerPortalPermissionServiceImplPortalAccessTestBuyerService";
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
        return recordingBuyerMapper(1, accounts).proxy();
    }

    private RecordingBuyerMapper recordingBuyerMapper(int onlineSessionCount, BuyerAccount... accounts)
    {
        return new RecordingBuyerMapper(onlineSessionCount, accounts);
    }

    private void assertOnlineBuyerSessionLookup(RecordingBuyerMapper buyerMapper)
    {
        assertEquals(1, buyerMapper.countOnlineSessionCallCount);
        assertEquals(Long.valueOf(11L), buyerMapper.countedBuyerId);
        assertEquals(Long.valueOf(22L), buyerMapper.countedBuyerAccountId);
        assertEquals("buyer_test_token", buyerMapper.countedTokenId);
    }

    private void assertBuyerPermissionLookup(RecordingBuyerPortalPermissionMapper permissionMapper)
    {
        assertEquals(1, permissionMapper.selectRoleKeysCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedRoleKeysBuyerId);
        assertEquals(Long.valueOf(22L), permissionMapper.selectedRoleKeysAccountId);
        assertEquals(1, permissionMapper.selectPermissionsCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedPermissionsBuyerId);
        assertEquals(Long.valueOf(22L), permissionMapper.selectedPermissionsAccountId);
    }

    private class RecordingBuyerMapper
    {
        private final int onlineSessionCount;

        private final Map<Long, BuyerAccount> accountById = new HashMap<>();

        private int countOnlineSessionCallCount;

        private Long countedBuyerId;

        private Long countedBuyerAccountId;

        private String countedTokenId;

        private RecordingBuyerMapper(int onlineSessionCount, BuyerAccount... accounts)
        {
            this.onlineSessionCount = onlineSessionCount;
            for (BuyerAccount account : accounts)
            {
                accountById.put(account.getBuyerAccountId(), account);
            }
        }

        private BuyerMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectBuyerAccountById".equals(methodName))
                {
                    return accountById.get((Long) args[0]);
                }
                if ("countOnlineBuyerSession".equals(methodName))
                {
                    countOnlineSessionCallCount++;
                    countedBuyerId = (Long) args[0];
                    countedBuyerAccountId = (Long) args[1];
                    countedTokenId = (String) args[2];
                    return onlineSessionCount;
                }
                if ("toString".equals(methodName))
                {
                    return "BuyerPortalPermissionServiceImplPortalAccessTestBuyerMapper";
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
    }

    @SuppressWarnings("unchecked")
    private <T> T failOnLookup(Class<T> type, String label)
    {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("toString".equals(methodName))
            {
                return "BuyerPortalPermissionServiceImplPortalAccessTest" + label;
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            throw new AssertionError(label + " must not be called for malformed buyer session");
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

    private class RecordingBuyerPortalPermissionMapper
    {
        private List<String> roleKeys = Collections.emptyList();

        private List<String> permissions = Collections.emptyList();

        private int selectRoleKeysCallCount;

        private Long selectedRoleKeysBuyerId;

        private Long selectedRoleKeysAccountId;

        private int selectPermissionsCallCount;

        private Long selectedPermissionsBuyerId;

        private Long selectedPermissionsAccountId;

        private RecordingBuyerPortalPermissionMapper withRoleKeys(String... roleKeys)
        {
            this.roleKeys = Arrays.asList(roleKeys);
            return this;
        }

        private RecordingBuyerPortalPermissionMapper withPermissions(String... permissions)
        {
            this.permissions = Arrays.asList(permissions);
            return this;
        }

        private BuyerPortalPermissionMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectBuyerAccountRoleKeys".equals(methodName))
                {
                    selectRoleKeysCallCount++;
                    selectedRoleKeysBuyerId = (Long) args[0];
                    selectedRoleKeysAccountId = (Long) args[1];
                    return roleKeys;
                }
                if ("selectBuyerAccountPermissions".equals(methodName))
                {
                    selectPermissionsCallCount++;
                    selectedPermissionsBuyerId = (Long) args[0];
                    selectedPermissionsAccountId = (Long) args[1];
                    return permissions;
                }
                if ("toString".equals(methodName))
                {
                    return "BuyerPortalPermissionServiceImplPortalAccessTestPermissionMapper";
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
