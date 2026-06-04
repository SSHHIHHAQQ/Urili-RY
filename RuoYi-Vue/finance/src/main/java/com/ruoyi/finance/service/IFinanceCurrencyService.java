package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.FinanceCurrency;
import com.ruoyi.finance.domain.FinanceCurrencyOption;
import com.ruoyi.finance.domain.FinanceCurrencyRateHistory;
import com.ruoyi.finance.domain.FinanceCurrencySyncConfig;
import com.ruoyi.finance.domain.FinanceCurrencySyncLog;
import com.ruoyi.finance.domain.FinanceCurrencySyncResult;

/**
 * 财务币种配置服务。
 */
public interface IFinanceCurrencyService
{
    List<FinanceCurrency> selectCurrencyList(FinanceCurrency query);

    FinanceCurrency selectCurrencyByCode(String currencyCode);

    List<FinanceCurrencyOption> selectEnabledCurrencyOptions();

    int insertCurrency(FinanceCurrency currency);

    int updateCurrency(String currencyCode, FinanceCurrency currency);

    int updateCurrencyStatus(String currencyCode, String status);

    int deleteCurrencyByCode(String currencyCode);

    List<FinanceCurrencyRateHistory> selectRateHistoryList(String currencyCode);

    FinanceCurrencySyncConfig selectSyncConfig();

    int saveSyncConfig(FinanceCurrencySyncConfig config);

    FinanceCurrencySyncResult testSyncConfig(FinanceCurrencySyncConfig config);

    FinanceCurrencySyncResult syncRates();

    List<FinanceCurrencySyncLog> selectSyncLogList(FinanceCurrencySyncLog query);
}
