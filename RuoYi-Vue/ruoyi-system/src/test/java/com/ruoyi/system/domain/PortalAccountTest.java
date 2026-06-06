package com.ruoyi.system.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalAccountTest
{
    @Test
    public void passwordMustBeWriteOnlyForPortalAccountResponses() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        TestPortalAccount account = new TestPortalAccount();
        account.setAccountId(10L);
        account.setUserName("seller-owner");
        account.setPassword("PlainPassword123");

        String json = mapper.writeValueAsString(account);

        assertFalse(json.contains("\"password\""));
        assertFalse(json.contains("PlainPassword123"));

        TestPortalAccount input = mapper.readValue(
                "{\"accountId\":10,\"userName\":\"seller-owner\",\"password\":\"PlainPassword123\"}",
                TestPortalAccount.class);
        assertEquals("PlainPassword123", input.getPassword());
    }

    private static class TestPortalAccount extends PortalAccount
    {
        private static final long serialVersionUID = 1L;
    }
}
