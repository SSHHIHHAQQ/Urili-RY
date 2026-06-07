package com.ruoyi.system.service.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import com.ruoyi.common.constant.Constants;

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
}
