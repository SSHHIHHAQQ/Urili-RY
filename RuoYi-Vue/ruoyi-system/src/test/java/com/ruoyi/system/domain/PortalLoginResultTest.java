package com.ruoyi.system.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalLoginResultTest
{
    @Test
    public void loginResponseMustNotSerializeInternalSubjectOrAccountIds() throws Exception
    {
        PortalLoginResult result = new PortalLoginResult();
        result.setToken("seller_portal_token");
        result.setTerminal("seller");
        result.setSubjectId(11L);
        result.setSubjectNo("SAA010001");
        result.setAccountId(22L);
        result.setUsername("seller-owner");
        result.setNickName("Seller Owner");
        result.setExpireMinutes(30);

        String json = new ObjectMapper().writeValueAsString(result);

        assertTrue(json.contains("\"token\":\"seller_portal_token\""));
        assertTrue(json.contains("\"terminal\":\"seller\""));
        assertTrue(json.contains("\"subjectNo\":\"SAA010001\""));
        assertTrue(json.contains("\"username\":\"seller-owner\""));
        assertFalse(json.contains("subjectId"));
        assertFalse(json.contains("accountId"));
    }
}
