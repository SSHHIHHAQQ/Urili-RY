package com.ruoyi.framework.aspectj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.After;
import org.junit.Test;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.IPortalPermissionCheckService;
import com.ruoyi.system.service.support.PortalPermissionChecker;
import com.ruoyi.system.service.support.PortalSessionContext;
import com.ruoyi.system.service.support.PortalTokenSupport;

public class PortalPreAuthorizeAspectTest
{
    @After
    public void clearSession()
    {
        PortalSessionContext.clear();
    }

    @Test
    public void aroundSetsAuthorizedPortalSessionDuringProceedAndClearsAfterward() throws Throwable
    {
        PortalLoginSession session = session("seller");
        PortalPreAuthorizeAspect aspect = aspect(session, service("seller", "seller:account:list"));
        AtomicInteger proceedCount = new AtomicInteger();

        Object result = aspect.around(joinPoint(TargetHandlers.class.getMethod("sellerAccountList"), () -> {
            proceedCount.incrementAndGet();
            assertSame(session, PortalSessionContext.getSession());
            return "ok";
        }));

        assertEquals("ok", result);
        assertEquals(1, proceedCount.get());
        assertNull(PortalSessionContext.getSession());
    }

    @Test
    public void aroundRestoresPreviousPortalSessionAfterProceed() throws Throwable
    {
        PortalLoginSession previous = session("buyer");
        PortalSessionContext.setSession(previous);
        PortalLoginSession current = session("seller");
        PortalPreAuthorizeAspect aspect = aspect(current, service("seller", "seller:account:list"));

        Object result = aspect.around(joinPoint(TargetHandlers.class.getMethod("sellerAccountList"), () -> {
            assertSame(current, PortalSessionContext.getSession());
            return "ok";
        }));

        assertEquals("ok", result);
        assertSame(previous, PortalSessionContext.getSession());
    }

    @Test
    public void aroundRejectsMissingPermissionWithoutProceeding() throws Throwable
    {
        PortalLoginSession session = session("seller");
        PortalPreAuthorizeAspect aspect = aspect(session, service("seller", "seller:account:list"));
        AtomicInteger proceedCount = new AtomicInteger();

        try
        {
            aspect.around(joinPoint(TargetHandlers.class.getMethod("sellerAccountEdit"), () -> {
                proceedCount.incrementAndGet();
                return "should-not-run";
            }));
        }
        catch (ServiceException e)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), e.getCode());
            assertEquals("没有操作权限", e.getMessage());
            assertEquals(0, proceedCount.get());
            assertNull(PortalSessionContext.getSession());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    private PortalPreAuthorizeAspect aspect(PortalLoginSession session, IPortalPermissionCheckService service)
    {
        PortalPermissionChecker checker = new PortalPermissionChecker(new FakePortalTokenSupport(session),
                Arrays.asList(service));
        return new PortalPreAuthorizeAspect(checker);
    }

    private ProceedingJoinPoint joinPoint(Method method, ProceedAction action)
    {
        TargetHandlers target = new TargetHandlers();
        MethodSignature signature = (MethodSignature) Proxy.newProxyInstance(
                MethodSignature.class.getClassLoader(),
                new Class<?>[] { MethodSignature.class },
                (proxy, invokedMethod, args) -> signatureValue(method, invokedMethod));
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                ProceedingJoinPoint.class.getClassLoader(),
                new Class<?>[] { ProceedingJoinPoint.class },
                (proxy, invokedMethod, args) -> joinPointValue(target, signature, action, invokedMethod));
    }

    private Object signatureValue(Method method, Method invokedMethod)
    {
        switch (invokedMethod.getName())
        {
            case "getMethod":
                return method;
            case "getName":
                return method.getName();
            case "getParameterTypes":
                return method.getParameterTypes();
            case "toString":
                return method.toString();
            default:
                return defaultValue(invokedMethod.getReturnType());
        }
    }

    private Object joinPointValue(TargetHandlers target, MethodSignature signature, ProceedAction action,
            Method invokedMethod) throws Throwable
    {
        switch (invokedMethod.getName())
        {
            case "proceed":
                return action.proceed();
            case "getSignature":
                return signature;
            case "getTarget":
            case "getThis":
                return target;
            case "getArgs":
                return new Object[0];
            case "toString":
                return "PortalPreAuthorizeAspectTestJoinPoint";
            default:
                return defaultValue(invokedMethod.getReturnType());
        }
    }

    private Object defaultValue(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (boolean.class.equals(returnType))
        {
            return false;
        }
        if (char.class.equals(returnType))
        {
            return '\0';
        }
        return 0;
    }

    private PortalLoginSession session(String terminal)
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal(terminal);
        session.setSubjectId(1L);
        session.setAccountId(2L);
        session.setTokenId(terminal + "_token");
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

    private interface ProceedAction
    {
        Object proceed() throws Throwable;
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

    private static class TargetHandlers
    {
        @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:list")
        public String sellerAccountList()
        {
            return "unused";
        }

        @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:edit")
        public String sellerAccountEdit()
        {
            return "unused";
        }
    }
}
