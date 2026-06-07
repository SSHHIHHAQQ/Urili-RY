package com.ruoyi.inventory.service;

import java.util.List;
import com.ruoyi.inventory.domain.InventoryOverviewAdjustPreviewResult;
import com.ruoyi.inventory.domain.InventoryOverviewItem;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;

/**
 * 库存总览服务。
 */
public interface IInventoryOverviewService
{
    List<InventoryOverviewItem> selectSpuList(InventoryOverviewItem query);

    List<InventoryOverviewItem> selectSkuList(InventoryOverviewItem query);

    List<InventorySkuWarehouseStock> selectWarehouseStockListBySkuId(Long skuId);

    InventoryOverviewAdjustPreviewResult previewAdjust(InventoryOverviewAdjustRequest request);

    InventoryOverviewAdjustPreviewResult confirmAdjust(InventoryOverviewAdjustRequest request);

    List<InventoryStockLedger> selectLedgerList(InventoryStockLedger query);
}
