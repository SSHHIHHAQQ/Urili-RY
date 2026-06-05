package com.ruoyi.system.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalDirectLoginTicketTest
{
    @Test
    public void tokenHashMustNotBeSerializedToAdminAuditResponses() throws Exception
    {
        PortalDirectLoginTicket ticket = new PortalDirectLoginTicket();
        ticket.setTicketId(10L);
        ticket.setTerminal("seller");
        ticket.setTargetSubjectNo("SAAA010001");
        ticket.setTargetUserName("seller-owner");
        ticket.setTokenHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

        String json = new ObjectMapper().writeValueAsString(ticket);

        assertTrue(json.contains("\"ticketId\":10"));
        assertTrue(json.contains("\"terminal\":\"seller\""));
        assertFalse(json.contains("tokenHash"));
        assertFalse(json.contains("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
    }
}
