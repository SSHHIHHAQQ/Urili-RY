package com.ruoyi.system.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalDirectLoginResultTest
{
    @Test
    public void tokenMustBeSerializedSeparatelyFromCleanLoginUrlWithoutTargetAccount() throws Exception
    {
        PortalDirectLoginResult result = new PortalDirectLoginResult();
        result.setToken("seller_plain_direct_login_token");
        result.setTicketId(10L);
        result.setLoginUrl("https://seller.example/direct-login");
        result.setExpireMinutes(30);
        result.setAccountId(20L);
        result.setUsername("seller-owner");

        String json = new ObjectMapper().writeValueAsString(result);

        assertTrue(json.contains("\"ticketId\":10"));
        assertTrue(json.contains("\"loginUrl\""));
        assertTrue(json.contains("\"expireMinutes\":30"));
        assertTrue(json.contains("\"token\":\"seller_plain_direct_login_token\""));
        assertFalse(json.contains("\"accountId\""));
        assertFalse(json.contains("\"username\""));
        assertFalse(json.contains("directLoginToken="));
    }
}
