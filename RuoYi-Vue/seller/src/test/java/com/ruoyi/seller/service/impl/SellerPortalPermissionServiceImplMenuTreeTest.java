package com.ruoyi.seller.service.impl;

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
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.service.support.PartnerSupport;

public class SellerPortalPermissionServiceImplMenuTreeTest
{
    @Test
    public void selectPortalMenuTreeRejectsOfflineSellerSession()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerMapper sellerMapper = recordingSellerMapper(0, account);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper();
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper.proxy(),
                permissionMapper.proxy());

        assertUnauthorized(() -> service.selectPortalMenuTree(session(11L, 22L)));

        assertOnlineSellerSessionLookup(sellerMapper);
        assertEquals(0, permissionMapper.selectMenuListCallCount);
    }

    @Test
    public void selectPortalMenuTreeReturnsSellerMenuTreeAndChecksOnlineSession()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerMapper sellerMapper = recordingSellerMapper(1, account);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper()
                .withMenus(menu(100L, 0L, "商品中心"), menu(101L, 100L, "商品列表"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper.proxy(),
                permissionMapper.proxy());

        List<PortalMenu> menuTree = service.selectPortalMenuTree(session(11L, 22L));

        assertOnlineSellerSessionLookup(sellerMapper);
        assertSellerMenuLookup(permissionMapper);
        assertEquals(1, menuTree.size());
        assertEquals(Long.valueOf(100L), menuTree.get(0).getMenuId());
        assertEquals("商品中心", menuTree.get(0).getMenuName());
        assertEquals(1, menuTree.get(0).getChildren().size());
        assertEquals(Long.valueOf(101L), menuTree.get(0).getChildren().get(0).getMenuId());
        assertEquals("商品列表", menuTree.get(0).getChildren().get(0).getMenuName());
    }

    @Test
    public void updateMenuRejectsMovingSellerMenuUnderItsDescendant()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper()
                .withMenuIndex(menu(10L, 0L, "商品中心"), menu(11L, 10L, "商品列表"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), recordingSellerMapper(1, account).proxy(),
                permissionMapper.proxy());
        PortalMenu payload = menu(10L, 11L, "商品中心");

        assertServiceException(() -> service.updateMenu(payload));

        assertEquals(0, permissionMapper.updateMenuCallCount);
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

    private PortalMenu menu(Long menuId, Long parentId, String menuName)
    {
        PortalMenu menu = new PortalMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        return menu;
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
                return "SellerPortalPermissionServiceImplMenuTreeTestSellerService";
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

    private void assertSellerMenuLookup(RecordingSellerPortalPermissionMapper permissionMapper)
    {
        assertEquals(1, permissionMapper.selectMenuListCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedMenuListSellerId);
        assertEquals(Long.valueOf(22L), permissionMapper.selectedMenuListAccountId);
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
                    return "SellerPortalPermissionServiceImplMenuTreeTestSellerMapper";
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
        private List<PortalMenu> menus = Collections.emptyList();

        private final Map<Long, PortalMenu> menuById = new HashMap<>();

        private int selectMenuListCallCount;

        private Long selectedMenuListSellerId;

        private Long selectedMenuListAccountId;

        private int updateMenuCallCount;

        private RecordingSellerPortalPermissionMapper withMenus(PortalMenu... menus)
        {
            this.menus = Arrays.asList(menus);
            return this;
        }

        private RecordingSellerPortalPermissionMapper withMenuIndex(PortalMenu... menus)
        {
            for (PortalMenu menu : menus)
            {
                menuById.put(menu.getMenuId(), menu);
            }
            return this;
        }

        private SellerPortalPermissionMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectSellerAccountMenuList".equals(methodName))
                {
                    selectMenuListCallCount++;
                    selectedMenuListSellerId = (Long) args[0];
                    selectedMenuListAccountId = (Long) args[1];
                    return menus;
                }
                if ("selectSellerMenuById".equals(methodName))
                {
                    return menuById.get((Long) args[0]);
                }
                if ("updateSellerMenu".equals(methodName))
                {
                    updateMenuCallCount++;
                    return 1;
                }
                if ("toString".equals(methodName))
                {
                    return "SellerPortalPermissionServiceImplMenuTreeTestPermissionMapper";
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
}
