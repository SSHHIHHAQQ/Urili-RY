package com.ruoyi.integration.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.integration.domain.SourceWarehouseStockItem;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.query.SourceWarehouseStockQuery;
import com.ruoyi.integration.domain.request.LogisticsChannelPairingRequest;
import com.ruoyi.integration.domain.request.SkuDimensionSelectedSyncRequest;
import com.ruoyi.integration.domain.request.SkuPairingRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionInfoRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionRequest;
import com.ruoyi.integration.domain.request.UpstreamCredentialRequest;
import com.ruoyi.integration.domain.request.UpstreamOrderRequest;
import com.ruoyi.integration.domain.request.UpstreamStatusRequest;
import com.ruoyi.integration.domain.request.UpstreamSyncRequest;
import com.ruoyi.integration.domain.request.WarehousePairingRequest;
import com.ruoyi.integration.service.IUpstreamSyncService;
import com.ruoyi.integration.service.IUpstreamSystemService;
import com.ruoyi.integration.support.UpstreamSystemConstants;

/**
 * 管理端上游系统管理。
 */
@RestController
@RequestMapping("/integration/admin/upstream-systems")
public class AdminUpstreamSystemController extends BaseController
{
    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @Autowired
    private IUpstreamSyncService upstreamSyncService;

