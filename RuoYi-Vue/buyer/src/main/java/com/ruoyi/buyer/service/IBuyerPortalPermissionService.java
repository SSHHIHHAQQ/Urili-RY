package com.ruoyi.buyer.service;

import java.util.List;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalPermissionInfo;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalTreeSelect;

/**
 * Buyer terminal menu and role service.
 */
public interface IBuyerPortalPermissionService
{
    public List<PortalMenu> selectMenuList(PortalMenu menu);

    public PortalMenu selectMenuById(Long menuId);

    public List<PortalTreeSelect> buildMenuTreeSelect(PortalMenu menu);

    public List<PortalTreeSelect> buildSelfManagementMenuTreeSelect();

    public List<Long> selectMenuIdsByRoleId(Long buyerId, Long roleId);

    public List<Long> selectSelfManagementMenuIdsByRoleId(Long buyerId, Long roleId);

    public boolean checkMenuNameUnique(PortalMenu menu);

    public boolean hasChildByMenuId(Long menuId);

    public boolean checkMenuExistRole(Long menuId);

    public int insertMenu(PortalMenu menu);

    public int updateMenu(PortalMenu menu);

    public int deleteMenuById(Long menuId);

    public List<PortalRole> selectRoleList(Long buyerId, PortalRole role);

    public List<PortalRole> selectRoleAll(Long buyerId);

    public PortalRole selectRoleById(Long buyerId, Long roleId);

    public int insertRole(Long buyerId, PortalRole role);

    public int updateRole(Long buyerId, PortalRole role);

    public int updateRoleStatus(Long buyerId, PortalRole role);

    public int deleteRoleByIds(Long buyerId, Long[] roleIds);

    public List<Long> selectAccountRoleIds(Long buyerId, Long accountId);

    public int assignAccountRoles(Long buyerId, Long accountId, Long[] roleIds);

    public PortalPermissionInfo selectPortalPermissionInfo(PortalLoginSession session);

    public List<PortalMenu> selectPortalMenuTree(PortalLoginSession session);
}
