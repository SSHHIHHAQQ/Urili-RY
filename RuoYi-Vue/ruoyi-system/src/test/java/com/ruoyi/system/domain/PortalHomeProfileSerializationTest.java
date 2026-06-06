package com.ruoyi.system.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PortalHomeProfileSerializationTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void permissionInfoMustNotSerializeInternalIdentityScope() throws Exception
    {
        PortalPermissionInfo info = new PortalPermissionInfo();
        info.setTerminal("seller");
        info.setSubjectId(9L);
        info.setAccountId(8L);
        info.setSubjectNo("SAA010001");
        info.setUserName("seller-owner");
        info.setPermissions(Collections.singleton("seller:account:list"));

        String json = mapper.writeValueAsString(info);

        assertInternalScopeHidden(json);
        assertTrue(json.contains("\"subjectNo\":\"SAA010001\""));
        assertTrue(json.contains("\"userName\":\"seller-owner\""));
        assertTrue(json.contains("seller:account:list"));
    }

    @Test
    public void subjectProfileMustNotSerializeInternalIdentityScope() throws Exception
    {
        PortalSubjectProfile profile = new PortalSubjectProfile();
        profile.setTerminal("buyer");
        profile.setSubjectId(19L);
        profile.setSubjectNo("BAA010001");
        profile.setSubjectName("Buyer Co");

        String json = mapper.writeValueAsString(profile);

        assertInternalScopeHidden(json);
        assertTrue(json.contains("\"subjectNo\":\"BAA010001\""));
        assertTrue(json.contains("\"subjectName\":\"Buyer Co\""));
    }

    @Test
    public void accountProfileMustNotSerializeInternalIdentityScope() throws Exception
    {
        PortalAccountProfile profile = new PortalAccountProfile();
        profile.setTerminal("seller");
        profile.setSubjectId(9L);
        profile.setAccountId(8L);
        profile.setUserName("seller-owner");

        String json = mapper.writeValueAsString(profile);

        assertInternalScopeHidden(json);
        assertTrue(json.contains("\"userName\":\"seller-owner\""));
    }

    @Test
    public void deptAndRoleProfilesMustNotSerializeInternalIdentityScope() throws Exception
    {
        PortalDeptProfile dept = new PortalDeptProfile();
        dept.setTerminal("seller");
        dept.setSubjectId(9L);
        dept.setDeptId(3L);
        dept.setDeptName("Operations");

        PortalRoleProfile role = new PortalRoleProfile();
        role.setTerminal("seller");
        role.setSubjectId(9L);
        role.setRoleId(4L);
        role.setRoleName("Owner");

        String deptJson = mapper.writeValueAsString(dept);
        String roleJson = mapper.writeValueAsString(role);

        assertInternalScopeHidden(deptJson);
        assertInternalScopeHidden(roleJson);
        assertTrue(deptJson.contains("\"deptName\":\"Operations\""));
        assertTrue(roleJson.contains("\"roleName\":\"Owner\""));
    }

    @Test
    public void ownSessionProfileMustNotSerializeInternalIdentityScope() throws Exception
    {
        PortalOwnSessionProfile profile = new PortalOwnSessionProfile();
        profile.setUserName("seller-owner");
        profile.setLoginIp("127.0.0.1");
        profile.setStatus("0");
        profile.setCurrent(Boolean.TRUE);

        String json = mapper.writeValueAsString(profile);

        assertInternalScopeHidden(json);
        assertFalse(json.contains("tokenId"));
        assertTrue(json.contains("\"userName\":\"seller-owner\""));
        assertTrue(json.contains("\"current\":true"));
    }

    private void assertInternalScopeHidden(String json)
    {
        assertFalse(json.contains("\"terminal\""));
        assertFalse(json.contains("\"subjectId\""));
        assertFalse(json.contains("\"accountId\""));
    }
}
