package com.ruoyi.buyer.controller;

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
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
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
        ajax.put("checkedKeys", permissionService.selectMenuIdsByRoleId(buyerId, roleId));
        ajax.put("menus", permissionService.buildMenuTreeSelect(new PortalMenu()));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:menu:add')")
    @Log(title = "买家端菜单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalMenu menu)
    {
        return toAjax(permissionService.insertMenu(menu));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:menu:edit')")
    @Log(title = "买家端菜单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalMenu menu)
    {
        return toAjax(permissionService.updateMenu(menu));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:menu:remove')")
    @Log(title = "买家端菜单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public AjaxResult remove(@PathVariable("menuId") Long menuId)
    {
        return toAjax(permissionService.deleteMenuById(menuId));
    }
}
