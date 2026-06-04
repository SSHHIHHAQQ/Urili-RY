package com.ruoyi.system.service.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.IPortalPermissionCheckService;

public class PortalPermissionCheckerTest
{
    @Test
    public void requireAuthorizedAllowsAuthenticatedSessionWhenNoPermissionIsRequired()
    {
        PortalLoginSession session = session("seller");
        PortalPermissionChecker checker = checker(session, service("seller"));

        PortalLoginSession result = checker.requireAuthorized("seller", new String[0], new String[0]);

        assertSame(session, result);
    }

    @Test
    public void requireAuthorizedRequiresAllAndAnyPermissionsWithinTerminal()
    {
        PortalLoginSession session = session("seller");
        PortalPermissionChecker checker = checker(session,
                service("seller", "seller:order:list", "seller:order:edit"));

        PortalLoginSession result = checker.requireAuthorized("seller",
                new String[] { "seller:order:list" },
                new String[] { "seller:order:delete", "seller:order:edit" });

        assertSame(session, result);
    }

    @Test
    public void requireAuthorizedRejectsMissingRequiredPermission()
    {
        PortalLoginSession session = session("seller");
        PortalPermissionChecker checker = checker(session, service("seller", "seller:order:list"));

        try
        {
            checker.requireAuthorized("seller", new String[] { "seller:order:edit" }, new String[0]);
        }
        catch (ServiceException e)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), e.getCode());
            assertEquals("没有操作权限", e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void requireAuthorizedRejectsMissingAnyPermission()
    {
        PortalLoginSession session = session("buyer");
        PortalPermissionChecker checker = checker(session, service("buyer", "buyer:order:list"));

        try
        {
            checker.requireAuthorized("buyer", new String[0],
                    new String[] { "buyer:order:edit", "buyer:order:delete" });
        }
        catch (ServiceException e)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), e.getCode());
            assertEquals("没有操作权限", e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void requireAuthorizedRejectsUnregisteredTerminalService()
    {
        PortalLoginSession session = session("buyer");
        PortalPermissionChecker checker = checker(session, service("seller", "seller:order:list"));

        try
        {
            checker.requireAuthorized("buyer", new String[0], new String[0]);
        }
        catch (ServiceException e)
        {
            assertEquals("端内权限服务未配置", e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    private PortalPermissionChecker checker(PortalLoginSession session, IPortalPermissionCheckService... services)
    {
        return new PortalPermissionChecker(new FakePortalTokenSupport(session), Arrays.asList(services));
    }

    private PortalLoginSession session(String terminal)
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal(terminal);
        session.setSubjectId(1L);
        session.setAccountId(2L);
        return session;
    }

    private IPortalPermissionCheckService service(String terminal, String... permissions)
    {
        Set<String> permissionSet = new LinkedHashSet<>(Arrays.asList(permissions));
        return new IPortalPermissionCheckService()
        {
            @Override
            public String terminal()
            {
                return terminal;
            }

            @Override
            public Set<String> selectPermissions(PortalLoginSession session)
            {
                return permissionSet;
            }
        };
    }

    private static class FakePortalTokenSupport extends PortalTokenSupport
    {
        private final PortalLoginSession session;

        private FakePortalTokenSupport(PortalLoginSession session)
        {
            this.session = session;
        }

        @Override
        public PortalLoginSession requireSession(String expectedTerminal)
        {
            if (session == null || !expectedTerminal.equals(session.getTerminal()))
            {
                throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
            }
            return session;
        }
    }
}
