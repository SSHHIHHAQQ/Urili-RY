package com.ruoyi.system.service.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.vo.RouterVo;

public class PortalPermissionSupportTest
{
    @Test
    public void hasAllPermissionsAllowsEmptyRequirement()
    {
        assertTrue(PortalPermissionSupport.hasAllPermissions(null, new String[0]));
    }

    @Test
    public void hasAllPermissionsRequiresEveryPermission()
    {
        Set<String> permissions = new LinkedHashSet<>();
        permissions.add("seller:order:list");
        permissions.add("seller:order:edit");

        assertTrue(PortalPermissionSupport.hasAllPermissions(permissions,
                new String[] { "seller:order:list", "seller:order:edit" }));
        assertFalse(PortalPermissionSupport.hasAllPermissions(permissions,
                new String[] { "seller:order:list", "seller:order:delete" }));
    }

    @Test
    public void hasAnyPermissionRequiresOneMatch()
    {
        Set<String> permissions = new LinkedHashSet<>();
        permissions.add("buyer:order:list");

        assertTrue(PortalPermissionSupport.hasAnyPermission(permissions,
                new String[] { "buyer:order:edit", "buyer:order:list" }));
        assertFalse(PortalPermissionSupport.hasAnyPermission(permissions,
                new String[] { "buyer:order:edit", "buyer:order:delete" }));
    }

    @Test
    public void allPermissionWildcardDoesNotPassPortalChecks()
    {
        Set<String> permissions = new LinkedHashSet<>();
        permissions.add(Constants.ALL_PERMISSION);

        assertFalse(PortalPermissionSupport.hasAllPermissions(permissions,
                new String[] { "seller:anything:view", "buyer:anything:view" }));
        assertFalse(PortalPermissionSupport.hasAnyPermission(permissions,
                new String[] { "seller:anything:view" }));
    }

    @Test
    public void buildRoutersConvertsRootPortalPageMenuToFrontendRouterPayload()
    {
        PortalMenu page = menu(100001L, 0L, "卖家首页", "/seller/portal", UserConstants.TYPE_MENU,
                "Seller/Portal/index", "SellerPortalHome", "seller:portal:home");

        List<RouterVo> routers = PortalPermissionSupport.buildRouters(Collections.singletonList(page));

        assertEquals(1, routers.size());
        RouterVo router = routers.get(0);
        assertEquals("/seller/portal", router.getPath());
        assertEquals("Seller/Portal/index", router.getComponent());
        assertEquals("SellerPortalHome", router.getName());
        assertEquals("seller:portal:home", router.getPerms());
        assertEquals("卖家首页", router.getMeta().getTitle());
        assertFalse(router.getHidden());
    }

    @Test
    public void buildRoutersConvertsPortalMenuTreeAndKeepsChildPerms()
    {
        PortalMenu dir = menu(100000L, 0L, "卖家端", "seller", UserConstants.TYPE_DIR, "",
                "SellerRoot", "");
        PortalMenu page = menu(100001L, 100000L, "卖家首页", "portal", UserConstants.TYPE_MENU,
                "Seller/Portal/index", "SellerPortalHome", "seller:portal:home");
        dir.setChildren(Collections.singletonList(page));

        List<RouterVo> routers = PortalPermissionSupport.buildRouters(Collections.singletonList(dir));

        RouterVo root = routers.get(0);
        assertEquals("/seller", root.getPath());
        assertEquals(UserConstants.LAYOUT, root.getComponent());
        assertEquals("noRedirect", root.getRedirect());
        assertTrue(root.getAlwaysShow());
        assertEquals(1, root.getChildren().size());

        RouterVo child = root.getChildren().get(0);
        assertEquals("portal", child.getPath());
        assertEquals("Seller/Portal/index", child.getComponent());
        assertEquals("seller:portal:home", child.getPerms());
        assertEquals("卖家首页", child.getMeta().getTitle());
    }

    private PortalMenu menu(Long menuId, Long parentId, String menuName, String path, String menuType, String component,
            String routeName, String perms)
    {
        PortalMenu menu = new PortalMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        menu.setPath(path);
        menu.setMenuType(menuType);
        menu.setComponent(component);
        menu.setRouteName(routeName);
        menu.setPerms(perms);
        menu.setVisible(UserConstants.NORMAL);
        menu.setStatus(UserConstants.NORMAL);
        menu.setIsCache("0");
        menu.setIsFrame(UserConstants.NO_FRAME);
        menu.setIcon("#");
        return menu;
    }
}
