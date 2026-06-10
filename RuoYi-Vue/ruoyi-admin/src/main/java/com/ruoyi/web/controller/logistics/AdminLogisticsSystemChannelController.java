package com.ruoyi.web.controller.logistics;

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
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.request.LogisticsStatusRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelCarrierMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelOrderSettingRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelWarehouseRequest;
import com.ruoyi.logistics.service.ILogisticsSystemChannelService;

/**
 * 管理端系统物流渠道管理。
 */
@RestController
@RequestMapping("/logistics/admin/system-channels")
public class AdminLogisticsSystemChannelController extends BaseController
{
    @Autowired
    private ILogisticsSystemChannelService systemChannelService;

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:list')")
    @GetMapping({"", "/list"})
    public TableDataInfo list(LogisticsSystemChannel query)
    {
        startPage();
        return getDataTable(systemChannelService.selectSystemChannelList(query));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:query')")
    @GetMapping("/{systemChannelCode}")
    public AjaxResult get(@PathVariable("systemChannelCode") String systemChannelCode)
    {
        return success(systemChannelService.selectSystemChannelByCode(systemChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:add')")
    @Log(title = "系统物流渠道", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody LogisticsSystemChannelRequest request)
    {
        return toAjax(systemChannelService.insertSystemChannel(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:edit')")
    @Log(title = "系统物流渠道", businessType = BusinessType.UPDATE)
    @PutMapping("/{systemChannelCode}")
    public AjaxResult edit(@PathVariable("systemChannelCode") String systemChannelCode,
        @Validated @RequestBody LogisticsSystemChannelRequest request)
    {
        return toAjax(systemChannelService.updateSystemChannel(systemChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:status')")
    @Log(title = "系统物流渠道启停", businessType = BusinessType.UPDATE)
    @PutMapping("/{systemChannelCode}/status")
    public AjaxResult status(@PathVariable("systemChannelCode") String systemChannelCode,
        @Validated @RequestBody LogisticsStatusRequest request)
    {
        return toAjax(systemChannelService.updateSystemChannelStatus(systemChannelCode, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:query')")
    @GetMapping("/{systemChannelCode}/carrier-mappings/list")
    public AjaxResult carrierMappings(@PathVariable("systemChannelCode") String systemChannelCode)
    {
        return success(systemChannelService.selectCarrierMappingList(systemChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:binding')")
    @Log(title = "系统渠道物流商映射", businessType = BusinessType.INSERT)
    @PostMapping("/{systemChannelCode}/carrier-mappings")
    public AjaxResult addCarrierMapping(@PathVariable("systemChannelCode") String systemChannelCode,
        @Validated @RequestBody LogisticsSystemChannelCarrierMappingRequest request)
    {
        return toAjax(systemChannelService.insertCarrierMapping(systemChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:binding')")
    @Log(title = "系统渠道物流商映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{systemChannelCode}/carrier-mappings/{mappingId}")
    public AjaxResult deleteCarrierMapping(@PathVariable("systemChannelCode") String systemChannelCode,
        @PathVariable("mappingId") Long mappingId)
    {
        return toAjax(systemChannelService.deleteCarrierMapping(systemChannelCode, mappingId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:query')")
    @GetMapping("/{systemChannelCode}/warehouses/list")
    public AjaxResult warehouses(@PathVariable("systemChannelCode") String systemChannelCode)
    {
        return success(systemChannelService.selectWarehouseBindingList(systemChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:binding')")
    @Log(title = "系统渠道仓库发货地址", businessType = BusinessType.INSERT)
    @PostMapping("/{systemChannelCode}/warehouses")
    public AjaxResult addWarehouse(@PathVariable("systemChannelCode") String systemChannelCode,
        @Validated @RequestBody LogisticsSystemChannelWarehouseRequest request)
    {
        return toAjax(systemChannelService.insertWarehouseBinding(systemChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:binding')")
    @Log(title = "系统渠道仓库发货地址", businessType = BusinessType.UPDATE)
    @PutMapping("/{systemChannelCode}/warehouses/{bindingId}")
    public AjaxResult editWarehouse(@PathVariable("systemChannelCode") String systemChannelCode,
        @PathVariable("bindingId") Long bindingId,
        @Validated @RequestBody LogisticsSystemChannelWarehouseRequest request)
    {
        return toAjax(systemChannelService.updateWarehouseBinding(systemChannelCode, bindingId, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:binding')")
    @Log(title = "系统渠道仓库发货地址", businessType = BusinessType.DELETE)
    @DeleteMapping("/{systemChannelCode}/warehouses/{bindingId}")
    public AjaxResult deleteWarehouse(@PathVariable("systemChannelCode") String systemChannelCode,
        @PathVariable("bindingId") Long bindingId)
    {
        return toAjax(systemChannelService.deleteWarehouseBinding(systemChannelCode, bindingId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:query')")
    @GetMapping("/{systemChannelCode}/order-setting")
    public AjaxResult orderSetting(@PathVariable("systemChannelCode") String systemChannelCode)
    {
        return success(systemChannelService.selectOrderSetting(systemChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:rule')")
    @Log(title = "系统渠道下单规则", businessType = BusinessType.UPDATE)
    @PutMapping("/{systemChannelCode}/order-setting")
    public AjaxResult saveOrderSetting(@PathVariable("systemChannelCode") String systemChannelCode,
        @Validated @RequestBody LogisticsSystemChannelOrderSettingRequest request)
    {
        return toAjax(systemChannelService.saveOrderSetting(systemChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:query')")
    @GetMapping("/options/carrier-accounts")
    public AjaxResult carrierAccountOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(systemChannelService.selectCarrierAccountOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:query')")
    @GetMapping("/options/carrier-channels")
    public AjaxResult carrierChannelOptions(@RequestParam("carrierAccountId") Long carrierAccountId,
        @RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(systemChannelService.selectCarrierChannelOptions(carrierAccountId, keyword));
    }

    @PreAuthorize("@ss.hasPermi('logistics:systemChannel:query')")
    @GetMapping("/options/warehouses")
    public AjaxResult warehouseOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(systemChannelService.selectWarehouseOptions(keyword));
    }

}
