package com.ruoyi.inventory.service;

import com.ruoyi.inventory.domain.InventoryStockSyncPolicyPreviewResult;
import com.ruoyi.inventory.domain.request.InventoryStockSyncPolicyRequest;

/**
 * 库存同步策略服务。
 */
public interface IInventoryStockSyncPolicyService
{
    InventoryStockSyncPolicyPreviewResult previewSyncPolicy(InventoryStockSyncPolicyRequest request);

    InventoryStockSyncPolicyPreviewResult confirmSyncPolicy(InventoryStockSyncPolicyRequest request);

    void applyAutoSyncForSpu(Long spuId, String operator);
}
