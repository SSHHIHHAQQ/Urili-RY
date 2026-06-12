package com.ruoyi.integration.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.lingxing.LingxingClientException;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;

/**
 * Guards upstream signed requests from local host clock drift.
 */
@Component
public class UpstreamClockHealthGuard
{
    private static final long MAX_CLOCK_SKEW_MS = 60_000L;

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    public void assertSystemClockHealthy()
    {
        Long databaseEpochMillis = upstreamSystemMapper.selectDatabaseEpochMillis();
        if (databaseEpochMillis == null || databaseEpochMillis <= 0)
        {
            throw new LingxingClientException("LOCAL_CLOCK_UNAVAILABLE",
                "Unable to verify local clock before calling Lingxing", false);
        }
        long skewMs = Math.abs(System.currentTimeMillis() - databaseEpochMillis);
        if (skewMs > MAX_CLOCK_SKEW_MS)
        {
            throw new LingxingClientException("LOCAL_CLOCK_SKEW",
                "Local system clock differs from database time by " + skewMs
                    + "ms; fix host time sync before calling Lingxing",
                false);
        }
    }
}
