package com.ruoyi.buyer.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.buyer.service.IBuyerService;

/**
 * 管理端买家管理
 */
@RestController
@RequestMapping("/buyer/admin/buyers")
public class AdminBuyerController extends BaseController
{
    @Autowired
    private IBuyerService buyerService;

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

    @PreAuthorize("@ss.hasPermi('buyer:admin:query')")
    @GetMapping("/{buyerId}/accounts")
    public AjaxResult accounts(@PathVariable("buyerId") Long buyerId)
    {
        return success(buyerService.selectBuyerAccountList(buyerId));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:add')")
    @Log(title = "买家账号", businessType = BusinessType.INSERT)
    @PostMapping("/{buyerId}/accounts")
    public AjaxResult addAccount(@PathVariable("buyerId") Long buyerId, @Validated @RequestBody BuyerAccount account)
    {
        return toAjax(buyerService.insertBuyerAccount(buyerId, account));
    }

    @PreAuthorize("@ss.hasPermi('buyer:admin:resetPwd')")
    @Log(title = "买家账号", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/resetPwd")
    public AjaxResult resetPassword(@RequestBody BuyerAccount account)
    {
        return toAjax(buyerService.resetBuyerAccountPassword(account));
    }
}
