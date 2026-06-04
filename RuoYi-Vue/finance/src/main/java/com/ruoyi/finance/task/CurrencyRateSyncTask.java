package com.ruoyi.finance.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.finance.domain.FinanceCurrencySyncConfig;
import com.ruoyi.finance.domain.FinanceCurrencySyncResult;
import com.ruoyi.finance.service.IFinanceCurrencyService;

/**
 * 币种官方汇率定时同步入口。
 */
@Component("currencyRateSyncTask")
public class CurrencyRateSyncTask
{
    private static final Logger log = LoggerFactory.getLogger(CurrencyRateSyncTask.class);

    private static final String STATUS_NORMAL = "0";

    private static final String SYNC_ENABLED = "1";

    private static final String ANCHOR_MISSING_MESSAGE = "未找到当天汇率基准时间之后的官方汇率";

    private static final String STATE_KEY_PREFIX = "finance:currency:scheduled-sync:";

    private static final int STATE_TTL_DAYS = 2;

    private static final ZoneId RATE_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private IFinanceCurrencyService financeCurrencyService;

    @Autowired
    private RedisCache redisCache;

    public void syncDailyRates()
    {
        FinanceCurrencySyncConfig config = financeCurrencyService.selectSyncConfig();
        if (!shouldSchedule(config))
        {
            return;
        }

        LocalDate today = LocalDate.now(RATE_ZONE);
        LocalDateTime now = LocalDateTime.now(RATE_ZONE);
        String stateKey = STATE_KEY_PREFIX + today.format(DateTimeFormatter.BASIC_ISO_DATE);
        if (Boolean.TRUE.equals(redisCache.getCacheObject(completedKey(stateKey))))
        {
            return;
        }

        int attempts = getAttempts(stateKey);
        if (!CurrencyRateSyncSchedulePolicy.shouldAttempt(now, config.getRateAnchorTime(), attempts))
        {
            return;
        }

        setAttempts(stateKey, attempts + 1);
        try
        {
            FinanceCurrencySyncResult result = financeCurrencyService.syncRates();
            markCompleted(stateKey);
            log.info("币种官方汇率定时同步成功，traceId={}, 返回币种数={}, 更新币种数={}",
                result.getTraceId(), result.getCurrencyCount(), result.getUpdatedCount());
        }
        catch (ServiceException ex)
        {
            if (isAnchorMissing(ex))
            {
                handleAnchorMissing(stateKey, attempts + 1, ex);
                return;
            }
            throw ex;
        }
    }

    private boolean shouldSchedule(FinanceCurrencySyncConfig config)
    {
        return config != null
            && config.getSyncConfigId() != null
            && STATUS_NORMAL.equals(config.getStatus())
            && SYNC_ENABLED.equals(config.getSyncEnabled());
    }

    private void handleAnchorMissing(String stateKey, int attempts, ServiceException ex)
    {
        if (attempts >= CurrencyRateSyncSchedulePolicy.MAX_ATTEMPTS)
        {
            markCompleted(stateKey);
            log.warn("币种官方汇率定时同步已重试{}次仍无符合基准时间的数据，保留上次成功汇率。",
                CurrencyRateSyncSchedulePolicy.RETRY_TIMES, ex);
            return;
        }
        log.warn("币种官方汇率定时同步未找到符合基准时间的数据，将在{}分钟后第{}次重试。",
            CurrencyRateSyncSchedulePolicy.RETRY_INTERVAL_MINUTES, attempts);
    }

    private int getAttempts(String stateKey)
    {
        Integer attempts = redisCache.getCacheObject(attemptsKey(stateKey));
        return attempts == null ? 0 : attempts;
    }

    private void setAttempts(String stateKey, int attempts)
    {
        redisCache.setCacheObject(attemptsKey(stateKey), attempts, STATE_TTL_DAYS, TimeUnit.DAYS);
    }

    private void markCompleted(String stateKey)
    {
        redisCache.setCacheObject(completedKey(stateKey), true, STATE_TTL_DAYS, TimeUnit.DAYS);
    }

    private String attemptsKey(String stateKey)
    {
        return stateKey + ":attempts";
    }

    private String completedKey(String stateKey)
    {
        return stateKey + ":completed";
    }

    private boolean isAnchorMissing(ServiceException ex)
    {
        String message = ex.getMessage();
        return message != null && message.contains(ANCHOR_MISSING_MESSAGE);
    }
}
