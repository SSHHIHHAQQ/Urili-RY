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
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.finance.domain.FinanceCurrency;
import com.ruoyi.finance.domain.FinanceCurrencyRateHistory;
import com.ruoyi.finance.domain.FinanceCurrencySyncConfig;
import com.ruoyi.finance.domain.FinanceCurrencySyncLog;
import com.ruoyi.finance.domain.request.CurrencyStatusRequest;
import com.ruoyi.finance.service.IFinanceCurrencyService;

/**
 * 管理端币种配置。
 */
@RestController
@RequestMapping("/finance/admin")
public class AdminCurrencyController extends BaseController
{
    @Autowired
    private IFinanceCurrencyService financeCurrencyService;

    @PreAuthorize("@ss.hasPermi('finance:currency:list')")
    @GetMapping("/currencies/list")
    public TableDataInfo list(FinanceCurrency query)
    {
        startPage();
        List<FinanceCurrency> list = financeCurrencyService.selectCurrencyList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:list')")
    @GetMapping("/currencies/options")
    public AjaxResult options()
    {
        return success(financeCurrencyService.selectEnabledCurrencyOptions());
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:query')")
    @GetMapping("/currencies/{currencyCode}")
    public AjaxResult get(@PathVariable("currencyCode") String currencyCode)
    {
        return success(financeCurrencyService.selectCurrencyByCode(currencyCode));
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:add')")
    @Log(title = "币种配置", businessType = BusinessType.INSERT)
    @PostMapping("/currencies")
    public AjaxResult add(@Validated @RequestBody FinanceCurrency currency)
    {
        return toAjax(financeCurrencyService.insertCurrency(currency));
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:edit')")
    @Log(title = "币种配置", businessType = BusinessType.UPDATE)
    @PutMapping("/currencies/{currencyCode}")
    public AjaxResult edit(@PathVariable("currencyCode") String currencyCode,
        @Validated @RequestBody FinanceCurrency currency)
    {
        return toAjax(financeCurrencyService.updateCurrency(currencyCode, currency));
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:edit')")
    @Log(title = "币种启停", businessType = BusinessType.UPDATE)
    @PutMapping("/currencies/{currencyCode}/status")
    public AjaxResult status(@PathVariable("currencyCode") String currencyCode,
        @RequestBody CurrencyStatusRequest request)
    {
        return toAjax(financeCurrencyService.updateCurrencyStatus(currencyCode, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:remove')")
    @Log(title = "币种配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/currencies/{currencyCode}")
    public AjaxResult remove(@PathVariable("currencyCode") String currencyCode)
    {
        return toAjax(financeCurrencyService.deleteCurrencyByCode(currencyCode));
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:query')")
    @GetMapping("/currencies/{currencyCode}/rate-history/list")
    public TableDataInfo history(@PathVariable("currencyCode") String currencyCode)
    {
        startPage();
        List<FinanceCurrencyRateHistory> list = financeCurrencyService.selectRateHistoryList(currencyCode);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:syncConfig')")
    @GetMapping("/currency-sync-config")
    public AjaxResult syncConfig()
    {
        return success(financeCurrencyService.selectSyncConfig());
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:syncConfig')")
    @Log(title = "币种汇率同步设置", businessType = BusinessType.UPDATE,
            excludeParamNames = { "credential", "credentialCiphertext" })
    @PutMapping("/currency-sync-config")
    public AjaxResult saveSyncConfig(@Validated @RequestBody FinanceCurrencySyncConfig config)
    {
        return toAjax(financeCurrencyService.saveSyncConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:sync')")
    @Log(title = "币种汇率测试连接", businessType = BusinessType.OTHER,
            excludeParamNames = { "credential", "credentialCiphertext" })
    @PostMapping("/currency-sync-config/test")
    public AjaxResult testSyncConfig(@RequestBody(required = false) FinanceCurrencySyncConfig config)
    {
        return success(financeCurrencyService.testSyncConfig(config));
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:sync')")
    @Log(title = "币种汇率同步", businessType = BusinessType.OTHER)
    @PostMapping("/currency-sync-config/sync")
    public AjaxResult syncRates()
    {
        return success(financeCurrencyService.syncRates());
    }

    @PreAuthorize("@ss.hasPermi('finance:currency:log')")
    @GetMapping("/currency-sync-config/logs/list")
    public TableDataInfo syncLogs(FinanceCurrencySyncLog query)
    {
        startPage();
        List<FinanceCurrencySyncLog> list = financeCurrencyService.selectSyncLogList(query);
        return getDataTable(list);
    }
}
