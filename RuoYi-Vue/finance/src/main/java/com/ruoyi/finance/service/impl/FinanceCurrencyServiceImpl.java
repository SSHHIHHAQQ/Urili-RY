package com.ruoyi.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.finance.domain.FinanceCurrency;
import com.ruoyi.finance.domain.FinanceCurrencyOption;
import com.ruoyi.finance.domain.FinanceCurrencyRateHistory;
import com.ruoyi.finance.domain.FinanceCurrencySyncConfig;
import com.ruoyi.finance.domain.FinanceCurrencySyncLog;
import com.ruoyi.finance.domain.FinanceCurrencySyncResult;
import com.ruoyi.finance.mapper.FinanceCurrencyMapper;
import com.ruoyi.finance.service.IFinanceCurrencyService;
import com.ruoyi.finance.support.CurrencyRateCandidate;
import com.ruoyi.finance.support.CurrencyRateSyncClient;
import com.ruoyi.finance.support.CurrencyRateSyncResponse;
import com.ruoyi.system.service.ISysDictDataService;
import com.ruoyi.system.service.support.SecretCipherSupport;

/**
 * 财务币种配置服务实现。
 */
@Service
public class FinanceCurrencyServiceImpl implements IFinanceCurrencyService
{
    private static final String DICT_CURRENCY_CODE = "currency_code";

    private static final String STATUS_NORMAL = "0";

    private static final String STATUS_DISABLED = "1";

    private static final String YES = "Y";

    private static final String NO = "N";

    private static final String SOURCE_MANUAL = "MANUAL";

    private static final String SOURCE_SYNC = "SYNC";

    private static final String SYNC_STATUS_SUCCESS = "SUCCESS";

    private static final String SYNC_STATUS_FAILED = "FAILED";

    private static final String SHOWAPI_PROVIDER_CODE = "SHOWAPI_BANK_RATE";

    private static final String SHOWAPI_PROVIDER_NAME = "ShowAPI银行汇率查询";

    private static final String SHOWAPI_RATE_URL = "https://route.showapi.com/105-30";

    private static final String BASE_CURRENCY_CNY = "CNY";

    private static final String DEFAULT_RATE_ANCHOR_TIME = "09:30:00";

    private static final ZoneId RATE_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private FinanceCurrencyMapper financeCurrencyMapper;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private SecretCipherSupport secretCipherSupport;

    @Autowired
    private CurrencyRateSyncClient rateSyncClient;

    @Override
    public List<FinanceCurrency> selectCurrencyList(FinanceCurrency query)
    {
        return financeCurrencyMapper.selectCurrencyList(query);
    }

    @Override
    public FinanceCurrency selectCurrencyByCode(String currencyCode)
    {
        FinanceCurrency currency = financeCurrencyMapper.selectCurrencyByCode(normalizeCode(currencyCode));
        if (currency == null)
        {
            throw new ServiceException("币种配置不存在");
        }
        return currency;
    }

    @Override
    public List<FinanceCurrencyOption> selectEnabledCurrencyOptions()
    {
        return financeCurrencyMapper.selectEnabledCurrencyOptions();
    }

    @Override
    @Transactional
    public int insertCurrency(FinanceCurrency currency)
    {
        normalizeCurrency(currency, true);
        if (financeCurrencyMapper.selectCurrencyByCode(currency.getCurrencyCode()) != null)
        {
            throw new ServiceException("币种配置已存在");
        }
        currency.setCreateBy(currentUsername());
        if (YES.equals(currency.getIsDefault()))
        {
            financeCurrencyMapper.clearDefaultCurrency(currency.getCurrencyCode(), currency.getCreateBy());
        }
        int rows = financeCurrencyMapper.insertCurrency(currency);
        insertManualHistoryIfNeeded(currency, "新增币种配置");
        return rows;
    }

