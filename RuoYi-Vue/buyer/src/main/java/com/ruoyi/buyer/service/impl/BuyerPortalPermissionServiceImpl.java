package com.ruoyi.buyer.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.mapper.BuyerPortalPermissionMapper;
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.buyer.service.support.BuyerPortalPermissionCatalog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalPermissionInfo;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalTreeSelect;
import com.ruoyi.system.service.IPortalPermissionCheckService;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalActorSupport;
import com.ruoyi.system.service.support.PortalPermissionSupport;

/**
 * Buyer terminal permission service.
 */
@Service
public class BuyerPortalPermissionServiceImpl implements IBuyerPortalPermissionService, IPortalPermissionCheckService
{
    private static final String OWNER_ROLE_KEY = "owner";

    @Autowired
    private BuyerPortalPermissionMapper permissionMapper;

    @Autowired
    private IBuyerService buyerService;

    @Autowired
    private BuyerMapper buyerMapper;

    @Override
    public List<PortalMenu> selectMenuList(PortalMenu menu)
    {
        return permissionMapper.selectBuyerMenuList(menu == null ? new PortalMenu() : menu);
    }

    @Override
    public PortalMenu selectMenuById(Long menuId)
    {
        PortalMenu menu = permissionMapper.selectBuyerMenuById(menuId);
        if (menu == null)
        {
            throw new ServiceException("买家端菜单不存在");
        }
        PortalPermissionSupport.assertTerminalMenuId(menu.getMenuId(), "buyer");
        return menu;
    }

    @Override
    public List<PortalTreeSelect> buildMenuTreeSelect(PortalMenu menu)
    {
        return PortalPermissionSupport.buildMenuTreeSelect(selectMenuList(menu));
    }

    @Override
    public List<PortalTreeSelect> buildSelfManagementMenuTreeSelect()
    {
        return PortalPermissionSupport.buildMenuTreeSelect(selectAssignableMenus());
    }

    @Override
    public List<Long> selectMenuIdsByRoleId(Long buyerId, Long roleId)
    {
        selectRoleById(buyerId, roleId);
        return permissionMapper.selectBuyerMenuIdsByRoleId(buyerId, roleId);
    }

    @Override
    public List<Long> selectSelfManagementMenuIdsByRoleId(Long buyerId, Long roleId)
    {
        List<Long> checkedKeys = selectMenuIdsByRoleId(buyerId, roleId);
        Set<Long> allowedMenuIds = new LinkedHashSet<>();
        for (PortalMenu menu : selectAssignableMenus())
        {
            allowedMenuIds.add(menu.getMenuId());
        }
        List<Long> result = new ArrayList<>();
        for (Long checkedKey : checkedKeys)
        {
            if (allowedMenuIds.contains(checkedKey))
            {
                result.add(checkedKey);
            }
        }
        return result;
    }

    @Override
    public boolean checkMenuNameUnique(PortalMenu menu)
    {
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        PortalMenu info = permissionMapper.checkBuyerMenuNameUnique(menu.getMenuName(), menu.getParentId());
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
        PortalPermissionSupport.normalizeTerminalMenu(menu, "buyer");
        assertMenuParentSafe(menu);
        if (!checkMenuNameUnique(menu))
        {
            throw new ServiceException("新增买家端菜单失败，菜单名称已存在");
        }
        menu.setCreateBy(PortalActorSupport.currentActorName());
        return permissionMapper.insertBuyerMenu(menu);
    }

    @Override
    public int updateMenu(PortalMenu menu)
    {
        selectMenuById(menu.getMenuId());
        PortalPermissionSupport.normalizeTerminalMenu(menu, "buyer");
        PortalPermissionSupport.assertMenuNotSelfParent(menu);
        assertMenuParentSafe(menu);
        if (!checkMenuNameUnique(menu))
        {
            throw new ServiceException("修改买家端菜单失败，菜单名称已存在");
        }
        menu.setUpdateBy(PortalActorSupport.currentActorName());
        return permissionMapper.updateBuyerMenu(menu);
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
        return permissionMapper.deleteBuyerMenuById(menuId);
    }

    @Override
    public List<PortalRole> selectRoleList(Long buyerId, PortalRole role)
    {
        buyerService.selectBuyerById(buyerId);
        PortalRole query = role == null ? new PortalRole() : role;
        query.setSubjectId(buyerId);
        return permissionMapper.selectBuyerRoleList(query);
    }

