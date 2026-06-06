package com.ruoyi.seller.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalRole;

/**
 * Seller terminal menu and role mapper.
 */
public interface SellerPortalPermissionMapper
{
    public List<PortalMenu> selectSellerMenuList(PortalMenu menu);

    public PortalMenu selectSellerMenuById(Long menuId);

    public int hasChildByMenuId(Long menuId);

    public int checkMenuExistRole(Long menuId);

    public PortalMenu checkSellerMenuNameUnique(@Param("menuName") String menuName, @Param("parentId") Long parentId);

    public int insertSellerMenu(PortalMenu menu);

    public int updateSellerMenu(PortalMenu menu);

    public int deleteSellerMenuById(Long menuId);

    public List<Long> selectSellerMenuIdsByRoleId(@Param("sellerId") Long sellerId, @Param("roleId") Long roleId);

    public List<PortalRole> selectSellerRoleList(PortalRole role);

    public PortalRole selectSellerRoleById(@Param("sellerId") Long sellerId, @Param("roleId") Long roleId);

    public PortalRole checkSellerRoleNameUnique(@Param("sellerId") Long sellerId, @Param("roleName") String roleName);

    public PortalRole checkSellerRoleKeyUnique(@Param("sellerId") Long sellerId, @Param("roleKey") String roleKey);

    public int insertSellerRole(PortalRole role);

    public int updateSellerRole(PortalRole role);

    public int updateSellerRoleStatus(@Param("sellerId") Long sellerId, @Param("roleId") Long roleId,
            @Param("status") String status, @Param("updateBy") String updateBy);

    public int deleteSellerRoleById(@Param("sellerId") Long sellerId, @Param("roleId") Long roleId,
            @Param("updateBy") String updateBy);

    public int countSellerAccountRoleByRoleId(@Param("sellerId") Long sellerId, @Param("roleId") Long roleId);

    public List<Long> selectSellerAccountRoleIds(@Param("sellerId") Long sellerId, @Param("accountId") Long accountId);

    public List<String> selectSellerAccountRoleKeys(@Param("sellerId") Long sellerId, @Param("accountId") Long accountId);

    public List<String> selectSellerAccountPermissions(@Param("sellerId") Long sellerId, @Param("accountId") Long accountId);

    public List<PortalMenu> selectSellerAccountMenuList(@Param("sellerId") Long sellerId, @Param("accountId") Long accountId);

    public int countSellerRolesByIds(@Param("sellerId") Long sellerId, @Param("roleIds") Long[] roleIds);

    public int deleteSellerAccountRoles(@Param("sellerId") Long sellerId, @Param("accountId") Long accountId);

    public int batchSellerAccountRoles(@Param("sellerId") Long sellerId, @Param("accountId") Long accountId,
            @Param("roleIds") Long[] roleIds);

    public int deleteSellerRoleMenuByRoleId(@Param("sellerId") Long sellerId, @Param("roleId") Long roleId);

    public int batchSellerRoleMenu(@Param("sellerId") Long sellerId, @Param("roleId") Long roleId,
            @Param("menuIds") Long[] menuIds);
}