    @Override
    @Transactional
    public int updateCurrency(String currencyCode, FinanceCurrency currency)
    {
        String normalizedCode = normalizeCode(currencyCode);
        FinanceCurrency current = selectCurrencyByCode(normalizedCode);
        currency.setCurrencyCode(normalizedCode);
        normalizeCurrency(currency, false);
        currency.setCurrencyId(current.getCurrencyId());
        currency.setUpdateBy(currentUsername());
        if (YES.equals(currency.getIsDefault()))
        {
            financeCurrencyMapper.clearDefaultCurrency(currency.getCurrencyCode(), currency.getUpdateBy());
        }
        int rows = financeCurrencyMapper.updateCurrency(currency);
        if (rateChanged(current.getEffectiveRate(), currency.getEffectiveRate()))
        {
            insertManualHistoryIfNeeded(currency, "人工维护生效汇率");
        }
        return rows;
    }

    @Override
    public int updateCurrencyStatus(String currencyCode, String status)
    {
        String normalizedStatus = normalizeStatus(status);
        selectCurrencyByCode(currencyCode);
        return financeCurrencyMapper.updateCurrencyStatus(normalizeCode(currencyCode), normalizedStatus, currentUsername());
    }

    @Override
    @Transactional
    public int deleteCurrencyByCode(String currencyCode)
    {
        String normalizedCode = normalizeCode(currencyCode);
        selectCurrencyByCode(normalizedCode);
        if (financeCurrencyMapper.countRateHistoryByCurrencyCode(normalizedCode) > 0)
        {
            throw new ServiceException("币种已有汇率历史，不能删除，请停用");
        }
        return financeCurrencyMapper.deleteCurrencyByCode(normalizedCode);
    }

    @Override
    public List<FinanceCurrencyRateHistory> selectRateHistoryList(String currencyCode)
    {
        return financeCurrencyMapper.selectRateHistoryList(normalizeCode(currencyCode));
    }

    @Override
    public FinanceCurrencySyncConfig selectSyncConfig()
    {
        FinanceCurrencySyncConfig config = selectStoredSyncConfig();
        if (config == null)
        {
            return defaultSyncConfig();
        }
        normalizeSyncConfig(config);
        return config;
    }

    @Override
    @Transactional
    public int saveSyncConfig(FinanceCurrencySyncConfig config)
    {
        normalizeSyncConfig(config);
        FinanceCurrencySyncConfig current = selectStoredSyncConfig();
        applyCredential(config, current);
        if (current == null)
        {
            config.setCreateBy(currentUsername());
            return financeCurrencyMapper.insertSyncConfig(config);
        }
        config.setSyncConfigId(current.getSyncConfigId());
        config.setCreateBy(current.getCreateBy());
        config.setCreateTime(current.getCreateTime());
        config.setUpdateBy(currentUsername());
        return financeCurrencyMapper.updateSyncConfig(config);
    }

    @Override
    public FinanceCurrencySyncResult testSyncConfig(FinanceCurrencySyncConfig config)
    {
        FinanceCurrencySyncConfig target = mergeTestConfig(config);
        String credential = resolveCredential(target);
        return fetchAndMaybeApplyRates(target, credential, false);
    }

    @Override
    @Transactional
    public FinanceCurrencySyncResult syncRates()
    {
        FinanceCurrencySyncConfig config = selectStoredSyncConfig();
        if (config == null)
        {
            throw new ServiceException("请先维护汇率同步设置");
        }
        normalizeSyncConfig(config);
        if (!STATUS_NORMAL.equals(config.getStatus()))
        {
            throw new ServiceException("汇率同步设置已停用");
        }
        return fetchAndMaybeApplyRates(config, resolveCredential(config), true);
    }

    @Override
    public List<FinanceCurrencySyncLog> selectSyncLogList(FinanceCurrencySyncLog query)
    {
        return financeCurrencyMapper.selectSyncLogList(query);
    }

