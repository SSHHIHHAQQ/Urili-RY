package com.ruoyi.buyer.service.impl;

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
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.service.support.PartnerSupport;

public class BuyerPortalPermissionServiceImplMenuTreeTest
{
    @Test
    public void selectPortalMenuTreeRejectsOfflineBuyerSession()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerMapper buyerMapper = recordingBuyerMapper(0, account);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper();
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper.proxy(),
                permissionMapper.proxy());

        assertUnauthorized(() -> service.selectPortalMenuTree(session(11L, 22L)));

        assertOnlineBuyerSessionLookup(buyerMapper);
        assertEquals(0, permissionMapper.selectMenuListCallCount);
    }

    @Test
    public void selectPortalMenuTreeReturnsBuyerMenuTreeAndChecksOnlineSession()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerMapper buyerMapper = recordingBuyerMapper(1, account);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper()
                .withMenus(buyerPageMenu(200000L, 0L, "门户首页", "buyer:portal:home"),
                        buyerPageMenu(200001L, 200000L, "商品列表", "buyer:product:list"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper.proxy(),
                permissionMapper.proxy());

        List<PortalMenu> menuTree = service.selectPortalMenuTree(session(11L, 22L));

        assertOnlineBuyerSessionLookup(buyerMapper);
        assertBuyerMenuLookup(permissionMapper);
        assertEquals(1, menuTree.size());
        assertEquals(Long.valueOf(200000L), menuTree.get(0).getMenuId());
        assertEquals("门户首页", menuTree.get(0).getMenuName());
        assertEquals(0, menuTree.get(0).getChildren().size());
    }

    @Test
    public void selectPortalMenuTreeRejectsDirtyBuyerTerminalMenus()
    {
        assertBuyerPortalMenuRejected(buyerPageMenu(100000L, 0L, "wrong id", "buyer:product:list"));
        assertBuyerPortalMenuRejected(buyerPageMenu(200000L, 0L, "cross perms", "seller:product:list"));
        assertBuyerPortalMenuRejected(buyerPageMenu(200000L, 0L, "admin perms", "buyer:admin:list"));
        assertBuyerPortalMenuRejected(buyerPageMenu(200000L, 0L, "wildcard perms", "*:*:*"));
        assertBuyerPortalMenuRejected(buyerPageMenu(200000L, 0L, "blank perms", ""));

        PortalMenu crossComponent = buyerPageMenu(200000L, 0L, "cross component", "buyer:product:list");
        crossComponent.setComponent("seller/product/list");
        assertBuyerPortalMenuRejected(crossComponent);

        PortalMenu commonComponent = buyerPageMenu(200000L, 0L, "common component", "buyer:product:list");
        commonComponent.setComponent("common/placeholder");
        assertBuyerPortalMenuRejected(commonComponent);

        PortalMenu blankComponent = buyerPageMenu(200000L, 0L, "blank component", "buyer:product:list");
        blankComponent.setComponent("");
        assertBuyerPortalMenuRejected(blankComponent);

        PortalMenu blankType = buyerPageMenu(200000L, 0L, "blank type", "buyer:product:list");
        blankType.setMenuType("");
        assertBuyerPortalMenuRejected(blankType);
    }

    @Test
    public void updateMenuRejectsMovingBuyerMenuUnderItsDescendant()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper()
                .withMenuIndex(menu(10L, 0L, "采购中心"), menu(11L, 10L, "商品列表"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), recordingBuyerMapper(1, account).proxy(),
                permissionMapper.proxy());
        PortalMenu payload = menu(10L, 11L, "采购中心");
        markValidBuyerPageMenu(payload, "buyer:product:list");

        assertServiceException(() -> service.updateMenu(payload));

        assertEquals(0, permissionMapper.updateMenuCallCount);
    }

    @Test
    public void insertMenuRejectsWildcardAdminAndCrossTerminalPermsBeforeMapperWrite()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper();
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), recordingBuyerMapper(1, account).proxy(),
                permissionMapper.proxy());

        for (String invalidPerm : new String[] { "*:*:*", "buyer:admin:list", "seller:account:list" })
        {
            PortalMenu payload = menu(null, 0L, "invalid " + invalidPerm);
            markValidBuyerPageMenu(payload, invalidPerm);
            payload.setPerms(invalidPerm);
            assertServiceException(() -> service.insertMenu(payload));
        }

        assertEquals(0, permissionMapper.insertMenuCallCount);
    }

    @Test
    public void insertMenuRejectsBlankPermsAndInvalidComponentBeforeMapperWrite()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper();
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), recordingBuyerMapper(1, account).proxy(),
                permissionMapper.proxy());

        for (String invalidComponent : new String[] { "", "seller/product/list", "admin/system/list", "Product/List" })
        {
            PortalMenu payload = menu(null, 0L, "invalid component");
            markValidBuyerPageMenu(payload, "buyer:product:list");
            payload.setComponent(invalidComponent);
            assertServiceException(() -> service.insertMenu(payload));
        }

        PortalMenu blankPagePerms = menu(null, 0L, "blank page perms");
        markValidBuyerPageMenu(blankPagePerms, "");
        assertServiceException(() -> service.insertMenu(blankPagePerms));

        PortalMenu blankButtonPerms = menu(null, 0L, "blank button perms");
        blankButtonPerms.setMenuType("F");
        blankButtonPerms.setPerms("");
        assertServiceException(() -> service.insertMenu(blankButtonPerms));

        assertEquals(0, permissionMapper.insertMenuCallCount);
    }

    @Test
    public void updateMenuRejectsWildcardAdminAndCrossTerminalPermsBeforeMapperWrite()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper()
                .withMenuIndex(menu(10L, 0L, "采购中心"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), recordingBuyerMapper(1, account).proxy(),
                permissionMapper.proxy());

        for (String invalidPerm : new String[] { "*:*:*", "buyer:admin:list", "seller:account:list" })
        {
            PortalMenu payload = menu(10L, 0L, "invalid " + invalidPerm);
            markValidBuyerPageMenu(payload, invalidPerm);
            payload.setPerms(invalidPerm);
            assertServiceException(() -> service.updateMenu(payload));
        }

        assertEquals(0, permissionMapper.updateMenuCallCount);
    }

    @Test
    public void updateMenuRejectsBlankPermsAndInvalidComponentBeforeMapperWrite()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper()
                .withMenuIndex(menu(10L, 0L, "采购中心"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), recordingBuyerMapper(1, account).proxy(),
                permissionMapper.proxy());

        for (String invalidComponent : new String[] { "", "seller/product/list", "admin/system/list", "Product/List" })
        {
            PortalMenu payload = menu(10L, 0L, "invalid component");
            markValidBuyerPageMenu(payload, "buyer:product:list");
            payload.setComponent(invalidComponent);
            assertServiceException(() -> service.updateMenu(payload));
        }

        PortalMenu blankPagePerms = menu(10L, 0L, "blank page perms");
        markValidBuyerPageMenu(blankPagePerms, "");
        assertServiceException(() -> service.updateMenu(blankPagePerms));

        PortalMenu blankButtonPerms = menu(10L, 0L, "blank button perms");
        blankButtonPerms.setMenuType("F");
        blankButtonPerms.setPerms("");
        assertServiceException(() -> service.updateMenu(blankButtonPerms));

        assertEquals(0, permissionMapper.updateMenuCallCount);
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

    private PortalMenu menu(Long menuId, Long parentId, String menuName)
    {
        PortalMenu menu = new PortalMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        menu.setMenuType("M");
        return menu;
    }

    private PortalMenu buyerPageMenu(Long menuId, Long parentId, String menuName, String perms)
    {
        PortalMenu menu = menu(menuId, parentId, menuName);
        markValidBuyerPageMenu(menu, perms);
        return menu;
    }

    private void markValidBuyerPageMenu(PortalMenu menu, String perms)
    {
        menu.setMenuType("C");
        menu.setComponent("buyer/product/list");
        menu.setPerms(perms);
    }

    private void assertBuyerPortalMenuRejected(PortalMenu dirtyMenu)
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerMapper buyerMapper = recordingBuyerMapper(1, account);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper()
                .withMenus(dirtyMenu);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper.proxy(),
                permissionMapper.proxy());

        assertServiceException(() -> service.selectPortalMenuTree(session(11L, 22L)));

        assertOnlineBuyerSessionLookup(buyerMapper);
        assertBuyerMenuLookup(permissionMapper);
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
                return "BuyerPortalPermissionServiceImplMenuTreeTestBuyerService";
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

    private void assertBuyerMenuLookup(RecordingBuyerPortalPermissionMapper permissionMapper)
    {
        assertEquals(1, permissionMapper.selectMenuListCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedMenuListBuyerId);
        assertEquals(Long.valueOf(22L), permissionMapper.selectedMenuListAccountId);
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
                if ("selectBuyerAccountByIdAndBuyerId".equals(methodName))
                {
                    Long buyerId = (Long) args[0];
                    BuyerAccount account = accountById.get((Long) args[1]);
                    return account != null && buyerId.equals(account.getBuyerId()) ? account : null;
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
                    return "BuyerPortalPermissionServiceImplMenuTreeTestBuyerMapper";
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
        private List<PortalMenu> menus = Collections.emptyList();

        private final Map<Long, PortalMenu> menuById = new HashMap<>();

        private int selectMenuListCallCount;

        private Long selectedMenuListBuyerId;

        private Long selectedMenuListAccountId;

        private int updateMenuCallCount;

        private int insertMenuCallCount;

        private RecordingBuyerPortalPermissionMapper withMenus(PortalMenu... menus)
        {
            this.menus = Arrays.asList(menus);
            return this;
        }

        private RecordingBuyerPortalPermissionMapper withMenuIndex(PortalMenu... menus)
        {
            for (PortalMenu menu : menus)
            {
                menuById.put(menu.getMenuId(), menu);
            }
            return this;
        }

        private BuyerPortalPermissionMapper proxy()
        {
            InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
                String methodName = method.getName();
                if ("selectBuyerAccountMenuList".equals(methodName))
                {
                    selectMenuListCallCount++;
                    selectedMenuListBuyerId = (Long) args[0];
                    selectedMenuListAccountId = (Long) args[1];
                    return menus;
                }
                if ("selectBuyerMenuById".equals(methodName))
                {
                    return menuById.get((Long) args[0]);
                }
                if ("updateBuyerMenu".equals(methodName))
                {
                    updateMenuCallCount++;
                    return 1;
                }
                if ("insertBuyerMenu".equals(methodName))
                {
                    insertMenuCallCount++;
                    return 1;
                }
                if ("toString".equals(methodName))
                {
                    return "BuyerPortalPermissionServiceImplMenuTreeTestPermissionMapper";
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
