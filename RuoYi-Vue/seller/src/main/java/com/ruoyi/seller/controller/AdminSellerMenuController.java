package com.ruoyi.seller.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
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

    @PreAuthorize("@ss.hasPermi('seller:admin:role:query')")
    @GetMapping("/roleMenuTreeselect/{sellerId}/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("sellerId") Long sellerId, @PathVariable("roleId") Long roleId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", permissionService.selectMenuIdsByRoleId(sellerId, roleId));
        ajax.put("menus", permissionService.buildMenuTreeSelect(new PortalMenu()));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:menu:add')")
    @Log(title = "卖家端菜单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalMenu menu)
    {
        return toAjax(permissionService.insertMenu(menu));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:menu:edit')")
    @Log(title = "卖家端菜单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalMenu menu)
    {
        return toAjax(permissionService.updateMenu(menu));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:menu:remove')")
    @Log(title = "卖家端菜单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public AjaxResult remove(@PathVariable("menuId") Long menuId)
    {
        return toAjax(permissionService.deleteMenuById(menuId));
    }
}
