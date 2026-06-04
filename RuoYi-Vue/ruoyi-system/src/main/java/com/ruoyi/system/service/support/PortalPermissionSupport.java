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
