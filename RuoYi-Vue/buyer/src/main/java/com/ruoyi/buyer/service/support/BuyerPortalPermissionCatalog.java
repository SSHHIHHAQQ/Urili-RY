package com.ruoyi.buyer.service.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Central permission catalog for buyer portal role assignment and navigation.
 */
public final class BuyerPortalPermissionCatalog
{
    public static final Set<String> SELF_MANAGEMENT_PERMS = permissions(
            "buyer:portal:home",
            "buyer:account:list",
            "buyer:account:add",
            "buyer:account:edit",
            "buyer:account:role:query",
            "buyer:account:role:edit",
            "buyer:account:loginLog:list",
            "buyer:account:operLog:list",
            "buyer:account:session:list",
            "buyer:dept:list",
            "buyer:dept:query",
            "buyer:dept:add",
            "buyer:dept:edit",
            "buyer:dept:remove",
            "buyer:role:list",
            "buyer:role:query",
            "buyer:role:add",
            "buyer:role:edit",
            "buyer:role:remove");

    public static final Set<String> BUSINESS_PERMS = permissions(
            "buyer:product:center:list",
            "buyer:product:center:query");

    public static final Set<String> ROLE_ASSIGNABLE_PERMS = combine(SELF_MANAGEMENT_PERMS, BUSINESS_PERMS);

    public static final Set<String> NAVIGATION_PERMS = ROLE_ASSIGNABLE_PERMS;

    private BuyerPortalPermissionCatalog()
    {
    }

    public static boolean isRoleAssignable(String permission)
    {
        return ROLE_ASSIGNABLE_PERMS.contains(permission);
    }

    public static boolean isNavigationPermission(String permission)
    {
        return NAVIGATION_PERMS.contains(permission);
    }

    public static String[] ownerDefaultPerms()
    {
        return ROLE_ASSIGNABLE_PERMS.toArray(new String[0]);
    }

    private static Set<String> permissions(String... permissions)
    {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(permissions)));
    }

    private static Set<String> combine(Set<String> first, Set<String> second)
    {
        LinkedHashSet<String> combined = new LinkedHashSet<>();
        combined.addAll(first);
        combined.addAll(second);
        return Collections.unmodifiableSet(combined);
    }
}