    @Override
    public List<PortalRole> selectRoleAll(Long buyerId)
    {
        return selectRoleList(buyerId, new PortalRole());
    }

    @Override
    public PortalRole selectRoleById(Long buyerId, Long roleId)
    {
        buyerService.selectBuyerById(buyerId);
        PortalRole role = permissionMapper.selectBuyerRoleById(buyerId, roleId);
        if (role == null)
        {
            throw new ServiceException("买家端角色不存在");
        }
        return role;
    }

    @Override
    @Transactional
    public int insertRole(Long buyerId, PortalRole role)
    {
        buyerService.selectBuyerById(buyerId);
        PortalPermissionSupport.normalizeRole(role, buyerId);
        checkRoleUnique(role);
        assertRoleMenusExist(role);
        role.setCreateBy(PortalActorSupport.currentActorName());
        int rows = permissionMapper.insertBuyerRole(role);
        insertRoleMenus(role);
        return rows;
    }

    @Override
    @Transactional
    public int updateRole(Long buyerId, PortalRole role)
    {
        assertRoleNotOwner(selectRoleById(buyerId, role.getRoleId()));
        PortalPermissionSupport.normalizeRole(role, buyerId);
        checkRoleUnique(role);
        assertRoleMenusExist(role);
        role.setUpdateBy(PortalActorSupport.currentActorName());
        int rows = permissionMapper.updateBuyerRole(role);
        permissionMapper.deleteBuyerRoleMenuByRoleId(buyerId, role.getRoleId());
        insertRoleMenus(role);
        return rows;
    }

    @Override
    public int updateRoleStatus(Long buyerId, PortalRole role)
    {
        assertRoleNotOwner(selectRoleById(buyerId, role.getRoleId()));
        String status = StringUtils.defaultIfBlank(role.getStatus(), UserConstants.NORMAL);
        PartnerSupport.assertStatus(status);
        return permissionMapper.updateBuyerRoleStatus(buyerId, role.getRoleId(), status, PortalActorSupport.currentActorName());
    }

    @Override
    @Transactional
    public int deleteRoleByIds(Long buyerId, Long[] roleIds)
    {
        Long[] ids = normalizeRoleIds(roleIds);
        if (ids.length == 0)
        {
            throw new ServiceException("请选择要删除的角色");
        }
        int rows = 0;
        for (Long roleId : ids)
        {
            assertRoleNotOwner(selectRoleById(buyerId, roleId));
            if (permissionMapper.countBuyerAccountRoleByRoleId(buyerId, roleId) > 0)
            {
                throw new ServiceException("角色已分配账号，不允许删除");
            }
            permissionMapper.deleteBuyerRoleMenuByRoleId(buyerId, roleId);
            rows += permissionMapper.deleteBuyerRoleById(buyerId, roleId, PortalActorSupport.currentActorName());
        }
        return rows;
    }

    @Override
    public List<Long> selectAccountRoleIds(Long buyerId, Long accountId)
    {
        assertBuyerAccount(buyerId, accountId);
        return permissionMapper.selectBuyerAccountRoleIds(buyerId, accountId);
    }

    @Override
    @Transactional
    public int assignAccountRoles(Long buyerId, Long accountId, Long[] roleIds)
    {
        BuyerAccount account = assertBuyerAccount(buyerId, accountId);
        Long[] ids = normalizeRoleIds(roleIds);
        if (ids.length > 0 && permissionMapper.countBuyerRolesByIds(buyerId, ids) != ids.length)
        {
            throw new ServiceException("存在不属于该买家的角色");
        }
        assertOwnerRoleBindingMatchesAccountRole(buyerId, account, ids);
        permissionMapper.deleteBuyerAccountRoles(buyerId, accountId);
        if (ids.length == 0)
        {
            return 1;
        }
        return permissionMapper.batchBuyerAccountRoles(buyerId, accountId, ids);
    }

