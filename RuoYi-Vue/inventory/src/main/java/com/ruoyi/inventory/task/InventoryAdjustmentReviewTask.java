package com.ruoyi.inventory.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.inventory.service.IInventoryAdjustmentReviewService;

/**
 * 库存调整审核定时任务入口。
 */
@Component("inventoryAdjustmentReviewTask")
public class InventoryAdjustmentReviewTask
{
    private static final Logger log = LoggerFactory.getLogger(InventoryAdjustmentReviewTask.class);

    @Autowired
    private IInventoryAdjustmentReviewService inventoryAdjustmentReviewService;

    public void effectDueReviews()
    {
        int affected = inventoryAdjustmentReviewService.effectDueReviews();
        if (affected > 0)
        {
            log.info("库存调整审核到期自动生效完成，处理数量={}", affected);
        }
    }
}
