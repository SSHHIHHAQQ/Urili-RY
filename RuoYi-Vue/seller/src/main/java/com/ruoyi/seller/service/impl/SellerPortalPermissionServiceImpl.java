package com.ruoyi.seller.service.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.mapper.SellerPortalPermissionMapper;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalPermissionInfo;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalTreeSelect;
import com.ruoyi.system.service.IPortalPermissionCheckService;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalPermissionSupport;

/**
 * Seller terminal permission service.
 */
@Service
public class SellerPortalPermissionServiceImpl implements ISellerPortalPermissionService, IPortalPermissionCheckService
{
    private static final String OWNER_ROLE_KEY = "owner";

    @Autowired
    private SellerPortalPermissionMapper permissionMapper;

    @Autowired
    private ISellerService sellerService;

    @Autowired
    private SellerMapper sellerMapper;

    @Override
    public List<PortalMenu> selectMenuList(PortalMenu menu)
    {
        return permissionMapper.selectSellerMenuList(menu == null ? new PortalMenu() : menu);
    }

    @Override
    public PortalMenu selectMenuById(Long menuId)
    {
        PortalMenu menu = permissionMapper.selectSellerMenuById(menuId);
        if (menu == null)
        {
            throw new ServiceException("卖家端菜单不存在");
        }
        return menu;
    }

    @Override
    public List<PortalTreeSelect> buildMenuTreeSelect(PortalMenu menu)
    {
        return PortalPermissionSupport.buildMenuTreeSelect(selectMenuList(menu));
    }

    @Override
    public List<Long> selectMenuIdsByRoleId(Long sellerId, Long roleId)
    {
        selectRoleById(sellerId, roleId);
        return permissionMapper.selectSellerMenuIdsByRoleId(sellerId, roleId);
    }

    @Override
    public boolean checkMenuNameUnique(PortalMenu menu)
    {
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        PortalMenu info = permissionMapper.checkSellerMenuNameUnique(menu.getMenuName(), menu.getParentId());
        return info == null || info.getMenuId().longValue() == menuId.longValue();
    }

    @Override
    public boolean hasChildByMenuId(Long menuId)
    {
        return permissionMapper.hasChildByMenuId(menuId) > 0;
    }

    @Override
    public boolean checkMenuExistRole(Long menuId)
    {
        return permissionMapper.checkMenuExistRole(menuId) > 0;
    }

    @Override
    public int insertMenu(PortalMenu menu)
    {
        PortalPermissionSupport.normalizeTerminalMenu(menu, "seller");
        assertMenuParentSafe(menu);
        if (!checkMenuNameUnique(menu))
        {
            throw new ServiceException("新增卖家端菜单失败，菜单名称已存在");
        }
        menu.setCreateBy(SecurityUtils.getUsername());
        return permissionMapper.insertSellerMenu(menu);
    }

    @Override
    public int updateMenu(PortalMenu menu)
    {
        selectMenuById(menu.getMenuId());
        PortalPermissionSupport.normalizeTerminalMenu(menu, "seller");
        PortalPermissionSupport.assertMenuNotSelfParent(menu);
        assertMenuParentSafe(menu);
        if (!checkMenuNameUnique(menu))
        {
            throw new ServiceException("修改卖家端菜单失败，菜单名称已存在");
        }
        menu.setUpdateBy(SecurityUtils.getUsername());
        return permissionMapper.updateSellerMenu(menu);
    }

    @Override
    public int deleteMenuById(Long menuId)
    {
        selectMenuById(menuId);
        if (hasChildByMenuId(menuId))
        {
            throw new ServiceException("存在子菜单，不允许删除");
        }
        if (checkMenuExistRole(menuId))
        {
            throw new ServiceException("菜单已分配，不允许删除");
        }
        return permissionMapper.deleteSellerMenuById(menuId);
    }

    @Override
    public List<PortalRole> selectRoleList(Long sellerId, PortalRole role)
    {
        sellerService.selectSellerById(sellerId);
        PortalRole query = role == null ? new PortalRole() : role;
        query.setSubjectId(sellerId);
        return permissionMapper.selectSellerRoleList(query);
    }

    @Override
    public List<PortalRole> selectRoleAll(Long sellerId)
    {
        return selectRoleList(sellerId, new PortalRole());
    }

    @Override
    public PortalRole selectRoleById(Long sellerId, Long roleId)
    {
        sellerService.selectSellerById(sellerId);
        PortalRole role = permissionMapper.selectSellerRoleById(sellerId, roleId);
        if (role == null)
        {
            throw new ServiceException("卖家端角色不存在");
        }
        return role;
    }

    @Override
    @Transactional
    public int insertRole(Long sellerId, PortalRole role)
    {
        sellerService.selectSellerById(sellerId);
        PortalPermissionSupport.normalizeRole(role, sellerId);
        checkRoleUnique(role);
        assertRoleMenusExist(role);
        role.setCreateBy(SecurityUtils.getUsername());
        int rows = permissionMapper.insertSellerRole(role);
        insertRoleMenus(role);
        return rows;
    }

