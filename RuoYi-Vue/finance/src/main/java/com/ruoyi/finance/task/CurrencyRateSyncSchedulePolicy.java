package com.ruoyi.finance.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.apache.commons.lang3.StringUtils;

/**
 * 币种汇率同步调度规则。
 */
class CurrencyRateSyncSchedulePolicy
{
    static final String DEFAULT_RATE_ANCHOR_TIME = "09:30:00";

    static final int FIRST_FETCH_DELAY_MINUTES = 1;

    static final int RETRY_INTERVAL_MINUTES = 15;

    static final int RETRY_TIMES = 4;

    static final int MAX_ATTEMPTS = 1 + RETRY_TIMES;

    private CurrencyRateSyncSchedulePolicy()
    {
    }

    static boolean shouldAttempt(LocalDateTime now, String rateAnchorTime, int attempts)
    {
        if (attempts < 0 || attempts >= MAX_ATTEMPTS)
        {
            return false;
        }
        LocalDateTime firstFetchTime = firstFetchTime(now.toLocalDate(), rateAnchorTime);
        LocalDateTime nextFetchTime = firstFetchTime.plusMinutes((long) attempts * RETRY_INTERVAL_MINUTES);
        return !now.isBefore(nextFetchTime);
    }

    static LocalDateTime firstFetchTime(LocalDate date, String rateAnchorTime)
    {
        return LocalDateTime.of(date, parseAnchorTime(rateAnchorTime)).plusMinutes(FIRST_FETCH_DELAY_MINUTES);
    }

    static LocalTime parseAnchorTime(String rateAnchorTime)
    {
        String value = StringUtils.defaultIfBlank(rateAnchorTime, DEFAULT_RATE_ANCHOR_TIME).trim();
        return LocalTime.parse(value.length() == 5 ? value + ":00" : value);
    }
}
