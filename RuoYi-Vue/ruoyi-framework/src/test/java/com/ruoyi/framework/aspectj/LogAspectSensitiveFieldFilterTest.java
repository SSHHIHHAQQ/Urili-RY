package com.ruoyi.framework.aspectj;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.AjaxResult;

public class LogAspectSensitiveFieldFilterTest
{
    @Test
    public void adminLogFilterExcludesCredentialFieldsFromResponseJson()
    {
        LogAspect aspect = new LogAspect();
        AjaxResult response = AjaxResult.success(new CredentialResponse());

        String json = JSON.toJSONString(response, aspect.excludePropertyPreFilter(new String[0]));

        assertSensitiveFieldsRemoved(json);
        assertTrue(json.contains("safe-value"));
    }

    @Test
    public void portalLogFilterExcludesCredentialFieldsFromResponseJson()
    {
        PortalLogAspect aspect = new PortalLogAspect(null);
        AjaxResult response = AjaxResult.success(new CredentialResponse());

        String json = JSON.toJSONString(response, aspect.excludePropertyPreFilter(new String[0]));

        assertSensitiveFieldsRemoved(json);
        assertTrue(json.contains("safe-value"));
    }

    @Test
    public void credentialExclusionListsMustStayAligned()
    {
        for (String sensitiveField : Arrays.asList("password", "oldPassword", "newPassword", "confirmPassword",
                "token", "jwt", "directLoginToken", "loginUrl", "accessToken", "refreshToken", "authorization",
                "appKey", "appSecret", "credential", "credentialCiphertext", "appKeyCiphertext",
                "appSecretCiphertext"))
        {
            assertTrue(Arrays.asList(LogAspect.EXCLUDE_PROPERTIES).contains(sensitiveField));
            assertTrue(Arrays.asList(PortalLogAspect.EXCLUDE_PROPERTIES).contains(sensitiveField));
        }
    }

    @Test
    public void adminLogFilterExcludesCredentialFieldsFromRequestParamMap() throws Exception
    {
        LogAspect aspect = new LogAspect();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appKey", "secret-value");
        params.put("appSecret", "secret-value");
        params.put("credential", "secret-value");
        params.put("credentialCiphertext", "secret-value");
        params.put("safe", "safe-value");

        Method method = LogAspect.class.getDeclaredMethod("filterRequestParamMap", Map.class, String[].class);
        method.setAccessible(true);
        Map<?, ?> filtered = (Map<?, ?>) method.invoke(aspect, params, new String[] { "extraSecret" });

        assertFalse(filtered.containsKey("appKey"));
        assertFalse(filtered.containsKey("appSecret"));
        assertFalse(filtered.containsKey("credential"));
        assertFalse(filtered.containsKey("credentialCiphertext"));
        assertTrue(filtered.containsKey("safe"));
    }

    @Test
    public void portalLogFilterExcludesScopeAndAuditFields()
    {
        for (String sensitiveField : Arrays.asList("subjectId", "accountId", "sellerId", "buyerId",
                "sellerAccountId", "buyerAccountId", "directLoginTicketId", "actingAdminId",
                "actingAdminName", "directLoginReason", "terminal", "tokenId", "operParam", "jsonResult"))
        {
            assertTrue(Arrays.asList(PortalLogAspect.EXCLUDE_PROPERTIES).contains(sensitiveField));
        }
    }

    @Test
    public void adminLogSanitizesInlineImageBase64Payloads() throws Exception
    {
        LogAspect aspect = new LogAspect();

        String sanitized = invokeSanitizeLogText(aspect,
                "{\"image\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ\"}");

        assertFalse(sanitized.contains("data:image"));
        assertFalse(sanitized.contains("iVBOR"));
        assertTrue(sanitized.contains("inline image"));
    }

    @Test
    public void portalLogSanitizesInlineImageBase64Payloads() throws Exception
    {
        PortalLogAspect aspect = new PortalLogAspect(null);

        String sanitized = invokeSanitizeLogText(aspect,
                "{\"image\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ\"}");

        assertFalse(sanitized.contains("data:image"));
        assertFalse(sanitized.contains("iVBOR"));
        assertTrue(sanitized.contains("inline image"));
    }

    private String invokeSanitizeLogText(Object aspect, String value) throws Exception
    {
        Method method = aspect.getClass().getDeclaredMethod("sanitizeLogText", String.class);
        method.setAccessible(true);
        return (String) method.invoke(aspect, value);
    }

    private void assertSensitiveFieldsRemoved(String json)
    {
        assertFalse(json.contains("secret-value"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("oldPassword"));
        assertFalse(json.contains("newPassword"));
        assertFalse(json.contains("confirmPassword"));
        assertFalse(json.contains("token"));
        assertFalse(json.contains("jwt"));
        assertFalse(json.contains("directLoginToken"));
        assertFalse(json.contains("loginUrl"));
        assertFalse(json.contains("accessToken"));
        assertFalse(json.contains("refreshToken"));
        assertFalse(json.contains("authorization"));
        assertFalse(json.contains("appKey"));
        assertFalse(json.contains("appSecret"));
        assertFalse(json.contains("credential"));
        assertFalse(json.contains("credentialCiphertext"));
        assertFalse(json.contains("appKeyCiphertext"));
        assertFalse(json.contains("appSecretCiphertext"));
    }

    private static class CredentialResponse
    {
        public String getPassword()
        {
            return "secret-value";
        }

        public String getOldPassword()
        {
            return "secret-value";
        }

        public String getNewPassword()
        {
            return "secret-value";
        }

        public String getConfirmPassword()
        {
            return "secret-value";
        }

        public String getToken()
        {
            return "secret-value";
        }

        public String getJwt()
        {
            return "secret-value";
        }

        public String getDirectLoginToken()
        {
            return "secret-value";
        }

        public String getLoginUrl()
        {
            return "secret-value";
        }

        public String getAccessToken()
        {
            return "secret-value";
        }

        public String getRefreshToken()
        {
            return "secret-value";
        }

        public String getAuthorization()
        {
            return "secret-value";
        }

        public String getAppKey()
        {
            return "secret-value";
        }

        public String getAppSecret()
        {
            return "secret-value";
        }

        public String getCredential()
        {
            return "secret-value";
        }

        public String getCredentialCiphertext()
        {
            return "secret-value";
        }

        public String getAppKeyCiphertext()
        {
            return "secret-value";
        }

        public String getAppSecretCiphertext()
        {
            return "secret-value";
        }

        public String getReason()
        {
            return "safe-value";
        }
    }
}
