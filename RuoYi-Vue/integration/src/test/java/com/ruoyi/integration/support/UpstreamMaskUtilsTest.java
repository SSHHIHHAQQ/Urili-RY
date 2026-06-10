package com.ruoyi.integration.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UpstreamMaskUtilsTest
{
    @Test
    public void redactJsonMasksCredentialKeys()
    {
        String redacted = UpstreamMaskUtils.redactJson(
            "{\"appKey\":\"plain-app-key\",\"app_key\":\"plain_app_key\",\"appSecret\":\"plain-secret\","
                + "\"authcode\":\"plain-auth\",\"password\":\"plain-password\",\"business\":\"keep\"}");

        assertFalse(redacted.contains("plain-app-key"));
        assertFalse(redacted.contains("plain_app_key"));
        assertFalse(redacted.contains("plain-secret"));
        assertFalse(redacted.contains("plain-auth"));
        assertFalse(redacted.contains("plain-password"));
        assertTrue(redacted.contains("\"appKey\":\"****\""));
        assertTrue(redacted.contains("\"app_key\":\"****\""));
        assertTrue(redacted.contains("\"business\":\"keep\""));
    }
}
