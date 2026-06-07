package com.ruoyi.inventory.controller;

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
import com.ruoyi.inventory.domain.InventoryOverviewItem;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;
import com.ruoyi.inventory.service.IInventoryOverviewService;

/**
 * 管理端库存总览。
 */
@RestController
@RequestMapping("/inventory/admin/overview")
public class AdminInventoryOverviewController extends BaseController
{
    @Autowired
    private IInventoryOverviewService inventoryOverviewService;

    @PreAuthorize("@ss.hasPermi('inventory:overview:list')")
    @GetMapping("/spu/list")
    public TableDataInfo spuList(InventoryOverviewItem query)
    {
        startPage();
        List<InventoryOverviewItem> list = inventoryOverviewService.selectSpuList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('inventory:overview:list')")
    @GetMapping("/sku/list")
    public TableDataInfo skuList(InventoryOverviewItem query)
    {
        startPage();
        List<InventoryOverviewItem> list = inventoryOverviewService.selectSkuList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('inventory:overview:query')")
    @GetMapping("/sku/{skuId}/warehouses")
    public AjaxResult warehouses(@PathVariable("skuId") Long skuId)
    {
        List<InventorySkuWarehouseStock> rows = inventoryOverviewService.selectWarehouseStockListBySkuId(skuId);
        return success(rows);
    }

    @PreAuthorize("@ss.hasPermi('inventory:overview:adjust')")
    @PostMapping("/adjust/preview")
    public AjaxResult previewAdjust(@RequestBody InventoryOverviewAdjustRequest request)
    {
        return success(inventoryOverviewService.previewAdjust(request));
    }

    @PreAuthorize("@ss.hasPermi('inventory:overview:adjust')")
    @Log(title = "库存总览调整", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust/confirm")
    public AjaxResult confirmAdjust(@RequestBody InventoryOverviewAdjustRequest request)
    {
        return success(inventoryOverviewService.confirmAdjust(request));
    }

    @PreAuthorize("@ss.hasPermi('inventory:overview:ledger')")
    @GetMapping("/ledger/list")
    public TableDataInfo ledgerList(InventoryStockLedger query)
    {
        startPage();
        List<InventoryStockLedger> list = inventoryOverviewService.selectLedgerList(query);
        return getDataTable(list);
    }
}
