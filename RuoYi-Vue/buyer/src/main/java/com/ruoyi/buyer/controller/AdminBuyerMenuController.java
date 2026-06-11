package com.ruoyi.buyer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.PortalMenu;

/**
 * Admin control for buyer terminal menus.
 */
@RestController
@RequestMapping("/buyer/admin/menus")
public class AdminBuyerMenuController extends BaseController
{
    @Autowired
    private IBuyerPortalPermissionService permissionService;

    @PreAuthorize("@ss.hasPermi('buyer:admin:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalMenu menu)
    {
        return success(permissionService.selectMenuList(menu));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:menu:query')")
    @GetMapping("/{menuId}")
    public AjaxResult getInfo(@PathVariable Long menuId)
    {
        return success(permissionService.selectMenuById(menuId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:menu:query')")
    @GetMapping("/treeselect")
    public AjaxResult treeselect(PortalMenu menu)
    {
        return success(permissionService.buildMenuTreeSelect(menu));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:query') and @ss.hasPermi('buyer:admin:menu:query')")
    @GetMapping("/roleMenuTreeselect/{buyerId}/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("buyerId") Long buyerId, @PathVariable("roleId") Long roleId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", permissionService.selectSelfManagementMenuIdsByRoleId(buyerId, roleId));
        ajax.put("menus", permissionService.buildSelfManagementMenuTreeSelect());
        return ajax;
    }
}
