package com.ruoyi.finance.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.Test;

public class CurrencyRateSyncSchedulePolicyTest
{
    @Test
    public void firstFetchTimeIsOneMinuteAfterAnchor()
    {
        LocalDate date = LocalDate.of(2026, 6, 4);

        LocalDateTime firstFetchTime = CurrencyRateSyncSchedulePolicy.firstFetchTime(date, "09:30:00");

        assertEquals(LocalDateTime.of(2026, 6, 4, 9, 31), firstFetchTime);
    }

    @Test
    public void shouldAttemptOnlyAfterNextDueTime()
    {
        assertFalse(CurrencyRateSyncSchedulePolicy.shouldAttempt(LocalDateTime.of(2026, 6, 4, 9, 30), "09:30:00", 0));
        assertTrue(CurrencyRateSyncSchedulePolicy.shouldAttempt(LocalDateTime.of(2026, 6, 4, 9, 31), "09:30:00", 0));
        assertFalse(CurrencyRateSyncSchedulePolicy.shouldAttempt(LocalDateTime.of(2026, 6, 4, 9, 45), "09:30:00", 1));
        assertTrue(CurrencyRateSyncSchedulePolicy.shouldAttempt(LocalDateTime.of(2026, 6, 4, 9, 46), "09:30:00", 1));
    }

    @Test
    public void shouldStopAfterInitialAttemptAndFourRetries()
    {
        LocalDateTime afterRetryWindow = LocalDateTime.of(2026, 6, 4, 10, 31);

        assertTrue(CurrencyRateSyncSchedulePolicy.shouldAttempt(afterRetryWindow, "09:30:00", 4));
        assertFalse(CurrencyRateSyncSchedulePolicy.shouldAttempt(afterRetryWindow, "09:30:00", 5));
    }

    @Test
    public void shouldUseDefaultAnchorWhenBlank()
    {
        assertEquals(LocalTime.of(9, 30), CurrencyRateSyncSchedulePolicy.parseAnchorTime(""));
    }
}
