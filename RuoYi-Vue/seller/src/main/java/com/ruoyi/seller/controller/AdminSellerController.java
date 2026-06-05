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
import com.ruoyi.system.domain.PortalDirectLoginRequest;
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalSessionProfile;

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

    @PreAuthorize("@ss.hasPermi('seller:admin:account:list')")
    @GetMapping("/{sellerId}/accounts")
    public AjaxResult accounts(@PathVariable("sellerId") Long sellerId)
    {
        return success(sellerService.selectSellerAccountList(sellerId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:role:query')")
    @GetMapping("/{sellerId}/accounts/{accountId}/roles")
    public AjaxResult accountRoles(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("roles", permissionService.selectRoleAll(sellerId));
        ajax.put("checkedKeys", permissionService.selectAccountRoleIds(sellerId, accountId));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:role:edit')")
    @Log(title = "卖家端账号角色", businessType = BusinessType.GRANT)
    @PutMapping("/{sellerId}/accounts/{accountId}/roles")
    public AjaxResult assignAccountRoles(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId,
            @RequestBody PortalAccountRoleAssign assign)
    {
        return toAjax(permissionService.assignAccountRoles(sellerId, accountId, assign == null ? null : assign.getRoleIds()));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:add')")
    @Log(title = "卖家账号", businessType = BusinessType.INSERT)
    @PostMapping("/{sellerId}/accounts")
    public AjaxResult addAccount(@PathVariable("sellerId") Long sellerId, @Validated @RequestBody SellerAccount account)
    {
        return toAjax(sellerService.insertSellerAccount(sellerId, account));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:edit')")
    @Log(title = "卖家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/{sellerId}/accounts")
    public AjaxResult editAccount(@PathVariable("sellerId") Long sellerId, @RequestBody SellerAccount account)
    {
        return toAjax(sellerService.updateSellerAccount(sellerId, account));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:lock')")
    @Log(title = "卖家账号锁定", businessType = BusinessType.UPDATE)
    @PutMapping("/{sellerId}/accounts/{accountId}/lock")
    public AjaxResult lockAccount(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId,
            @RequestBody(required = false) SellerAccount account)
    {
        return toAjax(sellerService.lockSellerAccount(sellerId, accountId, account == null ? null : account.getLockReason()));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:lock')")
    @Log(title = "卖家账号解锁", businessType = BusinessType.UPDATE)
    @PutMapping("/{sellerId}/accounts/{accountId}/unlock")
    public AjaxResult unlockAccount(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId)
    {
        return toAjax(sellerService.unlockSellerAccount(sellerId, accountId));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:resetPwd')")
    @Log(title = "卖家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/resetPwd")
    public AjaxResult resetPassword(@RequestBody SellerAccount account)
    {
        return toAjax(sellerService.resetSellerAccountPassword(account));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:account:resetPwd')")
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
    @GetMapping("/{sellerId}/sessions/list")
    public TableDataInfo sessions(@PathVariable("sellerId") Long sellerId)
    {
        startPage();
        List<PortalSessionProfile> list = sellerService.selectSellerSessionList(sellerId);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:forceLogout')")
    @GetMapping("/{sellerId}/accounts/{accountId}/sessions/list")
    public TableDataInfo accountSessions(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId)
    {
        startPage();
        List<PortalSessionProfile> list = sellerService.selectSellerAccountSessionList(sellerId, accountId);
        return getDataTable(list);
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
    @Log(title = "卖家免密登录", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @PostMapping("/{sellerId}/directLogin")
    public AjaxResult directLogin(@PathVariable("sellerId") Long sellerId,
            @RequestBody(required = false) PortalDirectLoginRequest request)
    {
        return success(sellerService.createSellerDirectLogin(sellerId, request == null ? null : request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:directLogin')")
    @Log(title = "卖家账号免密登录", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @PostMapping("/{sellerId}/accounts/{accountId}/directLogin")
    public AjaxResult accountDirectLogin(@PathVariable("sellerId") Long sellerId, @PathVariable("accountId") Long accountId,
            @RequestBody(required = false) PortalDirectLoginRequest request)
    {
        return success(sellerService.createSellerAccountDirectLogin(sellerId, accountId,
            request == null ? null : request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:loginLog:list')")
    @GetMapping("/loginLogs/list")
    public TableDataInfo loginLogs(PortalLoginLog log)
    {
        startPage();
        List<PortalLoginLog> list = sellerService.selectSellerLoginLogList(log);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:operLog:list')")
    @GetMapping("/operLogs/list")
    public TableDataInfo operLogs(PortalOperLog log)
    {
        startPage();
        List<PortalOperLog> list = sellerService.selectSellerOperLogList(log);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('seller:admin:ticket:list')")
    @GetMapping("/directLoginTickets/list")
    public TableDataInfo directLoginTickets(PortalDirectLoginTicket ticket)
    {
        startPage();
        List<PortalDirectLoginTicket> list = sellerService.selectSellerDirectLoginTicketList(ticket);
        return getDataTable(list);
    }
}
