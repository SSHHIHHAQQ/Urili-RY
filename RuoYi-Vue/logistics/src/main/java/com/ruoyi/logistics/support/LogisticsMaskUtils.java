package com.ruoyi.logistics.support;

import org.apache.commons.lang3.StringUtils;

/**
 * 物流商接入脱敏工具。
 */
public final class LogisticsMaskUtils
{
    private LogisticsMaskUtils()
    {
    }

    public static String mask(String value)
    {
        if (StringUtils.isBlank(value))
        {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8)
        {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    public static String maskEmail(String value)
    {
        if (StringUtils.isBlank(value))
        {
            return "";
        }
        String trimmed = value.trim();
        int at = trimmed.indexOf('@');
        if (at <= 1)
        {
            return mask(trimmed);
        }
        return trimmed.substring(0, Math.min(3, at)) + "***" + trimmed.substring(at);
    }

    public static String redactJson(String value)
    {
        if (StringUtils.isBlank(value))
        {
            return "";
        }
        return value
            .replaceAll("(?i)(\"app_token\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
            .replaceAll("(?i)(\"app_key\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
            .replaceAll("(?i)(\"access_token\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
            .replaceAll("(?i)(\"Authorization\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
            .replaceAll("(?i)(\"oa_telphone\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
            .replaceAll("(?i)(\"shipper_telphone\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");
    }
}
