package com.ruoyi.finance.controller;

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
import com.ruoyi.finance.domain.QuoteScheme;
import com.ruoyi.finance.domain.QuoteSchemeChannel;
import com.ruoyi.finance.domain.QuoteSchemeValueFeeRule;
import com.ruoyi.finance.domain.request.QuoteSchemeStatusRequest;
import com.ruoyi.finance.domain.request.QuoteSchemeWarehouseRequest;
import com.ruoyi.finance.service.IQuoteSchemeService;

@RestController
@RequestMapping("/finance/admin/quote-schemes")
public class AdminQuoteSchemeController extends BaseController
{
    @Autowired
    private IQuoteSchemeService quoteSchemeService;

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:list')")
    @GetMapping("/list")
    public TableDataInfo list(QuoteScheme query)
    {
        startPage();
        List<QuoteScheme> list = quoteSchemeService.selectQuoteSchemeList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:query')")
    @GetMapping("/{schemeId}")
    public AjaxResult get(@PathVariable("schemeId") Long schemeId)
    {
        return success(quoteSchemeService.selectQuoteSchemeById(schemeId));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:add')")
    @Log(title = "报价方案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody QuoteScheme scheme)
    {
        int rows = quoteSchemeService.insertQuoteScheme(scheme);
        return rows > 0 ? success(quoteSchemeService.selectQuoteSchemeById(scheme.getSchemeId())) : error();
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:edit')")
    @Log(title = "报价方案", businessType = BusinessType.UPDATE)
    @PutMapping("/{schemeId}")
    public AjaxResult edit(@PathVariable("schemeId") Long schemeId,
        @Validated @RequestBody QuoteScheme scheme)
    {
        return toAjax(quoteSchemeService.updateQuoteScheme(schemeId, scheme));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:status')")
    @Log(title = "报价方案启停", businessType = BusinessType.UPDATE)
    @PutMapping("/{schemeId}/status")
    public AjaxResult status(@PathVariable("schemeId") Long schemeId,
        @RequestBody QuoteSchemeStatusRequest request)
    {
        return toAjax(quoteSchemeService.updateQuoteSchemeStatus(schemeId, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:warehouse')")
    @GetMapping("/{schemeId}/warehouses")
    public AjaxResult warehouses(@PathVariable("schemeId") Long schemeId)
    {
        return success(quoteSchemeService.selectQuoteSchemeWarehouseList(schemeId));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:warehouse')")
    @Log(title = "报价方案仓库范围", businessType = BusinessType.UPDATE)
    @PutMapping("/{schemeId}/warehouses")
    public AjaxResult saveWarehouses(@PathVariable("schemeId") Long schemeId,
        @RequestBody QuoteSchemeWarehouseRequest request)
    {
        return toAjax(quoteSchemeService.saveQuoteSchemeWarehouses(schemeId, request.getWarehouseCodes()));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:channel')")
    @GetMapping("/{schemeId}/channels/list")
    public AjaxResult channels(@PathVariable("schemeId") Long schemeId)
    {
        return success(quoteSchemeService.selectQuoteSchemeChannelList(schemeId));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:channel')")
    @Log(title = "报价方案客户渠道", businessType = BusinessType.INSERT)
    @PostMapping("/{schemeId}/channels")
    public AjaxResult addChannel(@PathVariable("schemeId") Long schemeId,
        @Validated @RequestBody QuoteSchemeChannel channel)
    {
        return toAjax(quoteSchemeService.insertQuoteSchemeChannel(schemeId, channel));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:channel')")
    @Log(title = "报价方案客户渠道", businessType = BusinessType.UPDATE)
    @PutMapping("/{schemeId}/channels/{schemeChannelId}")
    public AjaxResult editChannel(@PathVariable("schemeId") Long schemeId,
        @PathVariable("schemeChannelId") Long schemeChannelId,
        @Validated @RequestBody QuoteSchemeChannel channel)
    {
        return toAjax(quoteSchemeService.updateQuoteSchemeChannel(schemeId, schemeChannelId, channel));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:channel')")
    @Log(title = "报价方案客户渠道", businessType = BusinessType.DELETE)
    @DeleteMapping("/{schemeId}/channels/{schemeChannelId}")
    public AjaxResult removeChannel(@PathVariable("schemeId") Long schemeId,
        @PathVariable("schemeChannelId") Long schemeChannelId)
    {
        return toAjax(quoteSchemeService.deleteQuoteSchemeChannel(schemeId, schemeChannelId));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:valueFee')")
    @GetMapping("/{schemeId}/value-fees/list")
    public AjaxResult valueFees(@PathVariable("schemeId") Long schemeId)
    {
        return success(quoteSchemeService.selectQuoteSchemeValueFeeRuleList(schemeId));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:valueFee')")
    @Log(title = "报价方案增值费", businessType = BusinessType.INSERT)
    @PostMapping("/{schemeId}/value-fees")
    public AjaxResult addValueFee(@PathVariable("schemeId") Long schemeId,
        @Validated @RequestBody QuoteSchemeValueFeeRule rule)
    {
        return toAjax(quoteSchemeService.insertQuoteSchemeValueFeeRule(schemeId, rule));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:valueFee')")
    @Log(title = "报价方案增值费", businessType = BusinessType.UPDATE)
    @PutMapping("/{schemeId}/value-fees/{valueFeeRuleId}")
    public AjaxResult editValueFee(@PathVariable("schemeId") Long schemeId,
        @PathVariable("valueFeeRuleId") Long valueFeeRuleId,
        @Validated @RequestBody QuoteSchemeValueFeeRule rule)
    {
        return toAjax(quoteSchemeService.updateQuoteSchemeValueFeeRule(schemeId, valueFeeRuleId, rule));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:valueFee')")
    @Log(title = "报价方案增值费", businessType = BusinessType.DELETE)
    @DeleteMapping("/{schemeId}/value-fees/{valueFeeRuleId}")
    public AjaxResult removeValueFee(@PathVariable("schemeId") Long schemeId,
        @PathVariable("valueFeeRuleId") Long valueFeeRuleId)
    {
        return toAjax(quoteSchemeService.deleteQuoteSchemeValueFeeRule(schemeId, valueFeeRuleId));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:list')")
    @GetMapping("/options/buyers")
    public AjaxResult buyerOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(quoteSchemeService.selectBuyerOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:list')")
    @GetMapping("/options/warehouses")
    public AjaxResult warehouseOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(quoteSchemeService.selectWarehouseOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:list')")
    @GetMapping("/options/customer-channels")
    public AjaxResult customerChannelOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(quoteSchemeService.selectCustomerChannelOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:list')")
    @GetMapping("/options/system-channels")
    public AjaxResult systemChannelOptions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(quoteSchemeService.selectSystemChannelOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('finance:quoteScheme:list')")
    @GetMapping("/options/fee-placeholders")
    public AjaxResult feePlaceholderOptions(@RequestParam(value = "feeType", required = false) String feeType)
    {
        return success(quoteSchemeService.selectFeePlaceholderOptions(feeType));
    }
}
