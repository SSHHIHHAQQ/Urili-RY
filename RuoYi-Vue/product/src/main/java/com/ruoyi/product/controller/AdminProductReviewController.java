package com.ruoyi.product.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.product.domain.ProductReviewActionRequest;
import com.ruoyi.product.domain.ProductReviewOperationLog;
import com.ruoyi.product.domain.ProductReviewRequest;
import com.ruoyi.product.service.IProductReviewService;

/**
 * 管理端商品审核。
 */
@RestController
@RequestMapping("/product/admin/reviews")
public class AdminProductReviewController extends BaseController
{
    @Autowired
    private IProductReviewService productReviewService;

    @PreAuthorize("@ss.hasPermi('review:productDistribution:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductReviewRequest query)
    {
        startPage();
        List<ProductReviewRequest> list = productReviewService.selectReviewList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:productDistribution:list')")
    @GetMapping("/pending-counts")
    public AjaxResult pendingCounts()
    {
        return success(productReviewService.selectPendingReviewTypeCounts());
    }

    @PreAuthorize("@ss.hasPermi('review:productDistribution:query')")
    @GetMapping("/{reviewId}")
    public AjaxResult get(@PathVariable("reviewId") Long reviewId)
    {
        return success(productReviewService.selectReviewById(reviewId));
    }

    @PreAuthorize("@ss.hasPermi('review:productDistribution:approve')")
    @Log(title = "商品审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{reviewId}/approve")
    public AjaxResult approve(@PathVariable("reviewId") Long reviewId,
        @RequestBody(required = false) ProductReviewActionRequest request)
    {
        String reason = request == null ? "" : request.getReason();
        return toAjax(productReviewService.approveReview(reviewId, reason));
    }

    @PreAuthorize("@ss.hasPermi('review:productDistribution:reject')")
    @Log(title = "商品审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{reviewId}/reject")
    public AjaxResult reject(@PathVariable("reviewId") Long reviewId,
        @RequestBody ProductReviewActionRequest request)
    {
        return toAjax(productReviewService.rejectReview(reviewId, request == null ? null : request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('review:productDistribution:log')")
    @GetMapping("/{reviewId}/logs")
    public AjaxResult logs(@PathVariable("reviewId") Long reviewId)
    {
        List<ProductReviewOperationLog> logs = productReviewService.selectReviewOperationLogs(reviewId);
        return success(logs);
    }
}
