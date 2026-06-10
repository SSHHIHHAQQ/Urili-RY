package com.ruoyi.logistics.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 物流商 hash 工具。
 */
public final class LogisticsHashUtils
{
    private LogisticsHashUtils()
    {
    }

    public static String sha256Hex(String value)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest)
            {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("hash failed", ex);
        }
    }
}