    @Override
    public PortalPermissionInfo selectPortalPermissionInfo(PortalLoginSession session)
    {
        BuyerAccount account = assertActiveBuyerSession(session);
        PortalPermissionInfo info = new PortalPermissionInfo();
        info.setTerminal(session.getTerminal());
        info.setSubjectId(session.getSubjectId());
        info.setSubjectNo(session.getSubjectNo());
        info.setAccountId(account.getBuyerAccountId());
        info.setUserName(account.getUserName());
        info.setNickName(account.getNickName());
        info.setRoles(new LinkedHashSet<>(permissionMapper.selectBuyerAccountRoleKeys(session.getSubjectId(), session.getAccountId())));
        info.setPermissions(splitAssignablePermissions(permissionMapper.selectBuyerAccountPermissions(
                session.getSubjectId(), session.getAccountId())));
        return info;
    }

    @Override
    public List<PortalMenu> selectPortalMenuTree(PortalLoginSession session)
    {
        assertActiveBuyerSession(session);
        List<PortalMenu> menus = permissionMapper.selectBuyerAccountMenuList(session.getSubjectId(), session.getAccountId());
        List<PortalMenu> navigationMenus = new ArrayList<>();
        for (PortalMenu menu : menus)
        {
            PortalPermissionSupport.assertReadableTerminalMenu(menu, "buyer");
            if (BuyerPortalPermissionCatalog.isNavigationPermission(StringUtils.trimToEmpty(menu.getPerms())))
            {
                navigationMenus.add(menu);
            }
        }
        return PortalPermissionSupport.buildMenuTree(navigationMenus);
    }

    private List<PortalMenu> selectAssignableMenus()
    {
        List<PortalMenu> menus = permissionMapper.selectBuyerMenuList(new PortalMenu());
        List<PortalMenu> assignableMenus = new ArrayList<>();
        for (PortalMenu menu : menus)
        {
            PortalPermissionSupport.assertReadableTerminalMenu(menu, "buyer");
            if (BuyerPortalPermissionCatalog.isRoleAssignable(StringUtils.trimToEmpty(menu.getPerms())))
            {
                assignableMenus.add(menu);
            }
        }
        return assignableMenus;
    }

