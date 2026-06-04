package com.ruoyi.seller.service;

import java.util.List;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalPermissionInfo;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalTreeSelect;

/**
 * Seller terminal menu and role service.
 */
public interface ISellerPortalPermissionService
{
    public List<PortalMenu> selectMenuList(PortalMenu menu);

    public PortalMenu selectMenuById(Long menuId);

    public List<PortalTreeSelect> buildMenuTreeSelect(PortalMenu menu);

    public List<Long> selectMenuIdsByRoleId(Long sellerId, Long roleId);

    public boolean checkMenuNameUnique(PortalMenu menu);

    public boolean hasChildByMenuId(Long menuId);

    public boolean checkMenuExistRole(Long menuId);

    public int insertMenu(PortalMenu menu);

    public int updateMenu(PortalMenu menu);

    public int deleteMenuById(Long menuId);

    public List<PortalRole> selectRoleList(Long sellerId, PortalRole role);

    public List<PortalRole> selectRoleAll(Long sellerId);

    public PortalRole selectRoleById(Long sellerId, Long roleId);

    public int insertRole(Long sellerId, PortalRole role);

    public int updateRole(Long sellerId, PortalRole role);

    public int updateRoleStatus(Long sellerId, PortalRole role);

    public int deleteRoleByIds(Long sellerId, Long[] roleIds);

    public List<Long> selectAccountRoleIds(Long sellerId, Long accountId);

    public int assignAccountRoles(Long sellerId, Long accountId, Long[] roleIds);

    public PortalPermissionInfo selectPortalPermissionInfo(PortalLoginSession session);

    public List<PortalMenu> selectPortalMenuTree(PortalLoginSession session);
}