    private FinanceCurrencySyncResult fetchAndMaybeApplyRates(FinanceCurrencySyncConfig config, String credential, boolean applyRates)
    {
        Date requestTime = new Date();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        FinanceCurrencySyncLog log = buildSyncLog(config, credential, traceId, requestTime);
        try
        {
            CurrencyRateSyncResponse response = rateSyncClient.fetchRates(config, credential);
            int updatedCount = applyRates ? applySyncedRates(config, response) : 0;
            log.setResponseTime(new Date());
            log.setCostMs(log.getResponseTime().getTime() - requestTime.getTime());
            log.setStatus(SYNC_STATUS_SUCCESS);
            log.setCurrencyCount(response.getCandidates().size());
            log.setUpdatedCount(updatedCount);
            log.setResponseSummary(response.getResponseSummary());
            financeCurrencyMapper.insertSyncLog(log);
            updateSyncConfigSummary(config, SYNC_STATUS_SUCCESS);
            return buildSyncResult(traceId, response.getCandidates().size(), updatedCount, SYNC_STATUS_SUCCESS);
        }
        catch (ServiceException ex)
        {
            log.setResponseTime(new Date());
            log.setCostMs(log.getResponseTime().getTime() - requestTime.getTime());
            log.setStatus(SYNC_STATUS_FAILED);
            log.setErrorCode("SYNC_FAILED");
            log.setErrorMessage(limit(ex.getMessage(), 1000));
            financeCurrencyMapper.insertSyncLog(log);
            updateSyncConfigSummary(config, SYNC_STATUS_FAILED);
            throw ex;
        }
    }

    private FinanceCurrencySyncConfig selectStoredSyncConfig()
    {
        FinanceCurrencySyncConfig config = financeCurrencyMapper.selectSyncConfigByProviderCode(SHOWAPI_PROVIDER_CODE);
        return config == null ? financeCurrencyMapper.selectFirstSyncConfig() : config;
    }

    private int applySyncedRates(FinanceCurrencySyncConfig config, CurrencyRateSyncResponse response)
    {
        List<FinanceCurrency> currencies = financeCurrencyMapper.selectEnabledCurrencyList();
        int updatedCount = 0;
        for (FinanceCurrency currency : currencies)
        {
            CurrencyRateCandidate candidate = selectAnchorCandidate(response, currency.getCurrencyCode(), config.getRateAnchorTime());
            if (candidate == null)
            {
                continue;
            }
            BigDecimal officialRate = candidate.getOfficialRate();
            BigDecimal effectiveRate = calculateEffectiveRate(currency, officialRate);
            Date effectiveRateTime = new Date();
            financeCurrencyMapper.updateSyncedRate(currency.getCurrencyCode(),
                StringUtils.defaultIfBlank(response.getBaseCurrencyCode(), config.getBaseCurrencyCode()),
                officialRate, effectiveRate, candidate.getOfficialRateTime(), effectiveRateTime, "system");
            insertSyncHistory(config, currency, officialRate, effectiveRate, candidate.getOfficialRateTime(), effectiveRateTime);
            updatedCount++;
        }
        if (updatedCount == 0)
        {
            throw new ServiceException("未找到当天汇率基准时间之后的官方汇率，本次不更新");
        }
        return updatedCount;
    }

    private CurrencyRateCandidate selectAnchorCandidate(CurrencyRateSyncResponse response, String currencyCode, String anchorTime)
    {
        LocalDate today = LocalDate.now(RATE_ZONE);
        LocalTime anchor = parseAnchorTime(anchorTime);
        return response.getCandidates().stream()
            .filter(candidate -> currencyCode.equals(candidate.getCurrencyCode()))
            .filter(candidate -> isOnOrAfterAnchor(candidate, today, anchor))
            .sorted(Comparator.comparing(CurrencyRateCandidate::getOfficialRateTime))
            .findFirst()
            .orElse(null);
    }

    private boolean isOnOrAfterAnchor(CurrencyRateCandidate candidate, LocalDate today, LocalTime anchor)
    {
        if (candidate.getOfficialRateTime() == null)
        {
            return false;
        }
        ZonedDateTime rateTime = candidate.getOfficialRateTime().toInstant().atZone(RATE_ZONE);
        return today.equals(rateTime.toLocalDate()) && !rateTime.toLocalTime().isBefore(anchor);
    }

