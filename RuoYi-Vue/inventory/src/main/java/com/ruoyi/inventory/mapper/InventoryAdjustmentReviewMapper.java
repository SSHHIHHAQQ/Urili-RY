package com.ruoyi.inventory.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewOperationLog;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicy;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicyBinding;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewRequest;

/**
 * 库存调整审核 Mapper。
 */
public interface InventoryAdjustmentReviewMapper
{
    List<InventoryAdjustmentReviewRequest> selectReviewList(InventoryAdjustmentReviewRequest query);

    InventoryAdjustmentReviewRequest selectReviewById(@Param("reviewId") Long reviewId);

    int insertReview(InventoryAdjustmentReviewRequest review);

    int updateReviewStatus(InventoryAdjustmentReviewRequest review);

    int updateReviewPlannedEffectiveTime(InventoryAdjustmentReviewRequest review);

    List<InventoryAdjustmentReviewOperationLog> selectReviewLogs(@Param("reviewId") Long reviewId);

    int insertReviewLog(InventoryAdjustmentReviewOperationLog log);

    List<InventoryAdjustmentReviewRequest> selectDueWaitingReviews(@Param("now") java.util.Date now,
        @Param("limit") Integer limit);

    List<InventoryAdjustmentReviewPolicy> selectPolicyList(InventoryAdjustmentReviewPolicy query);

    InventoryAdjustmentReviewPolicy selectPolicyById(@Param("policyId") Long policyId);

    InventoryAdjustmentReviewPolicy selectMatchedPolicy(@Param("sellerId") Long sellerId);

    int insertPolicy(InventoryAdjustmentReviewPolicy policy);

    int updatePolicy(InventoryAdjustmentReviewPolicy policy);

    List<InventoryAdjustmentReviewPolicyBinding> selectPolicyBindingList(InventoryAdjustmentReviewPolicyBinding query);

    int insertPolicyBinding(InventoryAdjustmentReviewPolicyBinding binding);

    int updatePolicyBinding(InventoryAdjustmentReviewPolicyBinding binding);

    Long sumSalesQty(@Param("skuId") Long skuId, @Param("days") Integer days);
}
