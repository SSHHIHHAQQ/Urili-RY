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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.system.domain.PortalAccountRoleAssign;
import com.ruoyi.system.domain.PortalDirectLoginRequest;
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalSessionProfile;

/**
 * 管理端买家管理
 */
@RestController
@RequestMapping("/buyer/admin/buyers")
public class AdminBuyerController extends BaseController
{
    @Autowired
    private IBuyerService buyerService;

    @Autowired
    private IBuyerPortalPermissionService permissionService;

    @PreAuthorize("@ss.hasPermi('buyer:admin:list')")
    @GetMapping("/list")
    public TableDataInfo list(Buyer buyer)
    {
        startPage();
        List<Buyer> list = buyerService.selectBuyerList(buyer);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:query')")
    @GetMapping("/{buyerId}")
    public AjaxResult get(@PathVariable("buyerId") Long buyerId)
    {
        return success(buyerService.selectBuyerById(buyerId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:add')")
    @Log(title = "买家管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Buyer buyer)
    {
        return toAjax(buyerService.insertBuyer(buyer));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:edit')")
    @Log(title = "买家管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Buyer buyer)
    {
        return toAjax(buyerService.updateBuyer(buyer));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:changeStatus')")
    @Log(title = "买家管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody Buyer buyer)
    {
        return toAjax(buyerService.updateBuyerStatus(buyer));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:list')")
    @GetMapping("/{buyerId}/accounts")
    public AjaxResult accounts(@PathVariable("buyerId") Long buyerId)
    {
        return success(buyerService.selectBuyerAccountList(buyerId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:role:query')")
    @GetMapping("/{buyerId}/accounts/{accountId}/roles")
    public AjaxResult accountRoles(@PathVariable("buyerId") Long buyerId, @PathVariable("accountId") Long accountId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("roles", permissionService.selectRoleAll(buyerId));
        ajax.put("checkedKeys", permissionService.selectAccountRoleIds(buyerId, accountId));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:role:edit')")
    @Log(title = "买家端账号角色", businessType = BusinessType.GRANT)
    @PutMapping("/{buyerId}/accounts/{accountId}/roles")
    public AjaxResult assignAccountRoles(@PathVariable("buyerId") Long buyerId, @PathVariable("accountId") Long accountId,
            @RequestBody PortalAccountRoleAssign assign)
    {
        return toAjax(permissionService.assignAccountRoles(buyerId, accountId, assign == null ? null : assign.getRoleIds()));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:add')")
    @Log(title = "买家账号", businessType = BusinessType.INSERT)
    @PostMapping("/{buyerId}/accounts")
    public AjaxResult addAccount(@PathVariable("buyerId") Long buyerId, @Validated @RequestBody BuyerAccount account)
    {
        return toAjax(buyerService.insertBuyerAccount(buyerId, account));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:edit')")
    @Log(title = "买家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/{buyerId}/accounts")
    public AjaxResult editAccount(@PathVariable("buyerId") Long buyerId, @RequestBody BuyerAccount account)
    {
        return toAjax(buyerService.updateBuyerAccount(buyerId, account));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:lock')")
    @Log(title = "买家账号锁定", businessType = BusinessType.UPDATE)
    @PutMapping("/{buyerId}/accounts/{accountId}/lock")
    public AjaxResult lockAccount(@PathVariable("buyerId") Long buyerId, @PathVariable("accountId") Long accountId,
            @RequestBody(required = false) BuyerAccount account)
    {
        return toAjax(buyerService.lockBuyerAccount(buyerId, accountId, account == null ? null : account.getLockReason()));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:lock')")
    @Log(title = "买家账号解锁", businessType = BusinessType.UPDATE)
    @PutMapping("/{buyerId}/accounts/{accountId}/unlock")
    public AjaxResult unlockAccount(@PathVariable("buyerId") Long buyerId, @PathVariable("accountId") Long accountId)
    {
        return toAjax(buyerService.unlockBuyerAccount(buyerId, accountId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:resetPwd')")
    @Log(title = "买家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/resetPwd")
    public AjaxResult resetPassword(@RequestBody BuyerAccount account)
    {
        return toAjax(buyerService.resetBuyerAccountPassword(account));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:account:resetPwd')")
    @Log(title = "买家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/resetDefaultPwd")
    public AjaxResult resetDefaultPassword(@RequestBody BuyerAccount account)
    {
        return toAjax(buyerService.resetBuyerAccountDefaultPassword(account));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:resetPwd')")
    @Log(title = "买家主账号", businessType = BusinessType.UPDATE)
    @PutMapping("/{buyerId}/resetOwnerPwd")
    public AjaxResult resetOwnerPassword(@PathVariable("buyerId") Long buyerId)
    {
        return toAjax(buyerService.resetBuyerOwnerPassword(buyerId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:forceLogout')")
    @GetMapping("/{buyerId}/sessions/list")
    public TableDataInfo sessions(@PathVariable("buyerId") Long buyerId)
    {
        startPage();
        List<PortalSessionProfile> list = buyerService.selectBuyerSessionList(buyerId);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:forceLogout')")
    @GetMapping("/{buyerId}/accounts/{accountId}/sessions/list")
    public TableDataInfo accountSessions(@PathVariable("buyerId") Long buyerId, @PathVariable("accountId") Long accountId)
    {
        startPage();
        List<PortalSessionProfile> list = buyerService.selectBuyerAccountSessionList(buyerId, accountId);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:forceLogout')")
    @Log(title = "买家端会话", businessType = BusinessType.FORCE)
    @DeleteMapping("/{buyerId}/sessions")
    public AjaxResult forceLogoutBuyer(@PathVariable("buyerId") Long buyerId)
    {
        return success(buyerService.forceLogoutBuyerSessions(buyerId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:forceLogout')")
    @Log(title = "买家端账号会话", businessType = BusinessType.FORCE)
    @DeleteMapping("/{buyerId}/accounts/{accountId}/sessions")
    public AjaxResult forceLogoutBuyerAccount(@PathVariable("buyerId") Long buyerId, @PathVariable("accountId") Long accountId)
    {
        return success(buyerService.forceLogoutBuyerAccountSessions(buyerId, accountId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:directLogin')")
    @Log(title = "买家免密登录", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @PostMapping("/{buyerId}/directLogin")
    public AjaxResult directLogin(@PathVariable("buyerId") Long buyerId,
            @RequestBody(required = false) PortalDirectLoginRequest request)
    {
        return success(buyerService.createBuyerDirectLogin(buyerId, request == null ? null : request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:directLogin')")
    @Log(title = "买家账号免密登录", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @PostMapping("/{buyerId}/accounts/{accountId}/directLogin")
    public AjaxResult accountDirectLogin(@PathVariable("buyerId") Long buyerId, @PathVariable("accountId") Long accountId,
            @RequestBody(required = false) PortalDirectLoginRequest request)
    {
        return success(buyerService.createBuyerAccountDirectLogin(buyerId, accountId,
            request == null ? null : request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:loginLog:list')")
    @GetMapping("/loginLogs/list")
    public TableDataInfo loginLogs(PortalLoginLog log)
    {
        startPage();
        List<PortalLoginLog> list = buyerService.selectBuyerLoginLogList(log);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:operLog:list')")
    @GetMapping("/operLogs/list")
    public TableDataInfo operLogs(PortalOperLog log)
    {
        startPage();
        List<PortalOperLog> list = buyerService.selectBuyerOperLogList(log);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:ticket:list')")
    @GetMapping("/directLoginTickets/list")
    public TableDataInfo directLoginTickets(PortalDirectLoginTicket ticket)
    {
        startPage();
        List<PortalDirectLoginTicket> list = buyerService.selectBuyerDirectLoginTicketList(ticket);
        return getDataTable(list);
    }
}
