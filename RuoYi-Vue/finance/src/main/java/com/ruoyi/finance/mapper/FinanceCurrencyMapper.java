package com.ruoyi.finance.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.finance.domain.FinanceCurrency;
import com.ruoyi.finance.domain.FinanceCurrencyOption;
import com.ruoyi.finance.domain.FinanceCurrencyRateHistory;
import com.ruoyi.finance.domain.FinanceCurrencySyncConfig;
import com.ruoyi.finance.domain.FinanceCurrencySyncLog;

/**
 * 财务币种配置 Mapper。
 */
public interface FinanceCurrencyMapper
{
    List<FinanceCurrency> selectCurrencyList(FinanceCurrency query);

    List<FinanceCurrency> selectEnabledCurrencyList();

    List<FinanceCurrencyOption> selectEnabledCurrencyOptions();

    FinanceCurrency selectCurrencyByCode(String currencyCode);

    int insertCurrency(FinanceCurrency currency);

    int updateCurrency(FinanceCurrency currency);

    int updateCurrencyStatus(@Param("currencyCode") String currencyCode, @Param("status") String status,
        @Param("updateBy") String updateBy);

    int deleteCurrencyByCode(String currencyCode);

    int clearDefaultCurrency(@Param("excludeCurrencyCode") String excludeCurrencyCode, @Param("updateBy") String updateBy);

    int updateSyncedRate(@Param("currencyCode") String currencyCode, @Param("baseCurrencyCode") String baseCurrencyCode,
        @Param("officialRate") BigDecimal officialRate, @Param("effectiveRate") BigDecimal effectiveRate,
        @Param("officialRateTime") Date officialRateTime, @Param("effectiveRateTime") Date effectiveRateTime,
        @Param("updateBy") String updateBy);

    int insertRateHistory(FinanceCurrencyRateHistory history);

    List<FinanceCurrencyRateHistory> selectRateHistoryList(@Param("currencyCode") String currencyCode);

    int countRateHistoryByCurrencyCode(@Param("currencyCode") String currencyCode);

    FinanceCurrencySyncConfig selectFirstSyncConfig();

    FinanceCurrencySyncConfig selectSyncConfigByProviderCode(@Param("providerCode") String providerCode);

    int insertSyncConfig(FinanceCurrencySyncConfig config);

    int updateSyncConfig(FinanceCurrencySyncConfig config);

    int updateSyncConfigSummary(@Param("syncConfigId") Long syncConfigId, @Param("lastSyncStatus") String lastSyncStatus,
        @Param("updateBy") String updateBy);

    int insertSyncLog(FinanceCurrencySyncLog log);

    List<FinanceCurrencySyncLog> selectSyncLogList(FinanceCurrencySyncLog query);
}