    @Override
    @Transactional
    public int updateRole(Long sellerId, PortalRole role)
    {
        assertRoleNotOwner(selectRoleById(sellerId, role.getRoleId()));
        PortalPermissionSupport.normalizeRole(role, sellerId);
        checkRoleUnique(role);
        assertRoleMenusExist(role);
        role.setUpdateBy(SecurityUtils.getUsername());
        int rows = permissionMapper.updateSellerRole(role);
        permissionMapper.deleteSellerRoleMenuByRoleId(sellerId, role.getRoleId());
        insertRoleMenus(role);
        return rows;
    }

    @Override
    public int updateRoleStatus(Long sellerId, PortalRole role)
    {
        assertRoleNotOwner(selectRoleById(sellerId, role.getRoleId()));
        String status = StringUtils.defaultIfBlank(role.getStatus(), UserConstants.NORMAL);
        PartnerSupport.assertStatus(status);
        return permissionMapper.updateSellerRoleStatus(sellerId, role.getRoleId(), status, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int deleteRoleByIds(Long sellerId, Long[] roleIds)
    {
        Long[] ids = PortalPermissionSupport.sanitizeIds(roleIds);
        if (ids.length == 0)
        {
            throw new ServiceException("请选择要删除的角色");
        }
        int rows = 0;
        for (Long roleId : ids)
        {
            assertRoleNotOwner(selectRoleById(sellerId, roleId));
            if (permissionMapper.countSellerAccountRoleByRoleId(sellerId, roleId) > 0)
            {
                throw new ServiceException("角色已分配账号，不允许删除");
            }
            permissionMapper.deleteSellerRoleMenuByRoleId(sellerId, roleId);
            rows += permissionMapper.deleteSellerRoleById(sellerId, roleId, SecurityUtils.getUsername());
        }
        return rows;
    }

    @Override
    public List<Long> selectAccountRoleIds(Long sellerId, Long accountId)
    {
        assertSellerAccount(sellerId, accountId);
        return permissionMapper.selectSellerAccountRoleIds(sellerId, accountId);
    }

    @Override
    @Transactional
    public int assignAccountRoles(Long sellerId, Long accountId, Long[] roleIds)
    {
        SellerAccount account = assertSellerAccount(sellerId, accountId);
        Long[] ids = PortalPermissionSupport.sanitizeIds(roleIds);
        if (ids.length > 0 && permissionMapper.countSellerRolesByIds(sellerId, ids) != ids.length)
        {
            throw new ServiceException("存在不属于该卖家的角色");
        }
        assertOwnerRoleBindingMatchesAccountRole(sellerId, account, ids);
        permissionMapper.deleteSellerAccountRoles(sellerId, accountId);
        if (ids.length == 0)
        {
            return 1;
        }
        return permissionMapper.batchSellerAccountRoles(sellerId, accountId, ids);
    }

    @Override
    public PortalPermissionInfo selectPortalPermissionInfo(PortalLoginSession session)
    {
        SellerAccount account = assertActiveSellerSession(session);
        PortalPermissionInfo info = new PortalPermissionInfo();
        info.setTerminal(session.getTerminal());
        info.setSubjectId(session.getSubjectId());
        info.setSubjectNo(session.getSubjectNo());
        info.setAccountId(account.getSellerAccountId());
        info.setUserName(account.getUserName());
        info.setNickName(account.getNickName());
        info.setRoles(new LinkedHashSet<>(permissionMapper.selectSellerAccountRoleKeys(session.getSubjectId(), session.getAccountId())));
        info.setPermissions(splitPermissions(permissionMapper.selectSellerAccountPermissions(session.getSubjectId(), session.getAccountId())));
        return info;
    }

    @Override
    public List<PortalMenu> selectPortalMenuTree(PortalLoginSession session)
    {
        assertActiveSellerSession(session);
        List<PortalMenu> menus = permissionMapper.selectSellerAccountMenuList(session.getSubjectId(), session.getAccountId());
        for (PortalMenu menu : menus)
        {
            PortalPermissionSupport.assertReadableTerminalMenu(menu, "seller");
        }
        return PortalPermissionSupport.buildMenuTree(menus);
    }

    @Override
    public String terminal()
    {
        return "seller";
    }

    @Override
    public Set<String> selectPermissions(PortalLoginSession session)
    {
        return selectPortalPermissionInfo(session).getPermissions();
    }

    private void insertRoleMenus(PortalRole role)
    {
        Long[] menuIds = PortalPermissionSupport.sanitizeIds(role.getMenuIds());
        if (menuIds.length > 0)
        {
            permissionMapper.batchSellerRoleMenu(role.getSubjectId(), role.getRoleId(), menuIds);
        }
    }

    private void assertRoleMenusExist(PortalRole role)
    {
        Long[] menuIds = PortalPermissionSupport.sanitizeIds(role.getMenuIds());
        if (menuIds.length > 0 && permissionMapper.countSellerMenusByIds(menuIds) != menuIds.length)
        {
            throw new ServiceException("存在不属于卖家端的菜单");
        }
        for (Long menuId : menuIds)
        {
            PortalMenu menu = permissionMapper.selectSellerMenuById(menuId);
            if (menu == null)
            {
                throw new ServiceException("存在不属于卖家端的菜单");
            }
            PortalPermissionSupport.assertTerminalMenuId(menu.getMenuId(), "seller");
            PortalPermissionSupport.assertTerminalMenuComponent(menu, "seller");
            PortalPermissionSupport.assertTerminalMenuPerms(menu, "seller");
        }
    }

    private void assertMenuParentSafe(PortalMenu menu)
    {
        Long parentId = menu.getParentId();
        if (PortalPermissionSupport.MENU_ROOT_ID.equals(parentId))
        {
            return;
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (!PortalPermissionSupport.MENU_ROOT_ID.equals(cursor))
        {
            if (menu.getMenuId() != null && menu.getMenuId().equals(cursor))
            {
                throw new ServiceException("上级菜单不能选择自己的子菜单");
            }
            if (!visited.add(cursor))
            {
                throw new ServiceException("卖家端菜单层级存在循环");
            }
            PortalMenu parent = selectMenuById(cursor);
            cursor = parent.getParentId();
        }
    }

    private void assertRoleNotOwner(PortalRole role)
    {
        if (isOwnerRole(role))
        {
            throw new ServiceException("卖家端 owner 角色不允许修改、停用或删除");
        }
    }

    private boolean isOwnerRole(PortalRole role)
    {
        return role != null && role.getRoleKey() != null && OWNER_ROLE_KEY.equalsIgnoreCase(role.getRoleKey().trim());
    }

    private void assertOwnerRoleBindingMatchesAccountRole(Long sellerId, SellerAccount account, Long[] roleIds)
    {
        PortalRole ownerRole = permissionMapper.checkSellerRoleKeyUnique(sellerId, OWNER_ROLE_KEY);
        boolean ownerAccount = PartnerSupport.ACCOUNT_ROLE_OWNER.equals(account.getAccountRole());
        if (ownerRole == null || !UserConstants.NORMAL.equals(ownerRole.getStatus()))
        {
            if (ownerAccount)
            {
                throw new ServiceException("卖家端 owner 角色不存在或已停用");
            }
            return;
        }
        boolean hasOwnerRole = Arrays.asList(roleIds).contains(ownerRole.getRoleId());
        if (ownerAccount && !hasOwnerRole)
        {
            throw new ServiceException("卖家主账号必须保留 owner 角色");
        }
        if (!ownerAccount && hasOwnerRole)
        {
            throw new ServiceException("非卖家主账号不能分配 owner 角色");
        }
    }

    private void checkRoleUnique(PortalRole role)
    {
        PortalRole roleName = permissionMapper.checkSellerRoleNameUnique(role.getSubjectId(), role.getRoleName());
        if (roleName != null && !roleName.getRoleId().equals(role.getRoleId()))
        {
            throw new ServiceException("角色名称已存在");
        }
        PortalRole roleKey = permissionMapper.checkSellerRoleKeyUnique(role.getSubjectId(), role.getRoleKey());
        if (roleKey != null && !roleKey.getRoleId().equals(role.getRoleId()))
        {
            throw new ServiceException("角色权限字符已存在");
        }
    }

    private SellerAccount assertSellerAccount(Long sellerId, Long accountId)
    {
        sellerService.selectSellerById(sellerId);
        SellerAccount account = sellerMapper.selectSellerAccountByIdAndSellerId(sellerId, accountId);
        if (account == null)
        {
            throw new ServiceException("卖家端账号不存在");
        }
        return account;
    }

    private SellerAccount assertActiveSellerSession(PortalLoginSession session)
    {
        assertSellerSessionShape(session);
        Seller seller = sellerService.selectSellerById(session.getSubjectId());
        SellerAccount account = assertSellerAccount(session.getSubjectId(), session.getAccountId());
        if (!PartnerSupport.STATUS_NORMAL.equals(seller.getStatus()))
        {
            throw new ServiceException("卖家已停用");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("卖家端账号已停用");
        }
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("卖家端账号已锁定");
        }
        if (sellerMapper.countOnlineSellerSession(session.getSubjectId(), session.getAccountId(), session.getTokenId()) <= 0)
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        return account;
    }

    private void assertSellerSessionShape(PortalLoginSession session)
    {
        if (session == null || !"seller".equals(session.getTerminal()) || session.getSubjectId() == null
                || session.getAccountId() == null || StringUtils.isBlank(session.getTokenId()))
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
    }

    private Set<String> splitPermissions(List<String> values)
    {
        Set<String> permissions = new LinkedHashSet<>();
        for (String value : values)
        {
            if (StringUtils.isNotEmpty(value))
            {
                for (String item : value.split(","))
                {
                    String permission = StringUtils.trimToEmpty(item);
                    if (StringUtils.isNotEmpty(permission))
                    {
                        assertSellerPortalPermission(permission);
                        permissions.add(permission);
                    }
                }
            }
        }
        return permissions;
    }

    private void assertSellerPortalPermission(String permission)
    {
        if (!permission.startsWith("seller:") || permission.startsWith("seller:admin:")
                || "*:*:*".equals(permission))
        {
            throw new ServiceException("卖家端权限配置异常");
        }
    }
}
