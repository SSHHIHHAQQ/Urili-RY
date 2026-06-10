package com.ruoyi.common.utils.file;

import java.util.Locale;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 图片资源字段只允许保存可复用的资源路径或外部 URL，不能保存内联 base64。
 */
public final class ImageResourceUtils
{
    private static final String API_PROFILE_PREFIX = "/api/profile/";

    private static final String PROFILE_SEGMENT = "/profile/";

    private static final String INLINE_IMAGE_REDACTION = "[inline image base64 redacted]";

    private static final Pattern DATA_URL_BASE64_PATTERN = Pattern.compile(
        "data:[a-zA-Z0-9.+/-]+;base64,[A-Za-z0-9+/=\\r\\n]+", Pattern.CASE_INSENSITIVE);

    private ImageResourceUtils()
    {
    }

    public static String normalizeStoredImageResource(String value, String fieldLabel)
    {
        String resource = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(resource))
        {
            return "";
        }
        assertNotInlineImage(resource, fieldLabel);
        return normalizeProfileResource(resource);
    }

    public static String normalizeExternalImageResourceOrEmpty(String value)
    {
        String resource = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(resource) || isInlineImage(resource))
        {
            return "";
        }
        return normalizeProfileResource(resource);
    }

    public static String normalizeDetailContentImageResources(String detailContent, String fieldLabel)
    {
        String content = StringUtils.defaultString(detailContent);
        if (StringUtils.isBlank(content))
        {
            return "";
        }
        assertNotInlineImage(content, fieldLabel);
        try
        {
            Object parsed = JSON.parse(content);
            if (parsed instanceof JSONObject object)
            {
                Object blocks = object.get("blocks");
                if (blocks instanceof JSONArray array)
                {
                    normalizeImageUrlBlocks(array, fieldLabel);
                    return object.toJSONString();
                }
            }
            if (parsed instanceof JSONArray array)
            {
                normalizeImageUrlBlocks(array, fieldLabel);
                return array.toJSONString();
            }
            return content;
        }
        catch (RuntimeException e)
        {
            return content;
        }
    }

    public static void assertNotInlineImage(String value, String fieldLabel)
    {
        if (isInlineImage(value))
        {
            throw new ServiceException(fieldLabel + "必须先上传到文件存储，不能保存 base64 内容");
        }
    }

    public static String redactInlineImagePayloads(String value)
    {
        if (StringUtils.isBlank(value))
        {
            return value;
        }
        String redacted = DATA_URL_BASE64_PATTERN.matcher(value).replaceAll(INLINE_IMAGE_REDACTION);
        String compact = redacted.trim().replaceAll("\\s+", "");
        if (compact.length() >= 100 && isInlineImage(compact))
        {
            return INLINE_IMAGE_REDACTION;
        }
        return redacted;
    }

    public static boolean isInlineImage(String value)
    {
        String resource = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(resource))
        {
            return false;
        }
        String lower = resource.toLowerCase(Locale.ROOT);
        if (lower.contains("data:image/") || lower.contains(";base64,") || lower.startsWith("base64,"))
        {
            return true;
        }
        String compact = resource.replaceAll("\\s+", "");
        if (compact.length() < 100)
        {
            return false;
        }
        return compact.startsWith("/9j/") || compact.startsWith("iVBOR") || compact.startsWith("R0lGOD")
            || compact.startsWith("UklGR") || compact.startsWith("PHN2Zy");
    }

    private static void normalizeImageUrlBlocks(JSONArray blocks, String fieldLabel)
    {
        for (Object item : blocks)
        {
            if (item instanceof JSONObject block)
            {
                String imageUrl = block.getString("imageUrl");
                if (StringUtils.isNotBlank(imageUrl))
                {
                    block.put("imageUrl", normalizeStoredImageResource(imageUrl, fieldLabel));
                }
            }
        }
    }

    private static String normalizeProfileResource(String resource)
    {
        if (resource.startsWith(API_PROFILE_PREFIX))
        {
            return stripQueryAndHash(resource.substring("/api".length()));
        }
        int profileIndex = resource.indexOf(PROFILE_SEGMENT);
        if (profileIndex >= 0)
        {
            return stripQueryAndHash(resource.substring(profileIndex));
        }
        return resource;
    }

    private static String stripQueryAndHash(String resource)
    {
        int queryIndex = resource.indexOf('?');
        int hashIndex = resource.indexOf('#');
        int end = resource.length();
        if (queryIndex >= 0)
        {
            end = Math.min(end, queryIndex);
        }
        if (hashIndex >= 0)
        {
            end = Math.min(end, hashIndex);
        }
        return resource.substring(0, end);
    }
}