    @Override
    public String terminal()
    {
        return "buyer";
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
            permissionMapper.batchBuyerRoleMenu(role.getSubjectId(), role.getRoleId(), menuIds);
        }
    }

    private Long[] normalizeRoleIds(Long[] roleIds)
    {
        if (roleIds == null || roleIds.length == 0)
        {
            return new Long[0];
        }
        Set<Long> values = new LinkedHashSet<>();
        for (Long roleId : roleIds)
        {
            if (roleId == null || roleId <= 0)
            {
                throw new ServiceException("买家端账号角色必须使用有效角色ID");
            }
            if (!values.add(roleId))
            {
                throw new ServiceException("买家端账号角色不能重复");
            }
        }
        return values.toArray(new Long[0]);
    }

    private void assertRoleMenusExist(PortalRole role)
    {
        Long[] menuIds = normalizeRoleMenuIds(role.getMenuIds());
        role.setMenuIds(menuIds);
        if (menuIds.length > 0 && permissionMapper.countBuyerMenusByIds(menuIds) != menuIds.length)
        {
            throw new ServiceException("存在不属于买家端的菜单");
        }
        for (Long menuId : menuIds)
        {
            PortalMenu menu = permissionMapper.selectBuyerMenuById(menuId);
            if (menu == null)
            {
                throw new ServiceException("存在不属于买家端的菜单");
            }
            PortalPermissionSupport.assertTerminalMenuId(menu.getMenuId(), "buyer");
            PortalPermissionSupport.assertTerminalMenuComponent(menu, "buyer");
            PortalPermissionSupport.assertTerminalMenuPerms(menu, "buyer");
            assertRoleMenuAssignable(menu);
        }
    }

    private void assertRoleMenuAssignable(PortalMenu menu)
    {
        if (!BuyerPortalPermissionCatalog.isRoleAssignable(StringUtils.trimToEmpty(menu.getPerms())))
        {
            throw new ServiceException("买家端角色只能分配已开放的端内权限模板");
        }
    }

    private Long[] normalizeRoleMenuIds(Long[] menuIds)
    {
        if (menuIds == null || menuIds.length == 0)
        {
            return new Long[0];
        }
        Set<Long> values = new LinkedHashSet<>();
        for (Long menuId : menuIds)
        {
            if (menuId == null || menuId <= 0)
            {
                throw new ServiceException("买家端角色菜单必须使用有效菜单ID");
            }
            if (!values.add(menuId))
            {
                throw new ServiceException("买家端角色菜单不能重复");
            }
        }
        return values.toArray(new Long[0]);
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
                throw new ServiceException("买家端菜单层级存在循环");
            }
            PortalMenu parent = selectMenuById(cursor);
            cursor = parent.getParentId();
        }
    }

    private void assertRoleNotOwner(PortalRole role)
    {
        if (isOwnerRole(role))
        {
            throw new ServiceException("买家端 owner 角色不允许修改、停用或删除");
        }
    }

    private boolean isOwnerRole(PortalRole role)
    {
        return role != null && role.getRoleKey() != null && OWNER_ROLE_KEY.equalsIgnoreCase(role.getRoleKey().trim());
    }

    private void assertOwnerRoleBindingMatchesAccountRole(Long buyerId, BuyerAccount account, Long[] roleIds)
    {
        PortalRole ownerRole = permissionMapper.checkBuyerRoleKeyUnique(buyerId, OWNER_ROLE_KEY);
        boolean ownerAccount = PartnerSupport.ACCOUNT_ROLE_OWNER.equals(account.getAccountRole());
        if (ownerRole == null || !UserConstants.NORMAL.equals(ownerRole.getStatus()))
        {
            if (ownerAccount)
            {
                throw new ServiceException("买家端 owner 角色不存在或已停用");
            }
            return;
        }
        boolean hasOwnerRole = Arrays.asList(roleIds).contains(ownerRole.getRoleId());
        if (ownerAccount && !hasOwnerRole)
        {
            throw new ServiceException("买家主账号必须保留 owner 角色");
        }
        if (!ownerAccount && hasOwnerRole)
        {
            throw new ServiceException("非买家主账号不能分配 owner 角色");
        }
    }

    private void checkRoleUnique(PortalRole role)
    {
        PortalRole roleName = permissionMapper.checkBuyerRoleNameUnique(role.getSubjectId(), role.getRoleName());
        if (roleName != null && !roleName.getRoleId().equals(role.getRoleId()))
        {
            throw new ServiceException("角色名称已存在");
        }
        PortalRole roleKey = permissionMapper.checkBuyerRoleKeyUnique(role.getSubjectId(), role.getRoleKey());
        if (roleKey != null && !roleKey.getRoleId().equals(role.getRoleId()))
        {
            throw new ServiceException("角色权限字符已存在");
        }
    }

    private BuyerAccount assertBuyerAccount(Long buyerId, Long accountId)
    {
        buyerService.selectBuyerById(buyerId);
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(buyerId, accountId);
        if (account == null)
        {
            throw new ServiceException("买家端账号不存在");
        }
        return account;
    }

    private BuyerAccount assertActiveBuyerSession(PortalLoginSession session)
    {
        assertBuyerSessionShape(session);
        Buyer buyer = buyerService.selectBuyerById(session.getSubjectId());
        BuyerAccount account = assertBuyerAccount(session.getSubjectId(), session.getAccountId());
        if (!PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            throw new ServiceException("买家已停用");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("买家端账号已停用");
        }
        if (PartnerSupport.isAccountLocked(account.getLockStatus()))
        {
            throw new ServiceException("买家端账号已锁定");
        }
        if (buyerMapper.countOnlineBuyerSession(session.getSubjectId(), session.getAccountId(), session.getTokenId()) <= 0)
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        return account;
    }

    private void assertBuyerSessionShape(PortalLoginSession session)
    {
        if (session == null || !"buyer".equals(session.getTerminal()) || session.getSubjectId() == null
                || session.getAccountId() == null || StringUtils.isBlank(session.getTokenId()))
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
    }

    private Set<String> splitAssignablePermissions(List<String> values)
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
                        assertBuyerPortalPermission(permission);
                        if (BuyerPortalPermissionCatalog.isRoleAssignable(permission))
                        {
                            permissions.add(permission);
                        }
                    }
                }
            }
        }
        return permissions;
    }

    private void assertBuyerPortalPermission(String permission)
    {
        if (!permission.startsWith("buyer:") || permission.startsWith("buyer:admin:")
                || permission.contains("*"))
        {
            throw new ServiceException("买家端权限配置异常");
        }
    }
}