    private BigDecimal calculateEffectiveRate(FinanceCurrency currency, BigDecimal officialRate)
    {
        BigDecimal effectiveRate = officialRate;
        String mode = StringUtils.defaultIfBlank(currency.getAdjustmentMode(), "NONE");
        BigDecimal adjustmentValue = currency.getAdjustmentValue();
        if ("MANUAL".equals(mode) && currency.getEffectiveRate() != null)
        {
            effectiveRate = currency.getEffectiveRate();
        }
        else if ("PERCENT_UP".equals(mode) && adjustmentValue != null)
        {
            effectiveRate = officialRate.multiply(BigDecimal.ONE.add(adjustmentValue));
        }
        else if ("PERCENT_DOWN".equals(mode) && adjustmentValue != null)
        {
            effectiveRate = officialRate.multiply(BigDecimal.ONE.subtract(adjustmentValue));
        }
        else if ("FIXED_DELTA".equals(mode) && adjustmentValue != null)
        {
            effectiveRate = officialRate.add(adjustmentValue);
        }
        return effectiveRate.setScale(defaultInt(currency.getRatePrecision(), 8), resolveRoundingMode(currency.getRoundingMode()));
    }

    private void normalizeCurrency(FinanceCurrency currency, boolean inserting)
    {
        currency.setCurrencyCode(normalizeCode(currency.getCurrencyCode()));
        String dictLabel = dictDataService.selectDictLabel(DICT_CURRENCY_CODE, currency.getCurrencyCode());
        if (StringUtils.isBlank(dictLabel))
        {
            throw new ServiceException("币种不在 currency_code 字典中");
        }
        if (StringUtils.isBlank(currency.getCurrencyName()))
        {
            currency.setCurrencyName(dictLabel);
        }
        currency.setCurrencyName(StringUtils.trimToEmpty(currency.getCurrencyName()));
        currency.setCurrencySymbol(StringUtils.trimToEmpty(currency.getCurrencySymbol()));
        currency.setBaseCurrencyCode(StringUtils.defaultIfBlank(normalizeCode(currency.getBaseCurrencyCode()), BASE_CURRENCY_CNY));
        currency.setRatePrecision(defaultInt(currency.getRatePrecision(), 8));
        currency.setAmountPrecision(defaultInt(currency.getAmountPrecision(), 2));
        currency.setRoundingMode(normalizeRoundingMode(currency.getRoundingMode()));
        currency.setAdjustmentMode(StringUtils.defaultIfBlank(currency.getAdjustmentMode(), "NONE").trim().toUpperCase());
        currency.setIsDefault(normalizeYesNo(currency.getIsDefault()));
        currency.setStatus(normalizeStatus(currency.getStatus()));
        Date now = new Date();
        if (currency.getOfficialRate() != null && currency.getOfficialRateTime() == null)
        {
            currency.setOfficialRateTime(now);
        }
        if ((currency.getEffectiveRate() != null || inserting) && currency.getEffectiveRateTime() == null)
        {
            currency.setEffectiveRateTime(now);
        }
    }

    private void normalizeSyncConfig(FinanceCurrencySyncConfig config)
    {
        config.setProviderCode(SHOWAPI_PROVIDER_CODE);
        config.setProviderName(SHOWAPI_PROVIDER_NAME);
        config.setBaseCurrencyCode(BASE_CURRENCY_CNY);
        config.setApiBaseUrl(SHOWAPI_RATE_URL);
        config.setAuthType("APP_KEY");
        config.setRequestTimeoutMs(10000);
        config.setRetryCount(0);
        config.setScheduleType("DAILY");
        config.setCronExpression("");
        config.setRateAnchorTime(normalizeRateAnchorTime(config.getRateAnchorTime()));
        config.setSyncEnabled(StringUtils.defaultIfBlank(config.getSyncEnabled(), "0"));
        config.setStatus(normalizeStatus(config.getStatus()));
        config.setRemark(StringUtils.trimToEmpty(config.getRemark()));
    }

    private void applyCredential(FinanceCurrencySyncConfig config, FinanceCurrencySyncConfig current)
    {
        if (StringUtils.isNotBlank(config.getCredential()))
        {
            String credential = config.getCredential().trim();
            config.setCredentialCiphertext(secretCipherSupport.encrypt(credential));
            config.setCredentialKeyId(secretCipherSupport.getEncryptionKeyId());
            config.setCredentialMasked(maskCredential(credential));
            return;
        }
        if (current == null || StringUtils.isBlank(current.getCredentialCiphertext()))
        {
            throw new ServiceException("请填写 ShowAPI appKey");
        }
        config.setCredentialCiphertext(current.getCredentialCiphertext());
        config.setCredentialKeyId(current.getCredentialKeyId());
        config.setCredentialMasked(current.getCredentialMasked());
    }

