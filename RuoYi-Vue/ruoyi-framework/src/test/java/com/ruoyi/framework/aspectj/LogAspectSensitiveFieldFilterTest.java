package com.ruoyi.framework.aspectj;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
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
                "token", "jwt", "directLoginToken", "loginUrl", "accessToken", "refreshToken", "authorization"))
        {
            assertTrue(Arrays.asList(LogAspect.EXCLUDE_PROPERTIES).contains(sensitiveField));
            assertTrue(Arrays.asList(PortalLogAspect.EXCLUDE_PROPERTIES).contains(sensitiveField));
        }
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

        public String getReason()
        {
            return "safe-value";
        }
    }
}
