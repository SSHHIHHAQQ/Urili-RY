package com.ruoyi.system.service.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalTreeSelect;
import com.ruoyi.system.domain.vo.MetaVo;
import com.ruoyi.system.domain.vo.RouterVo;

/**
 * Shared validation and tree helpers for seller/buyer terminal permissions.
 */
public class PortalPermissionSupport
{
    public static final Long MENU_ROOT_ID = 0L;

    private PortalPermissionSupport()
    {
    }

    public static void normalizeMenu(PortalMenu menu)
    {
        if (menu == null)
        {
            throw new ServiceException("菜单不能为空");
        }
        menu.setMenuName(PartnerSupport.trimRequired(menu.getMenuName(), "菜单名称不能为空"));
        menu.setParentId(menu.getParentId() == null ? MENU_ROOT_ID : menu.getParentId());
        menu.setOrderNum(menu.getOrderNum() == null ? 0 : menu.getOrderNum());
        menu.setPath(StringUtils.trimToEmpty(menu.getPath()));
        menu.setComponent(StringUtils.trimToEmpty(menu.getComponent()));
        menu.setQuery(StringUtils.trimToEmpty(menu.getQuery()));
        menu.setRouteName(StringUtils.trimToEmpty(menu.getRouteName()));
        menu.setIsFrame(StringUtils.defaultIfBlank(menu.getIsFrame(), UserConstants.NO_FRAME));
        menu.setIsCache(StringUtils.defaultIfBlank(menu.getIsCache(), "0"));
        menu.setMenuType(StringUtils.defaultIfBlank(menu.getMenuType(), UserConstants.TYPE_MENU));
        menu.setVisible(StringUtils.defaultIfBlank(menu.getVisible(), UserConstants.NORMAL));
        menu.setStatus(StringUtils.defaultIfBlank(menu.getStatus(), UserConstants.NORMAL));
        menu.setPerms(StringUtils.trimToEmpty(menu.getPerms()));
        menu.setIcon(StringUtils.defaultIfBlank(menu.getIcon(), "#"));
        menu.setRemark(StringUtils.trimToEmpty(menu.getRemark()));
        assertNormalOrException(menu.getVisible(), "显示状态参数不正确");
        assertNormalOrException(menu.getStatus(), "菜单状态参数不正确");
        if (!UserConstants.TYPE_DIR.equals(menu.getMenuType()) && !UserConstants.TYPE_MENU.equals(menu.getMenuType())
                && !UserConstants.TYPE_BUTTON.equals(menu.getMenuType()))
        {
            throw new ServiceException("菜单类型参数不正确");
        }
    }

    public static void normalizeTerminalMenu(PortalMenu menu, String terminal)
    {
        normalizeMenu(menu);
        assertTerminalMenuComponent(menu, terminal);
        assertTerminalMenuPerms(menu, terminal);
    }

    public static void assertReadableTerminalMenu(PortalMenu menu, String terminal)
    {
        if (menu == null)
        {
            throw new ServiceException("Terminal menu cannot be null");
        }
        assertTerminalMenuId(menu.getMenuId(), terminal);
        assertTerminalMenuType(menu.getMenuType());
        assertTerminalMenuComponent(menu, terminal);
        assertTerminalMenuPerms(menu, terminal);
    }

    public static void assertTerminalMenuId(Long menuId, String terminal)
    {
        if (menuId == null || menuId <= 0)
        {
            throw new ServiceException("端内菜单ID不能为空");
        }
        String value = PartnerSupport.trimRequired(terminal, "端类型不能为空").toLowerCase();
        if ("seller".equals(value) && (menuId < 100000L || menuId >= 200000L))
        {
            throw new ServiceException("卖家端菜单ID必须位于卖家端区间");
        }
        if ("buyer".equals(value) && (menuId < 200000L || menuId >= 300000L))
        {
            throw new ServiceException("买家端菜单ID必须位于买家端区间");
        }
    }

    private static void assertTerminalMenuType(String menuType)
    {
        if (!UserConstants.TYPE_DIR.equals(menuType) && !UserConstants.TYPE_MENU.equals(menuType)
                && !UserConstants.TYPE_BUTTON.equals(menuType))
        {
            throw new ServiceException("Terminal menu type is invalid");
        }
    }

