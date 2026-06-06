package com.ruoyi.buyer.service.impl;

import java.util.Arrays;
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
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalPermissionInfo;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalTreeSelect;
import com.ruoyi.system.service.IPortalPermissionCheckService;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalPermissionSupport;

/**
 * Buyer terminal permission service.
 */
@Service
public class BuyerPortalPermissionServiceImpl implements IBuyerPortalPermissionService, IPortalPermissionCheckService
{
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
        return menu;
    }

    @Override
    public List<PortalTreeSelect> buildMenuTreeSelect(PortalMenu menu)
    {
        return PortalPermissionSupport.buildMenuTreeSelect(selectMenuList(menu));
    }

    @Override
    public List<Long> selectMenuIdsByRoleId(Long buyerId, Long roleId)
    {
        selectRoleById(buyerId, roleId);
        return permissionMapper.selectBuyerMenuIdsByRoleId(buyerId, roleId);
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
        PortalPermissionSupport.normalizeMenu(menu);
        if (!checkMenuNameUnique(menu))
        {
            throw new ServiceException("新增买家端菜单失败，菜单名称已存在");
        }
        menu.setCreateBy(SecurityUtils.getUsername());
        return permissionMapper.insertBuyerMenu(menu);
    }

    @Override
    public int updateMenu(PortalMenu menu)
    {
        selectMenuById(menu.getMenuId());
        PortalPermissionSupport.normalizeMenu(menu);
        PortalPermissionSupport.assertMenuNotSelfParent(menu);
        if (!checkMenuNameUnique(menu))
        {
            throw new ServiceException("修改买家端菜单失败，菜单名称已存在");
        }
        menu.setUpdateBy(SecurityUtils.getUsername());
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
        role.setCreateBy(SecurityUtils.getUsername());
        int rows = permissionMapper.insertBuyerRole(role);
        insertRoleMenus(role);
        return rows;
    }

    @Override
    @Transactional
    public int updateRole(Long buyerId, PortalRole role)
    {
        selectRoleById(buyerId, role.getRoleId());
        PortalPermissionSupport.normalizeRole(role, buyerId);
        checkRoleUnique(role);
        role.setUpdateBy(SecurityUtils.getUsername());
        int rows = permissionMapper.updateBuyerRole(role);
        permissionMapper.deleteBuyerRoleMenuByRoleId(buyerId, role.getRoleId());
        insertRoleMenus(role);
        return rows;
    }

    @Override
    public int updateRoleStatus(Long buyerId, PortalRole role)
    {
        selectRoleById(buyerId, role.getRoleId());
        String status = StringUtils.defaultIfBlank(role.getStatus(), UserConstants.NORMAL);
        PartnerSupport.assertStatus(status);
        return permissionMapper.updateBuyerRoleStatus(buyerId, role.getRoleId(), status, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int deleteRoleByIds(Long buyerId, Long[] roleIds)
    {
        Long[] ids = PortalPermissionSupport.sanitizeIds(roleIds);
        if (ids.length == 0)
        {
            throw new ServiceException("请选择要删除的角色");
        }
        int rows = 0;
        for (Long roleId : ids)
        {
            selectRoleById(buyerId, roleId);
            if (permissionMapper.countBuyerAccountRoleByRoleId(buyerId, roleId) > 0)
            {
                throw new ServiceException("角色已分配账号，不允许删除");
            }
            permissionMapper.deleteBuyerRoleMenuByRoleId(buyerId, roleId);
            rows += permissionMapper.deleteBuyerRoleById(buyerId, roleId, SecurityUtils.getUsername());
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
        assertBuyerAccount(buyerId, accountId);
        Long[] ids = PortalPermissionSupport.sanitizeIds(roleIds);
        if (ids.length > 0 && permissionMapper.countBuyerRolesByIds(buyerId, ids) != ids.length)
        {
            throw new ServiceException("存在不属于该买家的角色");
        }
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
        info.setPermissions(splitPermissions(permissionMapper.selectBuyerAccountPermissions(session.getSubjectId(), session.getAccountId())));
        return info;
    }

    @Override
    public List<PortalMenu> selectPortalMenuTree(PortalLoginSession session)
    {
        assertActiveBuyerSession(session);
        return PortalPermissionSupport.buildMenuTree(permissionMapper.selectBuyerAccountMenuList(session.getSubjectId(), session.getAccountId()));
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
        BuyerAccount account = buyerMapper.selectBuyerAccountById(accountId);
        if (account == null || !buyerId.equals(account.getBuyerId()))
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

    private Set<String> splitPermissions(List<String> values)
    {
        Set<String> permissions = new LinkedHashSet<>();
        for (String value : values)
        {
            if (StringUtils.isNotEmpty(value))
            {
                permissions.addAll(Arrays.asList(value.trim().split(",")));
            }
        }
        permissions.removeIf(StringUtils::isEmpty);
        return permissions;
    }
}
