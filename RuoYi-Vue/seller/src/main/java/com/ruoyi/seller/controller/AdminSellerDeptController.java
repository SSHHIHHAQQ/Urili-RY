package com.ruoyi.seller.controller;

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
import com.ruoyi.seller.service.ISellerPortalDeptService;
import com.ruoyi.system.domain.PortalDept;

/**
 * Admin control for seller terminal departments.
 */
@RestController
@RequestMapping("/seller/admin/sellers/{sellerId}/depts")
public class AdminSellerDeptController extends BaseController
{
    @Autowired
    private ISellerPortalDeptService deptService;

    @PreAuthorize("@ss.hasPermi('seller:admin:dept:list')")
    @GetMapping("/list")
    public AjaxResult list(@PathVariable("sellerId") Long sellerId, PortalDept dept)
    {
        return success(deptService.selectDeptList(sellerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:dept:query')")
    @GetMapping("/{deptId}")
    public AjaxResult getInfo(@PathVariable("sellerId") Long sellerId, @PathVariable("deptId") Long deptId)
    {
        return success(deptService.selectDeptById(sellerId, deptId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:dept:query')")
    @GetMapping("/treeselect")
    public AjaxResult treeselect(@PathVariable("sellerId") Long sellerId, PortalDept dept)
    {
        return success(deptService.buildDeptTreeSelect(sellerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:dept:add')")
    @Log(title = "卖家端部门", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@PathVariable("sellerId") Long sellerId, @Validated @RequestBody PortalDept dept)
    {
        return toAjax(deptService.insertDept(sellerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:dept:edit')")
    @Log(title = "卖家端部门", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@PathVariable("sellerId") Long sellerId, @Validated @RequestBody PortalDept dept)
    {
        return toAjax(deptService.updateDept(sellerId, dept));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:dept:remove')")
    @Log(title = "卖家端部门", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable("sellerId") Long sellerId, @PathVariable("deptId") Long deptId)
    {
        return toAjax(deptService.deleteDeptById(sellerId, deptId));
    }
}
