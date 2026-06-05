package com.ruoyi.framework.web.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;

public class PermissionServiceAccountPermissionTest
{
    private PermissionService permissionService;

    @Before
    public void setUp()
    {
        permissionService = new PermissionService();
        setRequestContext();
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void sellerAccountPermissionsRejectSubjectAndRoleOnlyAdminPermissions()
    {
        setAdminPermissions(
                "seller:admin:query",
                "seller:admin:add",
                "seller:admin:edit",
                "seller:admin:resetPwd",
                "seller:admin:role:query",
                "seller:admin:role:edit");

        assertFalse(permissionService.hasPermi("seller:admin:account:list"));
        assertFalse(permissionService.hasPermi("seller:admin:account:add"));
        assertFalse(permissionService.hasPermi("seller:admin:account:edit"));
        assertFalse(permissionService.hasPermi("seller:admin:account:resetPwd"));
        assertFalse(permissionService.hasPermi("seller:admin:account:lock"));
        assertFalse(permissionService.hasPermi("seller:admin:account:role:query"));
        assertFalse(permissionService.hasPermi("seller:admin:account:role:edit"));
        assertFalse(permissionService.hasPermi("seller:admin:forceLogout"));
        assertFalse(permissionService.hasPermi("seller:admin:directLogin"));
        assertFalse(permissionService.hasPermi("seller:admin:ticket:list"));
    }

    @Test
    public void buyerAccountPermissionsRejectSubjectAndRoleOnlyAdminPermissions()
    {
        setAdminPermissions(
                "buyer:admin:query",
                "buyer:admin:add",
                "buyer:admin:edit",
                "buyer:admin:resetPwd",
                "buyer:admin:role:query",
                "buyer:admin:role:edit");

        assertFalse(permissionService.hasPermi("buyer:admin:account:list"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:add"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:edit"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:resetPwd"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:lock"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:role:query"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:role:edit"));
        assertFalse(permissionService.hasPermi("buyer:admin:forceLogout"));
        assertFalse(permissionService.hasPermi("buyer:admin:directLogin"));
        assertFalse(permissionService.hasPermi("buyer:admin:ticket:list"));
    }

    @Test
    public void exactAccountPermissionsAllowOnlyTheirOwnAccountActions()
    {
        setAdminPermissions(
                "seller:admin:account:list",
                "seller:admin:account:add",
                "seller:admin:account:edit",
                "seller:admin:account:resetPwd",
                "seller:admin:account:lock",
                "seller:admin:account:role:query",
                "seller:admin:account:role:edit");

        assertTrue(permissionService.hasPermi("seller:admin:account:list"));
        assertTrue(permissionService.hasPermi("seller:admin:account:add"));
        assertTrue(permissionService.hasPermi("seller:admin:account:edit"));
        assertTrue(permissionService.hasPermi("seller:admin:account:resetPwd"));
        assertTrue(permissionService.hasPermi("seller:admin:account:lock"));
        assertTrue(permissionService.hasPermi("seller:admin:account:role:query"));
        assertTrue(permissionService.hasPermi("seller:admin:account:role:edit"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:list"));
        assertFalse(permissionService.hasPermi("buyer:admin:account:lock"));
        assertFalse(permissionService.hasPermi("seller:admin:forceLogout"));
        assertFalse(permissionService.hasPermi("buyer:admin:forceLogout"));
        assertFalse(permissionService.hasPermi("seller:admin:directLogin"));
        assertFalse(permissionService.hasPermi("seller:admin:ticket:list"));
        assertFalse(permissionService.hasPermi("buyer:admin:directLogin"));
        assertFalse(permissionService.hasPermi("buyer:admin:ticket:list"));
    }

    @Test
    public void exactForceLogoutPermissionsAllowOnlyTheirOwnTerminalSessions()
    {
        setAdminPermissions("seller:admin:forceLogout");

        assertTrue(permissionService.hasPermi("seller:admin:forceLogout"));
        assertFalse(permissionService.hasPermi("buyer:admin:forceLogout"));
        assertFalse(permissionService.hasPermi("seller:admin:account:list"));
        assertFalse(permissionService.hasPermi("seller:admin:account:lock"));
        assertFalse(permissionService.hasPermi("seller:admin:directLogin"));
        assertFalse(permissionService.hasPermi("seller:admin:ticket:list"));
    }

    @Test
    public void exactDirectLoginPermissionsAllowOnlyTheirOwnTerminalDirectLoginAndTicketAudit()
    {
        setAdminPermissions("seller:admin:directLogin", "seller:admin:ticket:list");

        assertTrue(permissionService.hasPermi("seller:admin:directLogin"));
        assertTrue(permissionService.hasPermi("seller:admin:ticket:list"));
        assertFalse(permissionService.hasPermi("buyer:admin:directLogin"));
        assertFalse(permissionService.hasPermi("buyer:admin:ticket:list"));
        assertFalse(permissionService.hasPermi("seller:admin:account:resetPwd"));
        assertFalse(permissionService.hasPermi("seller:admin:account:lock"));
        assertFalse(permissionService.hasPermi("seller:admin:forceLogout"));
    }

    @Test
    public void accountAndSessionPermissionsDoNotGrantDirectLogin()
    {
        setAdminPermissions(
                "seller:admin:account:resetPwd",
                "seller:admin:account:lock",
                "seller:admin:forceLogout");

        assertTrue(permissionService.hasPermi("seller:admin:account:resetPwd"));
        assertTrue(permissionService.hasPermi("seller:admin:account:lock"));
        assertTrue(permissionService.hasPermi("seller:admin:forceLogout"));
        assertFalse(permissionService.hasPermi("seller:admin:directLogin"));
        assertFalse(permissionService.hasPermi("seller:admin:ticket:list"));
        assertFalse(permissionService.hasPermi("buyer:admin:directLogin"));
        assertFalse(permissionService.hasPermi("buyer:admin:ticket:list"));
    }

    @Test
    public void superAdminWildcardStillAllowsAccountActions()
    {
        setAdminPermissions(Constants.ALL_PERMISSION);

        assertTrue(permissionService.hasPermi("seller:admin:account:list"));
        assertTrue(permissionService.hasPermi("seller:admin:account:lock"));
        assertTrue(permissionService.hasPermi("seller:admin:account:role:edit"));
        assertTrue(permissionService.hasPermi("seller:admin:forceLogout"));
        assertTrue(permissionService.hasPermi("seller:admin:directLogin"));
        assertTrue(permissionService.hasPermi("seller:admin:ticket:list"));
        assertTrue(permissionService.hasPermi("buyer:admin:account:list"));
        assertTrue(permissionService.hasPermi("buyer:admin:account:lock"));
        assertTrue(permissionService.hasPermi("buyer:admin:account:role:edit"));
        assertTrue(permissionService.hasPermi("buyer:admin:forceLogout"));
        assertTrue(permissionService.hasPermi("buyer:admin:directLogin"));
        assertTrue(permissionService.hasPermi("buyer:admin:ticket:list"));
    }

    private void setAdminPermissions(String... permissions)
    {
        SysUser user = new SysUser();
        user.setUserId(9L);
        user.setDeptId(3L);
        user.setUserName("limited-admin");
        Set<String> permissionSet = new LinkedHashSet<>(Arrays.asList(permissions));
        LoginUser loginUser = new LoginUser(9L, 3L, user, permissionSet);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private void setRequestContext()
    {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null && args.length == 1)
                    {
                        return null;
                    }
                    if ("getAttributeNames".equals(method.getName()))
                    {
                        return Collections.emptyEnumeration();
                    }
                    if ("getParameterMap".equals(method.getName()))
                    {
                        return Collections.emptyMap();
                    }
                    if ("getRemoteAddr".equals(method.getName()))
                    {
                        return "127.0.0.1";
                    }
                    return defaultValue(method.getReturnType());
                });
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private Object defaultValue(Class<?> returnType)
    {
        if (returnType == Boolean.TYPE)
        {
            return false;
        }
        if (returnType == Byte.TYPE)
        {
            return (byte) 0;
        }
        if (returnType == Short.TYPE)
        {
            return (short) 0;
        }
        if (returnType == Integer.TYPE)
        {
            return 0;
        }
        if (returnType == Long.TYPE)
        {
            return 0L;
        }
        if (returnType == Float.TYPE)
        {
            return 0F;
        }
        if (returnType == Double.TYPE)
        {
            return 0D;
        }
        if (returnType == Character.TYPE)
        {
            return '\0';
        }
        return null;
    }
}
