package com.ruoyi.warehouse.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.domain.request.OfficialWarehousePairingRequest;
import com.ruoyi.warehouse.domain.request.OfficialWarehouseSyncRequest;
import com.ruoyi.warehouse.domain.request.WarehouseStatusRequest;
import com.ruoyi.warehouse.service.IWarehouseService;

/**
 * 管理端仓库管理。
 */
@RestController
@RequestMapping("/warehouse/admin")
public class AdminWarehouseController extends BaseController
{
    @Autowired
    private IWarehouseService warehouseService;

    @PreAuthorize("@ss.hasPermi('warehouse:official:list')")
    @GetMapping("/official/list")
    public TableDataInfo officialList(Warehouse query)
    {
        startPage();
        List<Warehouse> list = warehouseService.selectOfficialWarehouseList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:list')")
    @GetMapping("/official/{warehouseId}")
    public AjaxResult official(@PathVariable("warehouseId") Long warehouseId)
    {
        return success(warehouseService.selectOfficialWarehouseById(warehouseId));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:add')")
    @Log(title = "官方仓库", businessType = BusinessType.INSERT)
    @PostMapping("/official")
    public AjaxResult addOfficial(@Validated @RequestBody Warehouse warehouse)
    {
        return toAjax(warehouseService.insertOfficialWarehouse(warehouse));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:edit')")
    @Log(title = "官方仓库", businessType = BusinessType.UPDATE)
    @PutMapping("/official")
    public AjaxResult editOfficial(@Validated @RequestBody Warehouse warehouse)
    {
        return toAjax(warehouseService.updateOfficialWarehouse(warehouse));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:status')")
    @Log(title = "官方仓库启停", businessType = BusinessType.UPDATE)
    @PutMapping("/official/status")
    public AjaxResult officialStatus(@Validated @RequestBody WarehouseStatusRequest request)
    {
        return toAjax(warehouseService.updateOfficialWarehouseStatus(request));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:sync')")
    @GetMapping("/official/sync-connections")
    public AjaxResult syncConnections(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(warehouseService.selectSyncConnections(keyword));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:sync')")
    @GetMapping("/official/sync-candidates")
    public AjaxResult syncCandidates(@RequestParam("connectionCode") String connectionCode,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(warehouseService.selectSyncCandidates(connectionCode, keyword));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:sync')")
    @Log(title = "官方仓库同步", businessType = BusinessType.INSERT)
    @PostMapping("/official/sync")
    public AjaxResult syncOfficial(@Validated @RequestBody OfficialWarehouseSyncRequest request)
    {
        return toAjax(warehouseService.syncOfficialWarehouse(request));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:sync')")
    @GetMapping("/official/pairing-connections")
    public AjaxResult pairingConnections(@RequestParam("pairingRole") String pairingRole,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(warehouseService.selectPairingConnections(pairingRole, keyword));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:sync')")
    @GetMapping("/official/pairing-candidates")
    public AjaxResult pairingCandidates(@RequestParam("pairingRole") String pairingRole,
        @RequestParam("connectionCode") String connectionCode,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(warehouseService.selectPairingCandidates(pairingRole, connectionCode, keyword));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:official:sync')")
    @Log(title = "官方仓库配对", businessType = BusinessType.UPDATE)
    @PostMapping("/official/{warehouseId}/pairing")
    public AjaxResult pairOfficial(@PathVariable("warehouseId") Long warehouseId,
        @Validated @RequestBody OfficialWarehousePairingRequest request)
    {
        return toAjax(warehouseService.pairOfficialWarehouse(warehouseId, request));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:thirdParty:list')")
    @GetMapping("/third-party/list")
    public TableDataInfo thirdPartyList(Warehouse query)
    {
        startPage();
        List<Warehouse> list = warehouseService.selectThirdPartyWarehouseList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('warehouse:thirdParty:list')")
    @GetMapping("/third-party/{warehouseId}")
    public AjaxResult thirdParty(@PathVariable("warehouseId") Long warehouseId)
    {
        return success(warehouseService.selectThirdPartyWarehouseById(warehouseId));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:thirdParty:add')")
    @Log(title = "第三方仓库", businessType = BusinessType.INSERT)
    @PostMapping("/third-party")
    public AjaxResult addThirdParty(@Validated @RequestBody Warehouse warehouse)
    {
        return toAjax(warehouseService.insertThirdPartyWarehouse(warehouse));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:thirdParty:edit')")
    @Log(title = "第三方仓库", businessType = BusinessType.UPDATE)
    @PutMapping("/third-party")
    public AjaxResult editThirdParty(@Validated @RequestBody Warehouse warehouse)
    {
        return toAjax(warehouseService.updateThirdPartyWarehouse(warehouse));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:thirdParty:status')")
    @Log(title = "第三方仓库启停", businessType = BusinessType.UPDATE)
    @PutMapping("/third-party/status")
    public AjaxResult thirdPartyStatus(@Validated @RequestBody WarehouseStatusRequest request)
    {
        return toAjax(warehouseService.updateThirdPartyWarehouseStatus(request));
    }

    @PreAuthorize("@ss.hasAnyPermi('warehouse:official:list,warehouse:thirdParty:list')")
    @GetMapping("/options/currencies")
    public AjaxResult currencyOptions()
    {
        return success(warehouseService.selectEnabledCurrencyOptions());
    }

    @PreAuthorize("@ss.hasAnyPermi('warehouse:official:list,warehouse:thirdParty:list')")
    @GetMapping("/options/us-states")
    public AjaxResult usStates(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(warehouseService.selectUsStateList(keyword));
    }

    @PreAuthorize("@ss.hasAnyPermi('warehouse:official:list,warehouse:thirdParty:list')")
    @GetMapping("/options/us-cities")
    public AjaxResult usCities(@RequestParam(value = "stateName", required = false) String stateName,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(warehouseService.selectUsCityList(stateName, keyword));
    }

    @PreAuthorize("@ss.hasPermi('warehouse:thirdParty:list')")
    @GetMapping("/options/sellers")
    public AjaxResult sellerOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(warehouseService.selectNormalSellerOptions(keyword));
    }
}