    public static void assertTerminalMenuComponent(PortalMenu menu, String terminal)
    {
        if (!UserConstants.TYPE_MENU.equals(menu.getMenuType()))
        {
            return;
        }
        String expectedRoot = PartnerSupport.trimRequired(terminal, "端类型不能为空").toLowerCase();
        String component = StringUtils.trimToEmpty(menu.getComponent());
        if (StringUtils.isEmpty(component))
        {
            throw new ServiceException("端内页面菜单组件不能为空");
        }
        String normalized = normalizeMenuTarget(component).toLowerCase();
        String root = getFirstPathSegment(normalized);
        if (!root.equals(expectedRoot))
        {
            throw new ServiceException("端内页面菜单组件必须使用本端页面根路径");
        }
        if (isForbiddenComponentRoot(root))
        {
            throw new ServiceException("端内页面菜单组件不能使用后台或共享根路径");
        }
    }

    public static void assertTerminalMenuPerms(PortalMenu menu, String terminal)
    {
        String terminalPrefix = PartnerSupport.trimRequired(terminal, "端类型不能为空") + ":";
        String perms = StringUtils.trimToEmpty(menu.getPerms());
        boolean requiresPermission = UserConstants.TYPE_MENU.equals(menu.getMenuType())
                || UserConstants.TYPE_BUTTON.equals(menu.getMenuType());
        if (StringUtils.isEmpty(perms))
        {
            if (requiresPermission)
            {
                throw new ServiceException("端内页面或按钮菜单权限不能为空");
            }
            return;
        }
        boolean hasPermission = false;
        for (String permission : perms.split(","))
        {
            String value = StringUtils.trimToEmpty(permission);
            if (StringUtils.isEmpty(value))
            {
                continue;
            }
            hasPermission = true;
            if (value.contains("*"))
            {
                throw new ServiceException("端内菜单权限不能使用通配符");
            }
            if (!value.startsWith(terminalPrefix))
            {
                throw new ServiceException("端内菜单权限必须使用本端前缀");
            }
            if (value.startsWith(terminalPrefix + "admin:"))
            {
                throw new ServiceException("端内菜单权限不能使用管理端权限前缀");
            }
        }
        if (requiresPermission && !hasPermission)
        {
            throw new ServiceException("端内页面或按钮菜单权限不能为空");
        }
    }

    public static void assertMenuNotSelfParent(PortalMenu menu)
    {
        if (menu.getMenuId() != null && menu.getMenuId().equals(menu.getParentId()))
        {
            throw new ServiceException("上级菜单不能选择自己");
        }
    }

    public static void normalizeRole(PortalRole role, Long subjectId)
    {
        if (role == null)
        {
            throw new ServiceException("角色不能为空");
        }
        if (subjectId == null)
        {
            throw new ServiceException("主体ID不能为空");
        }
        role.setSubjectId(subjectId);
        role.setRoleName(PartnerSupport.trimRequired(role.getRoleName(), "角色名称不能为空"));
        role.setRoleKey(PartnerSupport.trimRequired(role.getRoleKey(), "角色权限字符不能为空"));
        role.setRoleSort(role.getRoleSort() == null ? 0 : role.getRoleSort());
        role.setStatus(StringUtils.defaultIfBlank(role.getStatus(), UserConstants.NORMAL));
        role.setRemark(StringUtils.trimToEmpty(role.getRemark()));
        assertNormalOrException(role.getStatus(), "角色状态参数不正确");
        role.setMenuIds(sanitizeIds(role.getMenuIds()));
    }

