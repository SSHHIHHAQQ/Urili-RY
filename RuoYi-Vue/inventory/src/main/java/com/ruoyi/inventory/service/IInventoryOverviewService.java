package com.ruoyi.inventory.service;

import java.util.List;
import com.ruoyi.inventory.domain.InventoryOverviewBatchAdjustPreviewResult;
import com.ruoyi.inventory.domain.InventoryOverviewAdjustPreviewResult;
import com.ruoyi.inventory.domain.InventoryOverviewItem;
import com.ruoyi.inventory.domain.InventoryOverviewSellerOption;
import com.ruoyi.inventory.domain.InventoryOverviewSkuWarehouseGroup;
import com.ruoyi.inventory.domain.InventoryOverviewWarehouseOption;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;
import com.ruoyi.inventory.domain.request.InventoryOverviewBatchAdjustRequest;

/**
 * 库存总览服务。
 */
public interface IInventoryOverviewService
{
    List<InventoryOverviewItem> selectSpuList(InventoryOverviewItem query);

    List<InventoryOverviewItem> selectSkuList(InventoryOverviewItem query);

    List<InventorySkuWarehouseStock> selectWarehouseStockList(InventorySkuWarehouseStock query);

    List<InventoryOverviewWarehouseOption> selectWarehouseOptions();

    List<InventoryOverviewWarehouseOption> selectOfficialWarehouseOptions();

    List<InventoryOverviewSellerOption> selectSellerOptions();

    List<InventorySkuWarehouseStock> selectWarehouseStockListBySkuId(Long skuId);

    List<InventoryOverviewSkuWarehouseGroup> selectSkuWarehouseGroupsBySpuId(Long spuId);

    InventoryOverviewAdjustPreviewResult previewAdjust(InventoryOverviewAdjustRequest request);

    InventoryOverviewAdjustPreviewResult confirmAdjust(InventoryOverviewAdjustRequest request);

    InventoryOverviewBatchAdjustPreviewResult previewBatchAdjust(InventoryOverviewBatchAdjustRequest request);

    InventoryOverviewBatchAdjustPreviewResult confirmBatchAdjust(InventoryOverviewBatchAdjustRequest request);

    List<InventoryStockLedger> selectLedgerList(InventoryStockLedger query);

    void refreshProductInventoryOverview(Long spuId);

    List<Long> selectSourceInventoryOverviewSpuIdsByConnection(String connectionCode);

    void refreshSourceInventoryOverviewByConnection(String connectionCode);

    void refreshSourceInventoryOverviewByConnection(String connectionCode, List<Long> preRebuildSpuIds);
}
