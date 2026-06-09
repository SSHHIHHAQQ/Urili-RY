package com.ruoyi.inventory.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewOperationLog;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicy;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicyBinding;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewRequest;
import com.ruoyi.inventory.domain.request.InventoryAdjustmentReviewActionRequest;
import com.ruoyi.inventory.service.IInventoryAdjustmentReviewService;

/**
 * 管理端库存调整审核。
 */
@RestController
@RequestMapping("/inventory/admin/adjustment-reviews")
public class AdminInventoryAdjustmentReviewController extends BaseController
{
    @Autowired
    private IInventoryAdjustmentReviewService inventoryAdjustmentReviewService;

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:list')")
    @GetMapping("/list")
    public TableDataInfo list(InventoryAdjustmentReviewRequest query)
    {
        startPage();
        List<InventoryAdjustmentReviewRequest> list = inventoryAdjustmentReviewService.selectReviewList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:query')")
    @GetMapping("/{reviewId}")
    public AjaxResult getInfo(@PathVariable("reviewId") Long reviewId)
    {
        return success(inventoryAdjustmentReviewService.selectReviewById(reviewId));
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:log')")
    @GetMapping("/{reviewId}/logs")
    public AjaxResult logs(@PathVariable("reviewId") Long reviewId)
    {
        List<InventoryAdjustmentReviewOperationLog> logs = inventoryAdjustmentReviewService.selectReviewLogs(reviewId);
        return success(logs);
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:effect')")
    @Log(title = "库存调整审核立即生效", businessType = BusinessType.UPDATE)
    @PostMapping("/{reviewId}/effect-now")
    public AjaxResult effectNow(@PathVariable("reviewId") Long reviewId,
        @RequestBody(required = false) InventoryAdjustmentReviewActionRequest request)
    {
        return success(inventoryAdjustmentReviewService.effectNow(reviewId, request));
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:edit')")
    @Log(title = "库存调整审核修改生效时间", businessType = BusinessType.UPDATE)
    @PostMapping("/{reviewId}/effective-time")
    public AjaxResult changeEffectiveTime(@PathVariable("reviewId") Long reviewId,
        @RequestBody InventoryAdjustmentReviewActionRequest request)
    {
        return success(inventoryAdjustmentReviewService.changeEffectiveTime(reviewId, request));
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:reject')")
    @Log(title = "库存调整审核驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/{reviewId}/reject")
    public AjaxResult reject(@PathVariable("reviewId") Long reviewId,
        @RequestBody(required = false) InventoryAdjustmentReviewActionRequest request)
    {
        return success(inventoryAdjustmentReviewService.reject(reviewId, request));
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:config')")
    @GetMapping("/policies/list")
    public TableDataInfo policyList(InventoryAdjustmentReviewPolicy query)
    {
        startPage();
        List<InventoryAdjustmentReviewPolicy> list = inventoryAdjustmentReviewService.selectPolicyList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:config')")
    @Log(title = "库存调整审核策略", businessType = BusinessType.INSERT)
    @PostMapping("/policies")
    public AjaxResult addPolicy(@RequestBody InventoryAdjustmentReviewPolicy policy)
    {
        return toAjax(inventoryAdjustmentReviewService.savePolicy(policy));
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:config')")
    @Log(title = "库存调整审核策略", businessType = BusinessType.UPDATE)
    @PutMapping("/policies/{policyId}")
    public AjaxResult editPolicy(@PathVariable("policyId") Long policyId,
        @RequestBody InventoryAdjustmentReviewPolicy policy)
    {
        policy.setPolicyId(policyId);
        return toAjax(inventoryAdjustmentReviewService.savePolicy(policy));
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:config')")
    @GetMapping("/policy-bindings/list")
    public TableDataInfo policyBindingList(InventoryAdjustmentReviewPolicyBinding query)
    {
        startPage();
        List<InventoryAdjustmentReviewPolicyBinding> list = inventoryAdjustmentReviewService.selectPolicyBindingList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:config')")
    @Log(title = "库存调整审核策略绑定", businessType = BusinessType.INSERT)
    @PostMapping("/policy-bindings")
    public AjaxResult addPolicyBinding(@RequestBody InventoryAdjustmentReviewPolicyBinding binding)
    {
        return toAjax(inventoryAdjustmentReviewService.savePolicyBinding(binding));
    }

    @PreAuthorize("@ss.hasPermi('review:inventoryAdjustment:config')")
    @Log(title = "库存调整审核策略绑定", businessType = BusinessType.UPDATE)
    @PutMapping("/policy-bindings/{bindingId}")
    public AjaxResult editPolicyBinding(@PathVariable("bindingId") Long bindingId,
        @RequestBody InventoryAdjustmentReviewPolicyBinding binding)
    {
        binding.setBindingId(bindingId);
        return toAjax(inventoryAdjustmentReviewService.savePolicyBinding(binding));
    }
}