    private String resolveCredential(FinanceCurrencySyncConfig config)
    {
        if (StringUtils.isNotBlank(config.getCredential()))
        {
            return config.getCredential().trim();
        }
        if (StringUtils.isBlank(config.getCredentialCiphertext()))
        {
            throw new ServiceException("请先维护 ShowAPI appKey");
        }
        return secretCipherSupport.decrypt(config.getCredentialCiphertext());
    }

    private FinanceCurrencySyncConfig mergeTestConfig(FinanceCurrencySyncConfig request)
    {
        FinanceCurrencySyncConfig current = financeCurrencyMapper.selectFirstSyncConfig();
        FinanceCurrencySyncConfig target = current == null ? defaultSyncConfig() : current;
        if (request != null)
        {
            if (StringUtils.isNotBlank(request.getCredential()))
            {
                target.setCredential(request.getCredential());
            }
            if (StringUtils.isNotBlank(request.getRateAnchorTime()))
            {
                target.setRateAnchorTime(request.getRateAnchorTime());
            }
        }
        normalizeSyncConfig(target);
        return target;
    }

    private FinanceCurrencySyncConfig defaultSyncConfig()
    {
        FinanceCurrencySyncConfig config = new FinanceCurrencySyncConfig();
        config.setProviderCode(SHOWAPI_PROVIDER_CODE);
        config.setProviderName(SHOWAPI_PROVIDER_NAME);
        config.setBaseCurrencyCode(BASE_CURRENCY_CNY);
        config.setApiBaseUrl(SHOWAPI_RATE_URL);
        config.setAuthType("APP_KEY");
        config.setRequestTimeoutMs(10000);
        config.setRetryCount(0);
        config.setScheduleType("DAILY");
        config.setCronExpression("");
        config.setRateAnchorTime(DEFAULT_RATE_ANCHOR_TIME);
        config.setSyncEnabled("0");
        config.setStatus(STATUS_NORMAL);
        return config;
    }

    private FinanceCurrencySyncLog buildSyncLog(FinanceCurrencySyncConfig config, String credential,
        String traceId, Date requestTime)
    {
        FinanceCurrencySyncLog log = new FinanceCurrencySyncLog();
        log.setTraceId(traceId);
        log.setSyncConfigId(config.getSyncConfigId() == null ? 0L : config.getSyncConfigId());
        log.setProviderCode(config.getProviderCode());
        log.setRequestUrl(rateSyncClient.maskRequestUrl(credential));
        log.setRequestTime(requestTime);
        log.setCreateTime(requestTime);
        log.setCurrencyCount(0);
        log.setUpdatedCount(0);
        return log;
    }

    private FinanceCurrencySyncResult buildSyncResult(String traceId, int currencyCount, int updatedCount, String status)
    {
        FinanceCurrencySyncResult result = new FinanceCurrencySyncResult();
        result.setTraceId(traceId);
        result.setCurrencyCount(currencyCount);
        result.setUpdatedCount(updatedCount);
        result.setStatus(status);
        return result;
    }

    private void insertManualHistoryIfNeeded(FinanceCurrency currency, String reason)
    {
        if (currency.getEffectiveRate() == null)
        {
            return;
        }
        FinanceCurrencyRateHistory history = new FinanceCurrencyRateHistory();
        history.setCurrencyCode(currency.getCurrencyCode());
        history.setBaseCurrencyCode(currency.getBaseCurrencyCode());
        history.setOfficialRate(currency.getOfficialRate());
        history.setEffectiveRate(currency.getEffectiveRate());
        history.setAdjustmentMode(currency.getAdjustmentMode());
        history.setAdjustmentValue(currency.getAdjustmentValue());
        history.setSourceType(SOURCE_MANUAL);
        history.setOfficialRateTime(currency.getOfficialRateTime());
        history.setEffectiveRateTime(new Date());
        history.setChangeReason(reason);
        history.setCreateBy(currentUsername());
        financeCurrencyMapper.insertRateHistory(history);
    }

