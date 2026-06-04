package com.ruoyi.integration.controller;

import java.util.List;
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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.request.LogisticsChannelPairingRequest;
import com.ruoyi.integration.domain.request.SkuPairingRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionInfoRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionRequest;
import com.ruoyi.integration.domain.request.UpstreamCredentialRequest;
import com.ruoyi.integration.domain.request.UpstreamStatusRequest;
import com.ruoyi.integration.domain.request.WarehousePairingRequest;
import com.ruoyi.integration.service.IUpstreamSystemService;

/**
 * 管理端上游系统管理。
 */
@RestController
@RequestMapping("/integration/admin/upstream-systems")
public class AdminUpstreamSystemController extends BaseController
{
    @Autowired
    private IUpstreamSystemService upstreamSystemService;

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

    @PreAuthorize("@ss.hasPermi('integration:upstream:sync')")
    @Log(title = "上游系统授权校验", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/authorize")
    public AjaxResult authorize(@PathVariable("connectionCode") String connectionCode)
    {
        return toAjax(upstreamSystemService.authorize(connectionCode));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:sync')")
    @Log(title = "上游系统同步", businessType = BusinessType.OTHER)
    @PostMapping("/{connectionCode}/sync")
    public AjaxResult sync(@PathVariable("connectionCode") String connectionCode)
    {
        return success(upstreamSystemService.syncAll(connectionCode));
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
    @DeleteMapping("/warehouse-pairings/{warehousePairingId}")
    public AjaxResult deleteWarehousePairing(@PathVariable("warehousePairingId") Long warehousePairingId)
    {
        return toAjax(upstreamSystemService.deleteWarehousePairing(warehousePairingId));
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
    @DeleteMapping("/logistics-channel-pairings/{logisticsChannelPairingId}")
    public AjaxResult deleteLogisticsChannelPairing(@PathVariable("logisticsChannelPairingId") Long logisticsChannelPairingId)
    {
        return toAjax(upstreamSystemService.deleteLogisticsChannelPairing(logisticsChannelPairingId));
    }

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/{connectionCode}/skus/list")
    public TableDataInfo skus(@PathVariable("connectionCode") String connectionCode,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        startPage();
        List<UpstreamSkuSyncItem> list = upstreamSystemService.selectSkuSyncList(connectionCode, status, keyword);
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
    @DeleteMapping("/sku-pairings/{skuPairingId}")
    public AjaxResult deleteSkuPairing(@PathVariable("skuPairingId") Long skuPairingId)
    {
        return toAjax(upstreamSystemService.deleteSkuPairing(skuPairingId));
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
}
