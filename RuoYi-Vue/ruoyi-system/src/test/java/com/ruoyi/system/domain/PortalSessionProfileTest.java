package com.ruoyi.system.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalSessionProfileTest
{
    @Test
    public void tokenIdMustSerializeForAdminSessionAuditRows() throws Exception
    {
        PortalSessionProfile profile = new PortalSessionProfile();
        profile.setTerminal("seller");
        profile.setSubjectId(9L);
        profile.setAccountId(8L);
        profile.setUserName("seller-owner");
        profile.setLoginIp("127.0.0.1");
        profile.setTokenId("portal-login-token-id-for-internal-use-only");
        profile.setStatus("0");
        profile.setCurrent(Boolean.TRUE);

        String json = new ObjectMapper().writeValueAsString(profile);

        assertTrue(json.contains("\"terminal\":\"seller\""));
        assertTrue(json.contains("\"subjectId\":9"));
        assertTrue(json.contains("\"accountId\":8"));
        assertTrue(json.contains("\"current\":true"));
        assertTrue(json.contains("\"tokenId\":\"portal-login-token-id-for-internal-use-only\""));
    }
}
