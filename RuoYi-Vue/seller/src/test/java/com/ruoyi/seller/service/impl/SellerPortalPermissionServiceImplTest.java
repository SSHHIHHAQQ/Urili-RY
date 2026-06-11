package com.ruoyi.seller.service.impl;

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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.mapper.SellerPortalPermissionMapper;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalRole;
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

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L });

        assertEquals(2, rows);
        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.countedSellerId);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.countedRoleIds);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.deletedSellerId);
        assertEquals(Long.valueOf(22L), permissionMapper.deletedAccountId);
        assertEquals(1, permissionMapper.batchAccountRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.batchedSellerId);
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

        int rows = service.assignAccountRoles(11L, 22L, new Long[0]);

        assertEquals(1, rows);
        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.deletedSellerId);
        assertEquals(Long.valueOf(22L), permissionMapper.deletedAccountId);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsInvalidRoleIdsBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(0);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, null, 0L }));

        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsDuplicateRoleIdsBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(0);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, 101L }));

        assertEquals(0, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesRejectsOwnerAccountWithoutOwnerRoleBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withOwnerRole(role(101L, 11L, "owner"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 102L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void assignAccountRolesAllowsOwnerAccountWhenOwnerRoleIsKept()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        account.setAccountRole(PartnerSupport.ACCOUNT_ROLE_OWNER);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(2)
                .withOwnerRole(role(101L, 11L, "owner"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        int rows = service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L });

        assertEquals(2, rows);
        assertEquals(1, permissionMapper.deleteAccountRolesCallCount);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.batchedRoleIds);
    }

    @Test
    public void assignAccountRolesRejectsNonOwnerAccountWithOwnerRoleBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withOwnerRole(role(101L, 11L, "owner"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertEquals(0, permissionMapper.deleteAccountRolesCallCount);
        assertEquals(0, permissionMapper.batchAccountRolesCallCount);
    }

    @Test
    public void updateRoleRejectsOwnerRoleBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "owner"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
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
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "owner"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(101L, 11L, "owner");
        payload.setStatus("1");

        assertServiceException(() -> service.updateRoleStatus(11L, payload));

        assertEquals(0, permissionMapper.updateRoleStatusCallCount);
    }

    @Test
    public void deleteRoleRejectsOwnerRoleBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "owner"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.deleteRoleByIds(11L, new Long[] { 101L }));

        assertEquals(0, permissionMapper.countAccountRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.deleteRoleCallCount);
    }

    @Test
    public void deleteRoleRejectsInvalidRoleIdsBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
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
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.deleteRoleByIds(11L, new Long[] { 101L, 101L }));

        assertEquals(0, permissionMapper.selectRoleByIdCallCount);
        assertEquals(0, permissionMapper.countAccountRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.deleteRoleCallCount);
    }

    @Test
    public void insertRoleRejectsMenuIdsOutsideSellerTerminalBeforeMutatingRole()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withValidMenuCount(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
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
    public void insertRoleRejectsInvalidSellerMenuIdsBeforeMutatingRole()
    {
        assertInvalidSellerRoleMenuIdsRejected(new Long[] { 301L, null });
        assertInvalidSellerRoleMenuIdsRejected(new Long[] { 301L, 0L });
        assertInvalidSellerRoleMenuIdsRejected(new Long[] { 301L, 301L });
    }

    @Test
    public void insertRoleRejectsDirtySellerMenusBeforeMutatingRole()
    {
        assertDirtySellerMenuRejected(menu(99999L, "F", "seller:account:list", ""));
        assertDirtySellerMenuRejected(menu(100001L, "F", "seller:admin:menu:list", ""));
        assertDirtySellerMenuRejected(menu(100002L, "C", "seller:product:list", "common/ProductList"));
    }

    @Test
    public void insertRoleRejectsNonSelfManagementSellerMenusBeforeMutatingRole()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        PortalMenu productMenu = menu(100003L, "F", "seller:product:list", "");
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withValidMenuCount(1)
                .withSelectedMenus(productMenu);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(null, 11L, "staff");
        payload.setMenuIds(new Long[] { 100003L });

        assertServiceException(() -> service.insertRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
        assertEquals(0, permissionMapper.insertRoleCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    @Test
    public void selectMenuByIdRejectsMenuOutsideSellerTerminalRange()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedMenus(menu(99999L, "M", "", ""));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.selectMenuById(99999L));

        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
    }

    @Test
    public void updateRoleRejectsMenuIdsOutsideSellerTerminalBeforeMutatingRoleOrBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"))
                .withValidMenuCount(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
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
    public void updateRoleRejectsInvalidSellerMenuIdsBeforeMutatingRoleOrBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"));
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
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
    public void updateRoleRejectsNonSelfManagementSellerMenusBeforeMutatingRoleOrBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        PortalMenu productMenu = menu(100003L, "F", "seller:product:list", "");
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"))
                .withValidMenuCount(1)
                .withSelectedMenus(productMenu);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(101L, 11L, "staff");
        payload.setMenuIds(new Long[] { 100003L });

        assertServiceException(() -> service.updateRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
        assertEquals(0, permissionMapper.updateRoleCallCount);
        assertEquals(0, permissionMapper.deleteRoleMenuCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    @Test
    public void selectMenuIdsByRoleIdKeepsSellerScopeForCheckedKeys()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withSelectedRole(role(101L, 11L, "staff"))
                .withSelectedMenuIds(301L, 302L);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        List<Long> checkedKeys = service.selectMenuIdsByRoleId(11L, 101L);

        assertEquals(Arrays.asList(301L, 302L), checkedKeys);
        assertEquals(1, permissionMapper.selectMenuIdsCallCount);
        assertEquals(Long.valueOf(11L), permissionMapper.selectedMenuSellerId);
        assertEquals(Long.valueOf(101L), permissionMapper.selectedMenuRoleId);
    }

    @Test
    public void selectMenuIdsByRoleIdRejectsRoleOutsideSellerBeforeCheckedKeysLookup()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.selectMenuIdsByRoleId(11L, 101L));

        assertEquals(0, permissionMapper.selectMenuIdsCallCount);
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

    @Test
    public void assignAccountRolesRejectsDisabledRolesBeforeMutatingBindings()
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());

        assertServiceException(() -> service.assignAccountRoles(11L, 22L, new Long[] { 101L, 102L }));

        assertEquals(1, permissionMapper.countRolesCallCount);
        assertArrayEquals(new Long[] { 101L, 102L }, permissionMapper.countedRoleIds);
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

    private PortalRole role(Long roleId, Long sellerId, String roleKey)
    {
        PortalRole role = new PortalRole();
        role.setRoleId(roleId);
        role.setSubjectId(sellerId);
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

    private void assertDirtySellerMenuRejected(PortalMenu menu)
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1)
                .withValidMenuCount(1)
                .withSelectedMenus(menu);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(null, 11L, "staff");
        payload.setMenuIds(new Long[] { menu.getMenuId() });

        assertServiceException(() -> service.insertRole(11L, payload));

        assertEquals(1, permissionMapper.countMenusCallCount);
        assertEquals(1, permissionMapper.selectMenuByIdCallCount);
        assertEquals(0, permissionMapper.insertRoleCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
    }

    private void assertInvalidSellerRoleMenuIdsRejected(Long[] menuIds)
    {
        Seller seller = seller(11L);
        SellerAccount account = account(22L, 11L);
        RecordingSellerPortalPermissionMapper permissionMapper = new RecordingSellerPortalPermissionMapper(1);
        SellerPortalPermissionServiceImpl service = service(sellerService(seller), sellerMapper(account),
                permissionMapper.proxy());
        PortalRole payload = role(null, 11L, "staff");
        payload.setMenuIds(menuIds);

        assertServiceException(() -> service.insertRole(11L, payload));

        assertEquals(0, permissionMapper.countMenusCallCount);
        assertEquals(0, permissionMapper.insertRoleCallCount);
        assertEquals(0, permissionMapper.batchRoleMenuCallCount);
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
            if ("selectSellerAccountByIdAndSellerId".equals(methodName))
            {
                Long sellerId = (Long) args[0];
                SellerAccount account = accountById.get((Long) args[1]);
                return account != null && sellerId.equals(account.getSellerId()) ? account : null;
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

        private int validMenuCount = -1;

        private int countRolesCallCount;

        private Long countedSellerId;

        private Long[] countedRoleIds;

        private int deleteAccountRolesCallCount;

        private Long deletedSellerId;

        private Long deletedAccountId;

        private int batchAccountRolesCallCount;

        private Long batchedSellerId;

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

        private Long selectedMenuSellerId;

        private Long selectedMenuRoleId;

        private int countMenusCallCount;

        private Long[] countedMenuIds;

        private int insertRoleCallCount;

        private int batchRoleMenuCallCount;

        private RecordingSellerPortalPermissionMapper(int validRoleCount)
        {
            this.validRoleCount = validRoleCount;
        }

        private RecordingSellerPortalPermissionMapper withOwnerRole(PortalRole ownerRole)
        {
            this.ownerRole = ownerRole;
            return this;
        }

        private RecordingSellerPortalPermissionMapper withSelectedRole(PortalRole selectedRole)
        {
            this.selectedRole = selectedRole;
            return this;
        }

        private RecordingSellerPortalPermissionMapper withSelectedMenuIds(Long... selectedMenuIds)
        {
            this.selectedMenuIds = Arrays.asList(selectedMenuIds);
            return this;
        }

        private RecordingSellerPortalPermissionMapper withSelectedMenus(PortalMenu... selectedMenus)
        {
            this.selectedMenus = new HashMap<>();
            for (PortalMenu menu : selectedMenus)
            {
                this.selectedMenus.put(menu.getMenuId(), menu);
            }
            return this;
        }

        private RecordingSellerPortalPermissionMapper withValidMenuCount(int validMenuCount)
        {
            this.validMenuCount = validMenuCount;
            return this;
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
                if ("countSellerMenusByIds".equals(methodName))
                {
                    countMenusCallCount++;
                    countedMenuIds = (Long[]) args[0];
                    return validMenuCount >= 0 ? validMenuCount : countedMenuIds.length;
                }
                if ("checkSellerRoleKeyUnique".equals(methodName))
                {
                    return ownerRole;
                }
                if ("selectSellerRoleById".equals(methodName))
                {
                    selectRoleByIdCallCount++;
                    return selectedRole;
                }
                if ("selectSellerMenuIdsByRoleId".equals(methodName))
                {
                    selectMenuIdsCallCount++;
                    selectedMenuSellerId = (Long) args[0];
                    selectedMenuRoleId = (Long) args[1];
                    return selectedMenuIds;
                }
                if ("selectSellerMenuById".equals(methodName))
                {
                    selectMenuByIdCallCount++;
                    return selectedMenus.get((Long) args[0]);
                }
                if ("updateSellerRole".equals(methodName))
                {
                    updateRoleCallCount++;
                    return 1;
                }
                if ("insertSellerRole".equals(methodName))
                {
                    insertRoleCallCount++;
                    return 1;
                }
                if ("updateSellerRoleStatus".equals(methodName))
                {
                    updateRoleStatusCallCount++;
                    return 1;
                }
                if ("countSellerAccountRoleByRoleId".equals(methodName))
                {
                    countAccountRoleCallCount++;
                    return 0;
                }
                if ("deleteSellerRoleMenuByRoleId".equals(methodName))
                {
                    deleteRoleMenuCallCount++;
                    return 1;
                }
                if ("deleteSellerRoleById".equals(methodName))
                {
                    deleteRoleCallCount++;
                    return 1;
                }
                if ("deleteSellerAccountRoles".equals(methodName))
                {
                    deleteAccountRolesCallCount++;
                    deletedSellerId = (Long) args[0];
                    deletedAccountId = (Long) args[1];
                    return 1;
                }
                if ("batchSellerAccountRoles".equals(methodName))
                {
                    batchAccountRolesCallCount++;
                    batchedSellerId = (Long) args[0];
                    batchedAccountId = (Long) args[1];
                    batchedRoleIds = (Long[]) args[2];
                    return batchedRoleIds.length;
                }
                if ("batchSellerRoleMenu".equals(methodName))
                {
                    batchRoleMenuCallCount++;
                    return ((Long[]) args[2]).length;
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
