package com.ruoyi.seller.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
import com.ruoyi.system.domain.PortalMenu;

/**
 * Admin control for seller terminal menus.
 */
@RestController
@RequestMapping("/seller/admin/menus")
public class AdminSellerMenuController extends BaseController
{
    @Autowired
    private ISellerPortalPermissionService permissionService;

    @PreAuthorize("@ss.hasPermi('seller:admin:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalMenu menu)
    {
        return success(permissionService.selectMenuList(menu));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:menu:query')")
    @GetMapping("/{menuId}")
    public AjaxResult getInfo(@PathVariable Long menuId)
    {
        return success(permissionService.selectMenuById(menuId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:menu:query')")
    @GetMapping("/treeselect")
    public AjaxResult treeselect(PortalMenu menu)
    {
        return success(permissionService.buildMenuTreeSelect(menu));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:query') and @ss.hasPermi('seller:admin:menu:query')")
    @GetMapping("/roleMenuTreeselect/{sellerId}/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("sellerId") Long sellerId, @PathVariable("roleId") Long roleId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", permissionService.selectSelfManagementMenuIdsByRoleId(sellerId, roleId));
        ajax.put("menus", permissionService.buildSelfManagementMenuTreeSelect());
        return ajax;
    }
}
