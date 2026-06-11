package com.ruoyi.buyer.service.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.mapper.BuyerPortalPermissionMapper;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalRole;
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

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L });

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

        int rows = service.assignAccountRoles(11L, 22L, new Long[0]);

        assertEquals(1, rows);
        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.deletedBuyerId);
        assertEquals(Long.valueOf(22L), permissionMapper.deletedAccountId);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsInvalidRoleIdsBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(0);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, null, 0L }));

        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsDuplicateRoleIdsBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(0);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, 101L }));

        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsOwnerAccountWithoutOwnerRoleBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withOwnerRole(role(101L, 11L, "owner"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 102L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesAllowsOwnerAccountWhenOwnerRoleIsKept()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(2)
                .withOwnerRole(role(101L, 11L, "owner"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L });

        assertEquals(2, rows);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.batchedRoleIds);
    }

    @Test
    public void assignAccountRolesRejectsNonOwnerAccountWithOwnerRoleBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withOwnerRole(role(101L, 11L, "owner"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void updateRoleRejectsOwnerRoleBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "owner"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(101L, 11L, "owner-renamed");
        payload.setRoleName("Owner");

        assertServiceException(() -> service.updateRole(11L, payload));

        assertEquals(0, permissionMapper.updateRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
    }

    @Test
    public void updateRoleStatusRejectsOwnerRoleBeforeMutatingStatus()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "owner"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(101L, 11L, "owner");
        payload.setStatus("1");

        assertServiceException(() -> service.updateRoleStatus(11L, payload));

        assertEquals(0, permissionMapper.updateRoleStatusCallCount);
    }

    @Test
    public void deleteRoleRejectsOwnerRoleBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "owner"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.deleteRoleByIds(11L, new Long[] { 101L }));

        assertEquals(0, permissionMapper.countAccountRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.deleteRoleCallCount);
    }

    @Test
    public void deleteRoleRejectsInvalidRoleIdsBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.deleteRoleByIds(11L, new Long[] { 101L, null, 0L }));

        assertEquals(0, permissionMapper.selectRoleByIdCallCount);
        assertEquals(0, permissionMapper.countAccountRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.deleteRoleCallCount);
    }

    @Test
    public void deleteRoleRejectsDuplicateRoleIdsBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.deleteRoleByIds(11L, new Long[] { 101L, 101L }));

        assertEquals(0, permissionMapper.selectRoleByIdCallCount);
        assertEquals(0, permissionMapper.countAccountRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.deleteRoleCallCount);
    }

    @Test
    public void insertRoleRejectsMenuIdsOutsideBuyerTerminalBeforeMutatingRole()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withValidMenuCount(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(null, 11L, "staff");
        payload.setMenuIds(new Long[] { 301L, 302L });

        assertServiceException(() -> service.insertRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertArrayEquals(new Long[] { 301L, 302L }, permissionMapper.countedMenuIds);
        assertEquals(0, permissionMapper.insertRoleCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    @Test
    public void insertRoleRejectsInvalidBuyerMenuIdsBeforeMutatingRole()
    {
        assertInvalidBuyerRoleMenuIdsRejected(new Long[] { 301L, null });
        assertInvalidBuyerRoleMenuIdsRejected(new Long[] { 301L, 0L });
        assertInvalidBuyerRoleMenuIdsRejected(new Long[] { 301L, 301L });
    }

    @Test
    public void insertRoleRejectsDirtyBuyerMenusBeforeMutatingRole()
    {
        assertDirtyBuyerMenuRejected(menu(199999L, "F", "buyer:account:list", ""));
        assertDirtyBuyerMenuRejected(menu(200001L, "F", "buyer:admin:menu:list", ""));
        assertDirtyBuyerMenuRejected(menu(200002L, "C", "buyer:product:list", "common/ProductList"));
    }

    @Test
    public void insertRoleRejectsNonSelfManagementBuyerMenusBeforeMutatingRole()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        PortalMenu productMenu = menu(200003L, "F", "buyer:product:list", "");
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withValidMenuCount(1)
                .withSelectedMenus(productMenu);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(null, 11L, "staff");
        payload.setMenuIds(new Long[] { 200003L });

        assertServiceException(() -> service.insertRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
        assertEquals(0, permissionMapper.insertRoleCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    @Test
    public void selectMenuByIdRejectsMenuOutsideBuyerTerminalRange()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedMenus(menu(199999L, "M", "", ""));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.selectMenuById(199999L));

        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
    }

    @Test
    public void updateRoleRejectsMenuIdsOutsideBuyerTerminalBeforeMutatingRoleOrBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"))
                .withValidMenuCount(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(101L, 11L, "staff");
        payload.setMenuIds(new Long[] { 301L, 302L });

        assertServiceException(() -> service.updateRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertArrayEquals(new Long[] { 301L, 302L }, permissionMapper.countedMenuIds);
        assertEquals(0, permissionMapper.updateRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    @Test
    public void updateRoleRejectsInvalidBuyerMenuIdsBeforeMutatingRoleOrBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"));
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(101L, 11L, "staff");
        payload.setMenuIds(new Long[] { 301L, null });

        assertServiceException(() -> service.updateRole(11L, payload));

        assertEquals(0, permissionMapper.countMenusCallCount);
        assertEquals(0, permissionMapper.updateRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    @Test
    public void updateRoleRejectsNonSelfManagementBuyerMenusBeforeMutatingRoleOrBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        PortalMenu productMenu = menu(200003L, "F", "buyer:product:list", "");
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"))
                .withValidMenuCount(1)
                .withSelectedMenus(productMenu);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(101L, 11L, "staff");
        payload.setMenuIds(new Long[] { 200003L });

        assertServiceException(() -> service.updateRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
        assertEquals(0, permissionMapper.updateRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    @Test
    public void selectMenuIdsByRoleIdKeepsBuyerScopeForCheckedKeys()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"))
                .withSelectedMenuIds(301L, 302L);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        List<Long> checkedKeys = service.selectMenuIdsByRoleId(11L, 101L);

        assertEquals(Arrays.asList(301L, 302L), checkedKeys);
        assertEquals(1, permissionMapper.selectMenuIdsCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedMenuBuyerId);
        assertEquals(Long.valueOf(101L), permissionMapper.selectedMenuRoleId);
    }

    @Test
    public void selectMenuIdsByRoleIdRejectsRoleOutsideBuyerBeforeCheckedKeysLookup()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.selectMenuIdsByRoleId(11L, 101L));

        assertEquals(0, permissionMapper.selectMenuIdsCallCount);
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

    @Test
    public void assignAccountRolesRejectsDisabledRolesBeforeMutatingBindings()
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.countedRoleIds);
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

    private PortalRole role(Long roleId, Long buyerId, String roleKey)
    {
        PortalRole role = new PortalRole();
        role.setRoleId(roleId);
        role.setSubjectId(buyerId);
        role.setRoleName("Owner");
        role.setRoleKey(roleKey);
        role.setRoleSort(0);
        role.setStatus(PartnerSupport.STATUS_NORMAL);
        return role;
    }

    private PortalMenu menu(Long menuId, String menuType, String perms, String component)
    {
        PortalMenu menu = new PortalMenu();
        menu.setMenuId(menuId);
        menu.setMenuType(menuType);
        menu.setPerms(perms);
        menu.setComponent(component);
        return menu;
    }

    private void assertDirtyBuyerMenuRejected(PortalMenu menu)
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1)
                .withValidMenuCount(1)
                .withSelectedMenus(menu);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(null, 11L, "staff");
        payload.setMenuIds(new Long[] { menu.getMenuId() });

        assertServiceException(() -> service.insertRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
        assertEquals(0, permissionMapper.insertRoleCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    private void assertInvalidBuyerRoleMenuIdsRejected(Long[] menuIds)
    {
        Buyer buyer = buyer(11L);
        BuyerAccount account = account(22L, 11L);
        RecordingBuyerPortalPermissionMapper permissionMapper = new RecordingBuyerPortalPermissionMapper(1);
        BuyerPortalPermissionServiceImpl service = service(buyerService(buyer), buyerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(null, 11L, "staff");
        payload.setMenuIds(menuIds);

        assertServiceException(() -> service.insertRole(11L, payload));

        assertEquals(0, permissionMapper.countMenusCallCount);
        assertEquals(0, permissionMapper.insertRoleCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
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
            if ("selectBuyerAccountByIdAndBuyerId".equals(methodName))
            {
                Long buyerId = (Long) args[0];
                BuyerAccount account = accountById.get((Long) args[1]);
                return account != null && buyerId.equals(account.getBuyerId()) ? account : null;
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

        private int validMenuCount = -1;

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

        private PortalRole ownerRole;

        private PortalRole selectedRole;

        private int selectRoleByIdCallCount;

        private int updateRoleCallCount;

        private int deleteRoleMenuCallCount;

        private int updateRoleStatusCallCount;

        private int countAccountRoleCallCount;

        private int deleteRoleCallCount;

        private List<Long> selectedMenuIds;

        private Map<Long, PortalMenu> selectedMenus = new HashMap<>();

        private int selectMenuIdsCallCount;

        private int selectMenuByIdCallCount;

        private Long selectedMenuBuyerId;

        private Long selectedMenuRoleId;

        private int countMenusCallCount;

        private Long[] countedMenuIds;

        private int insertRoleCallCount;

        private int batchRoleMenuCallCount;

        private RecordingBuyerPortalPermissionMapper(int validRoleCount)
        {
            this.validRoleCount = validRoleCount;
        }

        private RecordingBuyerPortalPermissionMapper withOwnerRole(PortalRole ownerRole)
        {
            this.ownerRole = ownerRole;
            return this;
        }

        private RecordingBuyerPortalPermissionMapper withSelectedRole(PortalRole selectedRole)
        {
            this.selectedRole = selectedRole;
            return this;
        }

        private RecordingBuyerPortalPermissionMapper withSelectedMenuIds(Long... selectedMenuIds)
        {
            this.selectedMenuIds = Arrays.asList(selectedMenuIds);
            return this;
        }

        private RecordingBuyerPortalPermissionMapper withSelectedMenus(PortalMenu... selectedMenus)
        {
            this.selectedMenus = new HashMap<>();
            for (PortalMenu menu : selectedMenus)
            {
                this.selectedMenus.put(menu.getMenuId(), menu);
            }
            return this;
        }

        private RecordingBuyerPortalPermissionMapper withValidMenuCount(int validMenuCount)
        {
            this.validMenuCount = validMenuCount;
            return this;
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
                if ("countBuyerMenusByIds".equals(methodName))
                {
                    countMenusCallCount++;
                    countedMenuIds = (Long[]) args[0];
                    return validMenuCount >= 0 ? validMenuCount : countedMenuIds.length;
                }
                if ("checkBuyerRoleKeyUnique".equals(methodName))
                {
                    return ownerRole;
                }
                if ("selectBuyerRoleById".equals(methodName))
                {
                    selectRoleByIdCallCount++;
                    return selectedRole;
                }
                if ("selectBuyerMenuIdsByRoleId".equals(methodName))
                {
                    selectMenuIdsCallCount++;
                    selectedMenuBuyerId = (Long) args[0];
                    selectedMenuRoleId = (Long) args[1];
                    return selectedMenuIds;
                }
                if ("selectBuyerMenuById".equals(methodName))
                {
                    selectMenuByIdCallCount++;
                    return selectedMenus.get((Long) args[0]);
                }
                if ("updateBuyerRole".equals(methodName))
                {
                    updateRoleCallCount++;
                    return 1;
                }
                if ("insertBuyerRole".equals(methodName))
                {
                    insertRoleCallCount++;
                    return 1;
                }
                if ("updateBuyerRoleStatus".equals(methodName))
                {
                    updateRoleStatusCallCount++;
                    return 1;
                }
                if ("countBuyerAccountRoleByRoleId".equals(methodName))
                {
                    countAccountRoleCallCount++;
                    return 0;
                }
                if ("deleteBuyerRoleMenuByRoleId".equals(methodName))
                {
                    deleteRoleMenuCallCount++;
                    return 1;
                }
                if ("deleteBuyerRoleById".equals(methodName))
                {
                    deleteRoleCallCount++;
                    return 1;
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
                if ("batchBuyerRoleMenu".equals(methodName))
                {
                    batchRoleMenuCallCount++;
                    return ((Long[]) args[2]).length;
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
