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
import com.ruoyi.logistics.domain.LogisticsCustomerChannel;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelBuyerScopeRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelQuoteMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelSystemMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsStatusRequest;
import com.ruoyi.logistics.service.ILogisticsCustomerChannelService;

/**
 * 管理端客户渠道管理。
 */
@RestController
@RequestMapping("/logistics/admin/customer-channels")
public class AdminLogisticsCustomerChannelController extends BaseController
{
    @Autowired
    private ILogisticsCustomerChannelService customerChannelService;

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:list')")
    @GetMapping({"", "/list"})
    public TableDataInfo list(LogisticsCustomerChannel query)
    {
        startPage();
        return getDataTable(customerChannelService.selectCustomerChannelList(query));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:query')")
    @GetMapping("/{customerChannelCode}")
    public AjaxResult get(@PathVariable("customerChannelCode") String customerChannelCode)
    {
        return success(customerChannelService.selectCustomerChannelByCode(customerChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:add')")
    @Log(title = "客户渠道", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody LogisticsCustomerChannelRequest request)
    {
        return toAjax(customerChannelService.insertCustomerChannel(request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:edit')")
    @Log(title = "客户渠道", businessType = BusinessType.UPDATE)
    @PutMapping("/{customerChannelCode}")
    public AjaxResult edit(@PathVariable("customerChannelCode") String customerChannelCode,
        @Validated @RequestBody LogisticsCustomerChannelRequest request)
    {
        return toAjax(customerChannelService.updateCustomerChannel(customerChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:status')")
    @Log(title = "客户渠道启停", businessType = BusinessType.UPDATE)
    @PutMapping("/{customerChannelCode}/status")
    public AjaxResult status(@PathVariable("customerChannelCode") String customerChannelCode,
        @Validated @RequestBody LogisticsStatusRequest request)
    {
        return toAjax(customerChannelService.updateCustomerChannelStatus(customerChannelCode, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:query')")
    @GetMapping("/{customerChannelCode}/system-mappings/list")
    public AjaxResult systemMappings(@PathVariable("customerChannelCode") String customerChannelCode)
    {
        return success(customerChannelService.selectSystemMappingList(customerChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:binding')")
    @Log(title = "客户渠道系统渠道绑定", businessType = BusinessType.INSERT)
    @PostMapping("/{customerChannelCode}/system-mappings")
    public AjaxResult addSystemMapping(@PathVariable("customerChannelCode") String customerChannelCode,
        @Validated @RequestBody LogisticsCustomerChannelSystemMappingRequest request)
    {
        return toAjax(customerChannelService.insertSystemMapping(customerChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:binding')")
    @Log(title = "客户渠道系统渠道绑定", businessType = BusinessType.UPDATE)
    @PutMapping("/{customerChannelCode}/system-mappings/{mappingId}")
    public AjaxResult editSystemMapping(@PathVariable("customerChannelCode") String customerChannelCode,
        @PathVariable("mappingId") Long mappingId,
        @Validated @RequestBody LogisticsCustomerChannelSystemMappingRequest request)
    {
        return toAjax(customerChannelService.updateSystemMapping(customerChannelCode, mappingId, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:binding')")
    @Log(title = "客户渠道系统渠道绑定", businessType = BusinessType.DELETE)
    @DeleteMapping("/{customerChannelCode}/system-mappings/{mappingId}")
    public AjaxResult deleteSystemMapping(@PathVariable("customerChannelCode") String customerChannelCode,
        @PathVariable("mappingId") Long mappingId)
    {
        return toAjax(customerChannelService.deleteSystemMapping(customerChannelCode, mappingId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:query')")
    @GetMapping("/{customerChannelCode}/quote-channel-mappings/list")
    public AjaxResult quoteChannelMappings(@PathVariable("customerChannelCode") String customerChannelCode)
    {
        return success(customerChannelService.selectQuoteMappingList(customerChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:binding')")
    @Log(title = "客户渠道报价主仓渠道映射", businessType = BusinessType.INSERT)
    @PostMapping("/{customerChannelCode}/quote-channel-mappings")
    public AjaxResult addQuoteChannelMapping(@PathVariable("customerChannelCode") String customerChannelCode,
        @Validated @RequestBody LogisticsCustomerChannelQuoteMappingRequest request)
    {
        return toAjax(customerChannelService.insertQuoteMapping(customerChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:binding')")
    @Log(title = "客户渠道报价主仓渠道映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{customerChannelCode}/quote-channel-mappings/{mappingId}")
    public AjaxResult deleteQuoteChannelMapping(@PathVariable("customerChannelCode") String customerChannelCode,
        @PathVariable("mappingId") Long mappingId)
    {
        return toAjax(customerChannelService.deleteQuoteMapping(customerChannelCode, mappingId));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:query')")
    @GetMapping("/{customerChannelCode}/buyer-scope")
    public AjaxResult buyerScope(@PathVariable("customerChannelCode") String customerChannelCode)
    {
        return success(customerChannelService.selectBuyerScopeList(customerChannelCode));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:buyer')")
    @Log(title = "客户渠道买家范围", businessType = BusinessType.UPDATE)
    @PutMapping("/{customerChannelCode}/buyer-scope")
    public AjaxResult saveBuyerScope(@PathVariable("customerChannelCode") String customerChannelCode,
        @Validated @RequestBody LogisticsCustomerChannelBuyerScopeRequest request)
    {
        return toAjax(customerChannelService.saveBuyerScope(customerChannelCode, request));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:query')")
    @GetMapping("/options/system-channels")
    public AjaxResult systemChannelOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(customerChannelService.selectSystemChannelOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('logistics:customerChannel:query')")
    @GetMapping("/options/buyers")
    public AjaxResult buyerOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(customerChannelService.selectBuyerOptions(keyword));
    }
}
