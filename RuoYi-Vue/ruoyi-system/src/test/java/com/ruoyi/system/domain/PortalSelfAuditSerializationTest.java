package com.ruoyi.system.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalSelfAuditSerializationTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void ownLoginLogMustNotSerializeInternalAuditScope() throws Exception
    {
        PortalOwnLoginLogProfile profile = new PortalOwnLoginLogProfile();
        profile.setUserName("seller-owner");
        profile.setIpaddr("127.0.0.1");
        profile.setStatus("0");
        profile.setMsg("direct login success");

        String json = mapper.writeValueAsString(profile);

        assertInternalAuditScopeHidden(json);
        assertFieldAbsent(json, "directLogin");
        assertTrue(json.contains("\"userName\":\"seller-owner\""));
        assertTrue(json.contains("\"msg\":\"direct login success\""));
    }

    @Test
    public void ownOperLogMustNotSerializeInternalAuditScope() throws Exception
    {
        PortalOwnOperLogProfile profile = new PortalOwnOperLogProfile();
        profile.setTitle("Seller portal menu");
        profile.setRequestMethod("GET");
        profile.setOperName("seller-owner");
        profile.setOperUrl("/seller/getRouters");
        profile.setOperIp("127.0.0.1");
        profile.setStatus(0);

        String json = mapper.writeValueAsString(profile);

        assertInternalAuditScopeHidden(json);
        assertFieldAbsent(json, "directLogin");
        assertFieldAbsent(json, "operParam");
        assertFieldAbsent(json, "jsonResult");
        assertFieldAbsent(json, "method");
        assertTrue(json.contains("\"title\":\"Seller portal menu\""));
        assertTrue(json.contains("\"operUrl\":\"/seller/getRouters\""));
    }

    @Test
    public void ownSessionMustNotSerializeInternalAuditScope() throws Exception
    {
        PortalOwnSessionProfile profile = new PortalOwnSessionProfile();
        profile.setUserName("seller-owner");
        profile.setLoginIp("127.0.0.1");
        profile.setStatus("online");
        profile.setCurrent(Boolean.TRUE);

        String json = mapper.writeValueAsString(profile);

        assertInternalAuditScopeHidden(json);
        assertFieldAbsent(json, "directLogin");
        assertTrue(json.contains("\"userName\":\"seller-owner\""));
        assertTrue(json.contains("\"current\":true"));
    }

    private void assertInternalAuditScopeHidden(String json)
    {
        assertFieldAbsent(json, "terminal");
        assertFieldAbsent(json, "subjectId");
        assertFieldAbsent(json, "accountId");
        assertFieldAbsent(json, "tokenId");
        assertFieldAbsent(json, "directLoginTicketId");
        assertFieldAbsent(json, "actingAdminId");
        assertFieldAbsent(json, "actingAdminName");
        assertFieldAbsent(json, "directLoginReason");
    }

    private void assertFieldAbsent(String json, String fieldName)
    {
        assertFalse(json.contains("\"" + fieldName + "\""));
    }
}
