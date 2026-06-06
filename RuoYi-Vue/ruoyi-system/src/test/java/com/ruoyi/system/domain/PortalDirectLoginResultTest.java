package com.ruoyi.system.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalDirectLoginResultTest
{
    @Test
    public void tokenMustNotBeSerializedToAdminDirectLoginResponse() throws Exception
    {
        PortalDirectLoginResult result = new PortalDirectLoginResult();
        result.setToken("seller_plain_direct_login_token");
        result.setTicketId(10L);
        result.setLoginUrl("http://127.0.0.1:8001/seller/direct-login?directLoginToken=seller_plain_direct_login_token");
        result.setExpireMinutes(30);
        result.setAccountId(20L);
        result.setUsername("seller-owner");

        String json = new ObjectMapper().writeValueAsString(result);

        assertTrue(json.contains("\"ticketId\":10"));
        assertTrue(json.contains("\"loginUrl\""));
        assertTrue(json.contains("\"expireMinutes\":30"));
        assertFalse(json.contains("\"token\""));
    }
}
