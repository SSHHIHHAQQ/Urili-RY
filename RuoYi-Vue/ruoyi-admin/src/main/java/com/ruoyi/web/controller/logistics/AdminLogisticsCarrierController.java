package com.ruoyi.web.controller.logistics;

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
import com.ruoyi.logistics.domain.LogisticsCarrierConnection;
import com.ruoyi.logistics.domain.LogisticsLabelOrder;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.request.Agg56CredentialRequest;
import com.ruoyi.logistics.domain.request.LogisticsChannelMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsConnectionRequest;
import com.ruoyi.logistics.domain.request.LogisticsCreateLabelRequest;
import com.ruoyi.logistics.domain.request.LogisticsLabelActionRequest;
import com.ruoyi.logistics.domain.request.LogisticsQuoteRequest;
import com.ruoyi.logistics.domain.request.LogisticsStatusRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelRequest;
import com.ruoyi.logistics.service.ILogisticsCarrierService;

/**
 * 管理端物流商管理。
 */
@RestController
@RequestMapping("/logistics/admin/carriers")
public class AdminLogisticsCarrierController extends BaseController
{
    @Autowired
    private ILogisticsCarrierService logisticsCarrierService;

    @PreAuthorize("@ss.hasPermi('logistics:carrier:list')")
    @GetMapping("/list")
    public TableDataInfo list(LogisticsCarrierConnection query)
    {
        startPage();
        return getDataTable(logisticsCarrierService.selectConnectionList(query));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:query')")
    @GetMapping("/{carrierAccountId}")
    public AjaxResult get(@PathVariable("carrierAccountId") Long carrierAccountId)
    {
        return success(logisticsCarrierService.selectConnectionByAccountId(carrierAccountId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:add')")
    @Log(title = "物流商账号", businessType = BusinessType.INSERT,
            excludeParamNames = { "appToken", "appKey", "appTokenCiphertext", "appKeyCiphertext" })
    @PostMapping
    public AjaxResult add(@Validated @RequestBody LogisticsConnectionRequest request)
    {
        return toAjax(logisticsCarrierService.insertConnection(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:edit')")
    @Log(title = "物流商账号", businessType = BusinessType.UPDATE)
    @PutMapping("/{carrierAccountId}")
    public AjaxResult edit(@PathVariable("carrierAccountId") Long carrierAccountId,
        @Validated @RequestBody LogisticsConnectionRequest request)
    {
        return toAjax(logisticsCarrierService.updateConnectionInfo(carrierAccountId, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:credential')")
    @Log(title = "AGG56物流商授权", businessType = BusinessType.UPDATE,
            excludeParamNames = { "appToken", "appKey", "appTokenCiphertext", "appKeyCiphertext" })
    @PutMapping("/{carrierAccountId}/agg56-credentials")
    public AjaxResult agg56Credentials(@PathVariable("carrierAccountId") Long carrierAccountId,
        @Validated @RequestBody Agg56CredentialRequest request)
    {
        return toAjax(logisticsCarrierService.updateAgg56Credentials(carrierAccountId, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:edit')")
    @Log(title = "物流商启停", businessType = BusinessType.UPDATE)
    @PutMapping("/{carrierAccountId}/status")
    public AjaxResult status(@PathVariable("carrierAccountId") Long carrierAccountId,
        @Validated @RequestBody LogisticsStatusRequest request)
    {
        return toAjax(logisticsCarrierService.updateConnectionStatus(carrierAccountId, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:edit')")
    @Log(title = "物流商排序", businessType = BusinessType.UPDATE)
    @PutMapping("/order")
    public AjaxResult order(@RequestBody List<Long> carrierAccountIds)
    {
        return toAjax(logisticsCarrierService.updateConnectionOrder(carrierAccountIds));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:sync')")
    @Log(title = "物流商授权校验", businessType = BusinessType.OTHER)
    @PostMapping("/{carrierAccountId}/authorize")
    public AjaxResult authorize(@PathVariable("carrierAccountId") Long carrierAccountId)
    {
        return toAjax(logisticsCarrierService.authorize(carrierAccountId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:sync')")
    @Log(title = "物流商渠道同步", businessType = BusinessType.OTHER)
    @PostMapping("/{carrierAccountId}/channels/sync")
    public AjaxResult syncChannels(@PathVariable("carrierAccountId") Long carrierAccountId)
    {
        return toAjax(logisticsCarrierService.syncChannels(carrierAccountId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:query')")
    @GetMapping("/{carrierAccountId}/channels/list")
    public AjaxResult channels(@PathVariable("carrierAccountId") Long carrierAccountId,
        @RequestParam(value = "status", required = false) String status)
    {
        return success(logisticsCarrierService.selectChannelCandidateList(carrierAccountId, status));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:channel')")
    @GetMapping("/system-channels/list")
    public TableDataInfo systemChannels(LogisticsSystemChannel query)
    {
        startPage();
        return getDataTable(logisticsCarrierService.selectSystemChannelList(query));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:channel')")
    @Log(title = "系统物流渠道", businessType = BusinessType.INSERT)
    @PostMapping("/system-channels")
    public AjaxResult addSystemChannel(@Validated @RequestBody LogisticsSystemChannelRequest request)
    {
        return toAjax(logisticsCarrierService.insertSystemChannel(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:channel')")
    @Log(title = "系统物流渠道", businessType = BusinessType.UPDATE)
    @PutMapping("/system-channels/{systemChannelCode}")
    public AjaxResult editSystemChannel(@PathVariable("systemChannelCode") String systemChannelCode,
        @RequestParam(value = "status", required = false) String status,
        @Validated @RequestBody LogisticsSystemChannelRequest request)
    {
        return toAjax(logisticsCarrierService.updateSystemChannel(systemChannelCode, request, status));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:channel')")
    @GetMapping("/{carrierAccountId}/channel-mappings/list")
    public AjaxResult channelMappings(@PathVariable("carrierAccountId") Long carrierAccountId)
    {
        return success(logisticsCarrierService.selectChannelMappingList(carrierAccountId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:channel')")
    @Log(title = "物流商渠道映射", businessType = BusinessType.INSERT)
    @PostMapping("/{carrierAccountId}/channel-mappings")
    public AjaxResult addChannelMapping(@PathVariable("carrierAccountId") Long carrierAccountId,
        @Validated @RequestBody LogisticsChannelMappingRequest request)
    {
        return toAjax(logisticsCarrierService.insertChannelMapping(carrierAccountId, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:channel')")
    @Log(title = "物流商渠道映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{carrierAccountId}/channel-mappings/{mappingId}")
    public AjaxResult deleteChannelMapping(@PathVariable("carrierAccountId") Long carrierAccountId,
        @PathVariable("mappingId") Long mappingId)
    {
        return toAjax(logisticsCarrierService.deleteChannelMapping(carrierAccountId, mappingId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:label')")
    @Log(title = "物流商报价", businessType = BusinessType.OTHER,
            excludeParamNames = { "recipientAddress", "shipperAddress", "boxes" })
    @PostMapping("/quote")
    public AjaxResult quote(@Validated @RequestBody LogisticsQuoteRequest request)
    {
        return success(logisticsCarrierService.quote(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:label')")
    @Log(title = "物流商创建面单", businessType = BusinessType.INSERT,
            excludeParamNames = { "recipientAddress", "shipperAddress", "boxes" })
    @PostMapping("/labels")
    public AjaxResult createLabel(@Validated @RequestBody LogisticsCreateLabelRequest request)
    {
        return success(logisticsCarrierService.createLabel(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:label')")
    @Log(title = "物流商获取面单", businessType = BusinessType.OTHER)
    @PostMapping("/labels/fetch")
    public AjaxResult getLabel(@Validated @RequestBody LogisticsLabelActionRequest request)
    {
        return success(logisticsCarrierService.getLabel(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:label')")
    @Log(title = "物流商取消面单", businessType = BusinessType.UPDATE)
    @PostMapping("/labels/cancel")
    public AjaxResult cancelLabel(@Validated @RequestBody LogisticsLabelActionRequest request)
    {
        return success(logisticsCarrierService.cancelLabel(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:query')")
    @GetMapping("/labels/list")
    public TableDataInfo labels(LogisticsLabelOrder query)
    {
        startPage();
        return getDataTable(logisticsCarrierService.selectLabelOrderList(query));
    }

    @PreAuthorize("@ss.hasPermi('logistics:carrier:log')")
    @GetMapping("/{carrierAccountId}/request-logs/list")
    public TableDataInfo requestLogs(@PathVariable("carrierAccountId") Long carrierAccountId)
    {
        startPage();
        return getDataTable(logisticsCarrierService.selectRequestLogList(carrierAccountId));
    }
}
