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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
import com.ruoyi.system.domain.PortalRole;

/**
 * Admin control for seller terminal roles.
 */
@RestController
@RequestMapping("/seller/admin/sellers/{sellerId}/roles")
public class AdminSellerRoleController extends BaseController
{
    @Autowired
    private ISellerPortalPermissionService permissionService;

    @PreAuthorize("@ss.hasPermi('seller:admin:role:list')")
    @GetMapping("/list")
    public TableDataInfo list(@PathVariable("sellerId") Long sellerId, PortalRole role)
    {
        startPage();
        List<PortalRole> list = permissionService.selectRoleList(sellerId, role);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:query')")
    @GetMapping("/{roleId}")
    public AjaxResult getInfo(@PathVariable("sellerId") Long sellerId, @PathVariable("roleId") Long roleId)
    {
        return success(permissionService.selectRoleById(sellerId, roleId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:add')")
    @Log(title = "卖家端角色", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@PathVariable("sellerId") Long sellerId, @Validated @RequestBody PortalRole role)
    {
        return toAjax(permissionService.insertRole(sellerId, role));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:edit')")
    @Log(title = "卖家端角色", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@PathVariable("sellerId") Long sellerId, @Validated @RequestBody PortalRole role)
    {
        return toAjax(permissionService.updateRole(sellerId, role));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:edit')")
    @Log(title = "卖家端角色", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@PathVariable("sellerId") Long sellerId, @RequestBody PortalRole role)
    {
        return toAjax(permissionService.updateRoleStatus(sellerId, role));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:remove')")
    @Log(title = "卖家端角色", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable("sellerId") Long sellerId, @PathVariable Long[] roleIds)
    {
        return toAjax(permissionService.deleteRoleByIds(sellerId, roleIds));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:query')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(@PathVariable("sellerId") Long sellerId)
    {
        return success(permissionService.selectRoleAll(sellerId));
    }
}