    public static Long[] sanitizeIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return new Long[0];
        }
        Set<Long> values = new LinkedHashSet<>();
        for (Long id : ids)
        {
            if (id != null && id > 0)
            {
                values.add(id);
            }
        }
        return values.toArray(new Long[0]);
    }

    public static List<PortalMenu> buildMenuTree(List<PortalMenu> menus)
    {
        List<PortalMenu> returnList = new ArrayList<>();
        List<Long> tempList = menus.stream().map(PortalMenu::getMenuId).collect(Collectors.toList());
        for (Iterator<PortalMenu> iterator = menus.iterator(); iterator.hasNext();)
        {
            PortalMenu menu = iterator.next();
            menu.setChildren(new ArrayList<>());
            if (!tempList.contains(menu.getParentId()))
            {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        return returnList.isEmpty() ? menus : returnList;
    }

    public static List<PortalTreeSelect> buildMenuTreeSelect(List<PortalMenu> menus)
    {
        return buildMenuTree(menus).stream().map(PortalTreeSelect::new).collect(Collectors.toList());
    }

    public static List<RouterVo> buildRouters(List<PortalMenu> menus)
    {
        List<RouterVo> routers = new ArrayList<>();
        if (menus == null)
        {
            return routers;
        }
        for (PortalMenu menu : menus)
        {
            if (UserConstants.TYPE_BUTTON.equals(menu.getMenuType()))
            {
                continue;
            }
            RouterVo router = new RouterVo();
            router.setHidden(UserConstants.EXCEPTION.equals(menu.getVisible()));
            router.setName(getRouterName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getRouterComponent(menu));
            router.setQuery(menu.getQuery());
            router.setPerms(menu.getPerms());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(),
                    StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
            List<PortalMenu> children = menu.getChildren();
            if (StringUtils.isNotEmpty(children))
            {
                if (UserConstants.TYPE_DIR.equals(menu.getMenuType()))
                {
                    router.setAlwaysShow(true);
                    router.setRedirect("noRedirect");
                }
                router.setChildren(buildRouters(children));
            }
            routers.add(router);
        }
        return routers;
    }

    public static boolean hasAllPermissions(Set<String> permissions, String[] requiredPermissions)
    {
        if (requiredPermissions == null || requiredPermissions.length == 0)
        {
            return true;
        }
        if (permissions == null || permissions.isEmpty())
        {
            return false;
        }
        for (String permission : requiredPermissions)
        {
            if (StringUtils.isNotEmpty(permission) && !hasPermission(permissions, permission))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean hasAnyPermission(Set<String> permissions, String[] anyPermissions)
    {
        if (anyPermissions == null || anyPermissions.length == 0)
        {
            return true;
        }
        if (permissions == null || permissions.isEmpty())
        {
            return false;
        }
        for (String permission : anyPermissions)
        {
            if (StringUtils.isNotEmpty(permission) && hasPermission(permissions, permission))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPermission(Set<String> permissions, String permission)
    {
        return permissions.contains(StringUtils.trim(permission));
    }

    private static String getRouterName(PortalMenu menu)
    {
        String routerName = StringUtils.isNotEmpty(menu.getRouteName()) ? menu.getRouteName() : menu.getPath();
        return StringUtils.capitalize(routerName);
    }

    private static String getRouterPath(PortalMenu menu)
    {
        String routerPath = StringUtils.trimToEmpty(menu.getPath());
        if (MENU_ROOT_ID.equals(menu.getParentId()) && UserConstants.TYPE_DIR.equals(menu.getMenuType())
                && StringUtils.isNotEmpty(routerPath) && !routerPath.startsWith("/"))
        {
            return "/" + routerPath;
        }
        return routerPath;
    }

    private static String getRouterComponent(PortalMenu menu)
    {
        if (StringUtils.isNotEmpty(menu.getComponent()))
        {
            return menu.getComponent();
        }
        if (UserConstants.TYPE_DIR.equals(menu.getMenuType()) && !MENU_ROOT_ID.equals(menu.getParentId()))
        {
            return UserConstants.PARENT_VIEW;
        }
        return UserConstants.LAYOUT;
    }

    private static String normalizeMenuTarget(String value)
    {
        return StringUtils.trimToEmpty(value).replace("\\", "/").replaceAll("^(?:\\./|/)+", "");
    }

    private static String getFirstPathSegment(String value)
    {
        String normalized = normalizeMenuTarget(value);
        int separatorIndex = normalized.indexOf('/');
        return separatorIndex < 0 ? normalized : normalized.substring(0, separatorIndex);
    }

    private static boolean isForbiddenComponentRoot(String root)
    {
        return "admin".equals(root) || "common".equals(root) || "shared".equals(root) || "system".equals(root)
                || "user".equals(root) || "monitor".equals(root) || "tool".equals(root);
    }

    private static void recursionFn(List<PortalMenu> list, PortalMenu t)
    {
        List<PortalMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (PortalMenu child : childList)
        {
            if (!getChildList(list, child).isEmpty())
            {
                recursionFn(list, child);
            }
        }
    }

    private static List<PortalMenu> getChildList(List<PortalMenu> list, PortalMenu t)
    {
        List<PortalMenu> tlist = new ArrayList<>();
        for (PortalMenu n : list)
        {
            if (n.getParentId() != null && n.getParentId().longValue() == t.getMenuId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    private static void assertNormalOrException(String value, String message)
    {
        if (!UserConstants.NORMAL.equals(value) && !UserConstants.EXCEPTION.equals(value))
        {
            throw new ServiceException(message);
        }
    }
}
