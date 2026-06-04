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
import com.ruoyi.buyer.service.IBuyerPortalDeptService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.PortalDept;

/**
 * Admin control for buyer terminal departments.
 */
@RestController
@RequestMapping("/buyer/admin/buyers/{buyerId}/depts")
public class AdminBuyerDeptController extends BaseController
{
    @Autowired
    private IBuyerPortalDeptService deptService;

    @PreAuthorize("@ss.hasPermi('buyer:admin:dept:list')")
    @GetMapping("/list")
    public AjaxResult list(@PathVariable("buyerId") Long buyerId, PortalDept dept)
    {
        return success(deptService.selectDeptList(buyerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:dept:query')")
    @GetMapping("/{deptId}")
    public AjaxResult getInfo(@PathVariable("buyerId") Long buyerId, @PathVariable("deptId") Long deptId)
    {
        return success(deptService.selectDeptById(buyerId, deptId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:dept:query')")
    @GetMapping("/treeselect")
    public AjaxResult treeselect(@PathVariable("buyerId") Long buyerId, PortalDept dept)
    {
        return success(deptService.buildDeptTreeSelect(buyerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:dept:add')")
    @Log(title = "买家端部门", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@PathVariable("buyerId") Long buyerId, @Validated @RequestBody PortalDept dept)
    {
        return toAjax(deptService.insertDept(buyerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:dept:edit')")
    @Log(title = "买家端部门", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@PathVariable("buyerId") Long buyerId, @Validated @RequestBody PortalDept dept)
    {
        return toAjax(deptService.updateDept(buyerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:dept:remove')")
    @Log(title = "买家端部门", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable("buyerId") Long buyerId, @PathVariable("deptId") Long deptId)
    {
        return toAjax(deptService.deleteDeptById(buyerId, deptId));
    }
}
