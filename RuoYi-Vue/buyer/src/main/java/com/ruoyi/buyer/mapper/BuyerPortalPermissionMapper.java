package com.ruoyi.buyer.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalRole;

/**
 * Buyer terminal menu and role mapper.
 */
public interface BuyerPortalPermissionMapper
{
    public List<PortalMenu> selectBuyerMenuList(PortalMenu menu);

    public PortalMenu selectBuyerMenuById(Long menuId);

    public int hasChildByMenuId(Long menuId);

    public int checkMenuExistRole(Long menuId);

    public PortalMenu checkBuyerMenuNameUnique(@Param("menuName") String menuName, @Param("parentId") Long parentId);

    public int insertBuyerMenu(PortalMenu menu);

    public int updateBuyerMenu(PortalMenu menu);

    public int deleteBuyerMenuById(Long menuId);

    public List<Long> selectBuyerMenuIdsByRoleId(Long roleId);

    public List<PortalRole> selectBuyerRoleList(PortalRole role);

    public PortalRole selectBuyerRoleById(@Param("buyerId") Long buyerId, @Param("roleId") Long roleId);

    public PortalRole checkBuyerRoleNameUnique(@Param("buyerId") Long buyerId, @Param("roleName") String roleName);

    public PortalRole checkBuyerRoleKeyUnique(@Param("buyerId") Long buyerId, @Param("roleKey") String roleKey);

    public int insertBuyerRole(PortalRole role);

    public int updateBuyerRole(PortalRole role);

    public int updateBuyerRoleStatus(@Param("buyerId") Long buyerId, @Param("roleId") Long roleId,
            @Param("status") String status, @Param("updateBy") String updateBy);

    public int deleteBuyerRoleById(@Param("buyerId") Long buyerId, @Param("roleId") Long roleId,
            @Param("updateBy") String updateBy);

    public int countBuyerAccountRoleByRoleId(Long roleId);

    public List<Long> selectBuyerAccountRoleIds(@Param("buyerId") Long buyerId, @Param("accountId") Long accountId);

    public List<String> selectBuyerAccountRoleKeys(@Param("buyerId") Long buyerId, @Param("accountId") Long accountId);

    public List<String> selectBuyerAccountPermissions(@Param("buyerId") Long buyerId, @Param("accountId") Long accountId);

    public List<PortalMenu> selectBuyerAccountMenuList(@Param("buyerId") Long buyerId, @Param("accountId") Long accountId);

    public int countBuyerRolesByIds(@Param("buyerId") Long buyerId, @Param("roleIds") Long[] roleIds);

    public int deleteBuyerAccountRoles(@Param("accountId") Long accountId);

    public int batchBuyerAccountRoles(@Param("accountId") Long accountId, @Param("roleIds") Long[] roleIds);

    public int deleteBuyerRoleMenuByRoleId(Long roleId);

    public int batchBuyerRoleMenu(@Param("roleId") Long roleId, @Param("menuIds") Long[] menuIds);
}