    private void insertSyncHistory(FinanceCurrencySyncConfig config, FinanceCurrency currency, BigDecimal officialRate,
        BigDecimal effectiveRate, Date officialRateTime, Date effectiveRateTime)
    {
        FinanceCurrencyRateHistory history = new FinanceCurrencyRateHistory();
        history.setCurrencyCode(currency.getCurrencyCode());
        history.setBaseCurrencyCode(config.getBaseCurrencyCode());
        history.setOfficialRate(officialRate);
        history.setEffectiveRate(effectiveRate);
        history.setAdjustmentMode(currency.getAdjustmentMode());
        history.setAdjustmentValue(currency.getAdjustmentValue());
        history.setSourceType(SOURCE_SYNC);
        history.setSourceConfigId(config.getSyncConfigId());
        history.setOfficialRateTime(officialRateTime);
        history.setEffectiveRateTime(effectiveRateTime);
        history.setChangeReason("外部官方汇率同步");
        history.setCreateBy("system");
        financeCurrencyMapper.insertRateHistory(history);
    }

    private void updateSyncConfigSummary(FinanceCurrencySyncConfig config, String status)
    {
        if (config.getSyncConfigId() != null)
        {
            financeCurrencyMapper.updateSyncConfigSummary(config.getSyncConfigId(), status, currentUsernameOrSystem());
        }
    }

    private String normalizeCode(String code)
    {
        return StringUtils.trimToEmpty(code).toUpperCase();
    }

    private String normalizeStatus(String status)
    {
        String normalized = StringUtils.defaultIfBlank(status, STATUS_NORMAL);
        if (!STATUS_NORMAL.equals(normalized) && !STATUS_DISABLED.equals(normalized))
        {
            throw new ServiceException("状态值不合法");
        }
        return normalized;
    }

    private String normalizeYesNo(String value)
    {
        String normalized = StringUtils.defaultIfBlank(value, NO).trim().toUpperCase();
        if (!YES.equals(normalized) && !NO.equals(normalized))
        {
            throw new ServiceException("是否默认值不合法");
        }
        return normalized;
    }

    private String normalizeRoundingMode(String roundingMode)
    {
        String normalized = StringUtils.defaultIfBlank(roundingMode, "HALF_UP").trim().toUpperCase();
        resolveRoundingMode(normalized);
        return normalized;
    }

    private String normalizeRateAnchorTime(String rateAnchorTime)
    {
        LocalTime localTime = parseAnchorTime(rateAnchorTime);
        return localTime.toString().length() == 5 ? localTime + ":00" : localTime.toString();
    }

    private LocalTime parseAnchorTime(String rateAnchorTime)
    {
        String value = StringUtils.defaultIfBlank(rateAnchorTime, DEFAULT_RATE_ANCHOR_TIME).trim();
        try
        {
            return LocalTime.parse(value.length() == 5 ? value + ":00" : value);
        }
        catch (Exception ex)
        {
            throw new ServiceException("汇率基准时间格式不合法");
        }
    }

    private RoundingMode resolveRoundingMode(String roundingMode)
    {
        try
        {
            return RoundingMode.valueOf(StringUtils.defaultIfBlank(roundingMode, "HALF_UP"));
        }
        catch (IllegalArgumentException ex)
        {
            throw new ServiceException("舍入方式不合法");
        }
    }

    private int defaultInt(Integer value, int defaultValue)
    {
        return value == null ? defaultValue : value;
    }

    private boolean rateChanged(BigDecimal before, BigDecimal after)
    {
        if (before == null && after == null)
        {
            return false;
        }
        if (before == null || after == null)
        {
            return true;
        }
        return before.compareTo(after) != 0;
    }

    private String maskCredential(String credential)
    {
        if (StringUtils.isBlank(credential))
        {
            return "";
        }
        String trimmed = credential.trim();
        if (trimmed.length() <= 8)
        {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String limit(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String currentUsername()
    {
        return SecurityUtils.getUsername();
    }

    private String currentUsernameOrSystem()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception ex)
        {
            return "system";
        }
    }
}
