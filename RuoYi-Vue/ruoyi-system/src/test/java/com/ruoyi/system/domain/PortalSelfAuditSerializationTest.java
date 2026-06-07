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
        profile.setMsg("免密登录成功");

        String json = mapper.writeValueAsString(profile);

        assertInternalAuditScopeHidden(json);
        assertFalse(json.contains("directLogin"));
        assertTrue(json.contains("\"userName\":\"seller-owner\""));
        assertTrue(json.contains("\"msg\":\"免密登录成功\""));
    }

    @Test
    public void ownOperLogMustNotSerializeInternalAuditScope() throws Exception
    {
        PortalOwnOperLogProfile profile = new PortalOwnOperLogProfile();
        profile.setTitle("卖家端菜单");
        profile.setRequestMethod("GET");
        profile.setOperName("seller-owner");
        profile.setOperUrl("/seller/getRouters");
        profile.setOperIp("127.0.0.1");
        profile.setStatus(0);

        String json = mapper.writeValueAsString(profile);

        assertInternalAuditScopeHidden(json);
        assertFalse(json.contains("directLogin"));
        assertFalse(json.contains("operParam"));
        assertFalse(json.contains("jsonResult"));
        assertFalse(json.contains("method"));
        assertTrue(json.contains("\"title\":\"卖家端菜单\""));
        assertTrue(json.contains("\"operUrl\":\"/seller/getRouters\""));
    }

    private void assertInternalAuditScopeHidden(String json)
    {
        assertFalse(json.contains("subjectId"));
        assertFalse(json.contains("accountId"));
        assertFalse(json.contains("tokenId"));
        assertFalse(json.contains("directLoginTicketId"));
        assertFalse(json.contains("actingAdminId"));
        assertFalse(json.contains("actingAdminName"));
        assertFalse(json.contains("directLoginReason"));
    }
}