    @PreAuthorize("@ss.hasPermi('integration:upstream:list')")
    @GetMapping("/list")
    public TableDataInfo list(UpstreamSystemConnection query)
    {
        startPage();
        List<UpstreamSystemConnection> list = upstreamSystemService.selectConnectionList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}")
    public AjaxResult get(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.selectConnectionByCode(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:add')")
    @Log(title = "上游系统接入", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody UpstreamConnectionRequest request)
    {
        return toAjax(upstreamSystemService.insertConnection(request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:edit')")
    @Log(title = "上游系统接入", businessType = BusinessType.UPDATE)
    @PutMapping("/{connectionCode}")
    public AjaxResult edit(@PathVariable("connectionCode") String connectionCode,
        @Validated @RequestBody UpstreamConnectionInfoRequest request)
    {
        return toAjax(upstreamSystemService.updateConnectionInfo(connectionCode, request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:credential')")
    @Log(title = "上游系统重新授权", businessType = BusinessType.UPDATE)
    @PutMapping("/{connectionCode}/credentials")
    public AjaxResult credentials(@PathVariable("connectionCode") String connectionCode,
        @Validated @RequestBody UpstreamCredentialRequest request)
    {
        return toAjax(upstreamSystemService.updateConnectionCredentials(connectionCode, request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:edit')")
    @Log(title = "上游系统启停", businessType = BusinessType.UPDATE)
    @PutMapping("/{connectionCode}/status")
    public AjaxResult status(@PathVariable("connectionCode") String connectionCode,
        @Validated @RequestBody UpstreamStatusRequest request)
    {
        return toAjax(upstreamSystemService.updateConnectionStatus(connectionCode, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:edit')")
    @Log(title = "上游系统排序", businessType = BusinessType.UPDATE)
    @PutMapping("/order")
    public AjaxResult order(@Validated @RequestBody UpstreamOrderRequest request)
    {
        return toAjax(upstreamSystemService.updateConnectionOrder(request.getConnectionCodes()));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:sync')")
    @Log(title = "上游系统授权校验", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/authorize")
    public AjaxResult authorize(@PathVariable("connectionCode") String connectionCode)
    {
        return toAjax(upstreamSystemService.authorize(connectionCode));
    }

    @PreAuthorize("@ss.hasAnyPermi('integration:upstream:sync,integration:upstream:dimensionSync,integration:upstream:inventorySync')")
    @Log(title = "上游系统同步", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/sync")
    public AjaxResult sync(@PathVariable("connectionCode") String connectionCode,
        @RequestBody(required = false) UpstreamSyncRequest request)
    {
        checkSyncPermissions(request);
        return success(upstreamSyncService.syncSelected(connectionCode, request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:sync')")
    @Log(title = "上游SKU同步", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/skus/sync")
    public AjaxResult syncSkus(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSyncService.syncSkusOnly(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:dimensionSync')")
    @Log(title = "上游SKU仓库尺寸重量同步", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/sku-dimensions/sync")
    public AjaxResult syncSkuDimensions(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSyncService.syncSkuDimensionsOnly(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:dimensionSync')")
    @Log(title = "指定SKU仓库尺寸重量同步", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/sku-dimensions/sync-selected")
    public AjaxResult syncSelectedSkuDimensions(@PathVariable("connectionCode") String connectionCode,
        @RequestBody SkuDimensionSelectedSyncRequest request)
    {
        return success(upstreamSyncService.syncSkuDimensionsBySkuList(connectionCode, request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:inventorySync')")
    @Log(title = "上游SKU库存同步", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/inventory/sync")
    public AjaxResult syncInventory(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSyncService.syncWarehouseStocksOnly(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:inventoryQuery')")
    @GetMapping("/{connectionCode}/inventory/list")
    public TableDataInfo inventory(@PathVariable("connectionCode") String connectionCode,
        SourceWarehouseStockQuery query)
    {
        startPage();
        query.setConnectionCode(connectionCode);
        List<SourceWarehouseStockItem> list = upstreamSystemService.selectSourceWarehouseStockList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:inventoryQuery')")
    @GetMapping("/{connectionCode}/inventory-sync-state")
    public AjaxResult inventorySyncState(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.selectInventorySyncState(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/sync-states")
    public AjaxResult syncStates(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.selectSyncStateList(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/warehouses")
    public AjaxResult warehouses(@PathVariable("connectionCode") String connectionCode,
        @RequestParam(value = "status", required = false) String status)
    {
        return success(upstreamSystemService.selectWarehouseSyncList(connectionCode, status));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/warehouse-pairings")
    public AjaxResult warehousePairings(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.selectWarehousePairingList(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:pair')")
    @Log(title = "仓库配对", businessType = BusinessType.INSERT)
    @PostMapping("/{connectionCode}/warehouse-pairings")
    public AjaxResult addWarehousePairing(@PathVariable("connectionCode") String connectionCode,
        @Validated @RequestBody WarehousePairingRequest request)
    {
        return toAjax(upstreamSystemService.insertWarehousePairing(connectionCode, request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:pair')")
    @Log(title = "仓库解除配对", businessType = BusinessType.DELETE)
    @DeleteMapping("/{connectionCode}/warehouse-pairings/{warehousePairingId}")
    public AjaxResult deleteWarehousePairing(@PathVariable("connectionCode") String connectionCode,
        @PathVariable("warehousePairingId") Long warehousePairingId)
    {
        return toAjax(upstreamSystemService.deleteWarehousePairing(connectionCode, warehousePairingId));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/logistics-channels")
    public AjaxResult logisticsChannels(@PathVariable("connectionCode") String connectionCode,
        @RequestParam(value = "status", required = false) String status)
    {
        return success(upstreamSystemService.selectLogisticsChannelSyncList(connectionCode, status));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/logistics-channel-pairings")
    public AjaxResult logisticsChannelPairings(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.selectLogisticsChannelPairingList(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:pair')")
    @Log(title = "物流渠道配对", businessType = BusinessType.INSERT)
    @PostMapping("/{connectionCode}/logistics-channel-pairings")
    public AjaxResult addLogisticsChannelPairing(@PathVariable("connectionCode") String connectionCode,
        @Validated @RequestBody LogisticsChannelPairingRequest request)
    {
        return toAjax(upstreamSystemService.insertLogisticsChannelPairing(connectionCode, request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:pair')")
    @Log(title = "物流渠道解除配对", businessType = BusinessType.DELETE)
    @DeleteMapping("/{connectionCode}/logistics-channel-pairings/{logisticsChannelPairingId}")
    public AjaxResult deleteLogisticsChannelPairing(@PathVariable("connectionCode") String connectionCode,
        @PathVariable("logisticsChannelPairingId") Long logisticsChannelPairingId)
    {
        return toAjax(upstreamSystemService.deleteLogisticsChannelPairing(connectionCode, logisticsChannelPairingId));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/skus/list")
    public TableDataInfo skus(@PathVariable("connectionCode") String connectionCode,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "pairingStatus", required = false) String pairingStatus,
        @RequestParam(value = "dimensionStatus", required = false) String dimensionStatus,
        @RequestParam(value = "field", required = false) String field,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        startPage();
        List<UpstreamSkuSyncItem> list = upstreamSystemService.selectSkuSyncList(connectionCode, status,
            pairingStatus, dimensionStatus, field, keyword);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/sku-pairings")
    public AjaxResult skuPairings(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.selectSkuPairingList(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:pair')")
    @Log(title = "SKU配对", businessType = BusinessType.INSERT)
    @PostMapping("/{connectionCode}/sku-pairings")
    public AjaxResult addSkuPairing(@PathVariable("connectionCode") String connectionCode,
        @Validated @RequestBody SkuPairingRequest request)
    {
        return toAjax(upstreamSystemService.insertSkuPairing(connectionCode, request));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:pair')")
    @Log(title = "SKU解除配对", businessType = BusinessType.DELETE)
    @DeleteMapping("/{connectionCode}/sku-pairings/{skuPairingId}")
    public AjaxResult deleteSkuPairing(@PathVariable("connectionCode") String connectionCode,
        @PathVariable("skuPairingId") Long skuPairingId)
    {
        return toAjax(upstreamSystemService.deleteSkuPairing(connectionCode, skuPairingId));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/sku-sync-state")
    public AjaxResult skuSyncState(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.selectSkuSyncState(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:log')")
    @GetMapping("/{connectionCode}/request-logs/list")
    public TableDataInfo requestLogs(@PathVariable("connectionCode") String connectionCode)
    {
        startPage();
        return getDataTable(upstreamSystemService.selectRequestLogList(connectionCode));
    }

    private void checkSyncPermissions(UpstreamSyncRequest request)
    {
        List<String> syncTypes = request == null || request.getSyncTypes() == null || request.getSyncTypes().isEmpty()
            ? Arrays.asList(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
                UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL,
                UpstreamSystemConstants.SYNC_TYPE_SKU)
            : request.getSyncTypes();
        for (String syncType : syncTypes)
        {
            String normalized = syncType == null ? "" : syncType.trim().toUpperCase(Locale.ROOT);
            if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(normalized))
            {
                requireSyncPermission("integration:upstream:dimensionSync", "缺少SKU仓库尺寸重量同步权限");
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(normalized))
            {
                requireSyncPermission("integration:upstream:inventorySync", "缺少SKU库存同步权限");
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(normalized)
                || UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(normalized)
                || UpstreamSystemConstants.SYNC_TYPE_SKU.equals(normalized))
            {
                requireSyncPermission("integration:upstream:sync", "缺少上游系统同步权限");
            }
        }
    }

    private void requireSyncPermission(String permission, String message)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Set<String> permissions = loginUser == null ? null : loginUser.getPermissions();
        if (permissions == null || (!permissions.contains(Constants.ALL_PERMISSION) && !permissions.contains(permission)))
        {
            throw new ServiceException(message);
        }
    }
}
