package com.ruoyi.buyer.controller;

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
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.PortalRole;

/**
 * Admin control for buyer terminal roles.
 */
@RestController
@RequestMapping("/buyer/admin/buyers/{buyerId}/roles")
public class AdminBuyerRoleController extends BaseController
{
    @Autowired
    private IBuyerPortalPermissionService permissionService;

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:list')")
    @GetMapping("/list")
    public TableDataInfo list(@PathVariable("buyerId") Long buyerId, PortalRole role)
    {
        startPage();
        List<PortalRole> list = permissionService.selectRoleList(buyerId, role);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:query')")
    @GetMapping("/{roleId}")
    public AjaxResult getInfo(@PathVariable("buyerId") Long buyerId, @PathVariable("roleId") Long roleId)
    {
        return success(permissionService.selectRoleById(buyerId, roleId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:add')")
    @Log(title = "买家端角色", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@PathVariable("buyerId") Long buyerId, @Validated @RequestBody PortalRole role)
    {
        return toAjax(permissionService.insertRole(buyerId, role));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:edit')")
    @Log(title = "买家端角色", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@PathVariable("buyerId") Long buyerId, @Validated @RequestBody PortalRole role)
    {
        return toAjax(permissionService.updateRole(buyerId, role));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:edit')")
    @Log(title = "买家端角色", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@PathVariable("buyerId") Long buyerId, @RequestBody PortalRole role)
    {
        return toAjax(permissionService.updateRoleStatus(buyerId, role));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:remove')")
    @Log(title = "买家端角色", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable("buyerId") Long buyerId, @PathVariable Long[] roleIds)
    {
        return toAjax(permissionService.deleteRoleByIds(buyerId, roleIds));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:role:query')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(@PathVariable("buyerId") Long buyerId)
    {
        return success(permissionService.selectRoleAll(buyerId));
    }
}
