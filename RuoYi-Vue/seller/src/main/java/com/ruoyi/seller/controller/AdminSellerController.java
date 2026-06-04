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
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.system.domain.PortalAccountRoleAssign;

/**
 * 管理端卖家管理
 */
@RestController
@RequestMapping("/seller/admin/sellers")
public class AdminSellerController extends BaseController
{
    @Autowired
    private ISellerService sellerService;

    @Autowired
    private ISellerPortalPermissionService permissionService;

    @PreAuthorize("@ss.hasPermi('seller:admin:list')")
    @GetMapping("/list")
    public TableDataInfo list(Seller seller)
    {
        startPage();
        List<Seller> list = sellerService.selectSellerList(seller);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:query')")
    @GetMapping("/{sellerId}")
    public AjaxResult get(@PathVariable("sellerId") Long sellerId)
    {
        return success(sellerService.selectSellerById(sellerId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:add')")
    @Log(title = "卖家管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Seller seller)
    {
        return toAjax(sellerService.insertSeller(seller));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:edit')")
    @Log(title = "卖家管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Seller seller)
    {
        return toAjax(sellerService.updateSeller(seller));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:changeStatus')")
    @Log(title = "卖家管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody Seller seller)
    {
        return toAjax(sellerService.updateSellerStatus(seller));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:query')")
    @GetMapping("/{sellerId}/accounts")
    public AjaxResult accounts(@PathVariable("sellerId") Long sellerId)
    {
        return success(sellerService.selectSellerAccountList(sellerId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:query')")
    @GetMapping("/{sellerId}/accounts/{accountId}/roles")
    public AjaxResult accountRoles(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("roles", permissionService.selectRoleAll(sellerId));
        ajax.put("checkedKeys", permissionService.selectAccountRoleIds(sellerId, accountId));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:role:edit')")
    @Log(title = "卖家端账号角色", businessType = BusinessType.GRANT)
    @PutMapping("/{sellerId}/accounts/{accountId}/roles")
    public AjaxResult assignAccountRoles(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId,
            @RequestBody PortalAccountRoleAssign assign)
    {
        return toAjax(permissionService.assignAccountRoles(sellerId, accountId, assign == null ? null : assign.getRoleIds()));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:add')")
    @Log(title = "卖家账号", businessType = BusinessType.INSERT)
    @PostMapping("/{sellerId}/accounts")
    public AjaxResult addAccount(@PathVariable("sellerId") Long sellerId, @Validated @RequestBody SellerAccount account)
    {
        return toAjax(sellerService.insertSellerAccount(sellerId, account));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:edit')")
    @Log(title = "鍗栧璐﹀彿", businessType = BusinessType.UPDATE)
    @PutMapping("/{sellerId}/accounts")
    public AjaxResult editAccount(@PathVariable("sellerId") Long sellerId, @RequestBody SellerAccount account)
    {
        return toAjax(sellerService.updateSellerAccount(sellerId, account));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:resetPwd')")
    @Log(title = "卖家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/resetPwd")
    public AjaxResult resetPassword(@RequestBody SellerAccount account)
    {
        return toAjax(sellerService.resetSellerAccountPassword(account));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:resetPwd')")
    @Log(title = "卖家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/resetDefaultPwd")
    public AjaxResult resetDefaultPassword(@RequestBody SellerAccount account)
    {
        return toAjax(sellerService.resetSellerAccountDefaultPassword(account));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:resetPwd')")
    @Log(title = "卖家主账号", businessType = BusinessType.UPDATE)
    @PutMapping("/{sellerId}/resetOwnerPwd")
    public AjaxResult resetOwnerPassword(@PathVariable("sellerId") Long sellerId)
    {
        return toAjax(sellerService.resetSellerOwnerPassword(sellerId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:forceLogout')")
    @Log(title = "卖家端会话", businessType = BusinessType.FORCE)
    @DeleteMapping("/{sellerId}/sessions")
    public AjaxResult forceLogoutSeller(@PathVariable("sellerId") Long sellerId)
    {
        return success(sellerService.forceLogoutSellerSessions(sellerId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:forceLogout')")
    @Log(title = "卖家端账号会话", businessType = BusinessType.FORCE)
    @DeleteMapping("/{sellerId}/accounts/{accountId}/sessions")
    public AjaxResult forceLogoutSellerAccount(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId)
    {
        return success(sellerService.forceLogoutSellerAccountSessions(sellerId, accountId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:directLogin')")
    @Log(title = "卖家免密登录", businessType = BusinessType.OTHER)
    @PostMapping("/{sellerId}/directLogin")
    public AjaxResult directLogin(@PathVariable("sellerId") Long sellerId)
    {
        return success(sellerService.createSellerDirectLogin(sellerId));
    }
}
