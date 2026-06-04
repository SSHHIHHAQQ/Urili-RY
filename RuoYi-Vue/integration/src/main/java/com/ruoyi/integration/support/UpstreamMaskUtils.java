package com.ruoyi.integration.support;

import org.apache.commons.lang3.StringUtils;

/**
 * 上游系统脱敏工具。
 */
public final class UpstreamMaskUtils
{
    private UpstreamMaskUtils()
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

    public static String redactJson(String value)
    {
        if (StringUtils.isBlank(value))
        {
            return "";
        }
        return value
            .replaceAll("(?i)(\"appSecret\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
            .replaceAll("(?i)(\"authcode\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
            .replaceAll("(?i)(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");
    }
}
