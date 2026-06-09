package com.ruoyi.inventory.service;

import java.util.List;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewDecision;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewOperationLog;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicy;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicyBinding;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewRequest;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.request.InventoryAdjustmentReviewActionRequest;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;

/**
 * 库存调整审核服务。
 */
public interface IInventoryAdjustmentReviewService
{
    InventoryAdjustmentReviewDecision evaluateAdjustment(InventorySkuWarehouseStock stock,
        InventoryOverviewAdjustRequest request);

    InventoryAdjustmentReviewRequest submitAdjustmentReview(InventorySkuWarehouseStock stock,
        InventoryOverviewAdjustRequest request, InventoryAdjustmentReviewDecision decision);

    List<InventoryAdjustmentReviewRequest> selectReviewList(InventoryAdjustmentReviewRequest query);

    InventoryAdjustmentReviewRequest selectReviewById(Long reviewId);

    List<InventoryAdjustmentReviewOperationLog> selectReviewLogs(Long reviewId);

    InventoryAdjustmentReviewRequest effectNow(Long reviewId, InventoryAdjustmentReviewActionRequest request);

    InventoryAdjustmentReviewRequest reject(Long reviewId, InventoryAdjustmentReviewActionRequest request);

    InventoryAdjustmentReviewRequest changeEffectiveTime(Long reviewId, InventoryAdjustmentReviewActionRequest request);

    int effectDueReviews();

    List<InventoryAdjustmentReviewPolicy> selectPolicyList(InventoryAdjustmentReviewPolicy query);

    int savePolicy(InventoryAdjustmentReviewPolicy policy);

    List<InventoryAdjustmentReviewPolicyBinding> selectPolicyBindingList(InventoryAdjustmentReviewPolicyBinding query);

    int savePolicyBinding(InventoryAdjustmentReviewPolicyBinding binding);
}
