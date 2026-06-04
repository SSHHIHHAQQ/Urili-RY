package com.ruoyi.framework.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalPermissionChecker;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Seller/buyer portal permission guard aspect.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortalPreAuthorizeAspect
{
    private final PortalPermissionChecker portalPermissionChecker;

    public PortalPreAuthorizeAspect(PortalPermissionChecker portalPermissionChecker)
    {
        this.portalPermissionChecker = portalPermissionChecker;
    }

    @Around("@annotation(com.ruoyi.common.annotation.PortalPreAuthorize)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        PortalPreAuthorize portalPreAuthorize = getPortalPreAuthorize(joinPoint);
        PortalLoginSession previousSession = PortalSessionContext.getSession();
        PortalLoginSession session = portalPermissionChecker.requireAuthorized(
                portalPreAuthorize.terminal(),
                portalPreAuthorize.hasPermi(),
                portalPreAuthorize.hasAnyPermi());
        PortalSessionContext.setSession(session);
        try
        {
            return joinPoint.proceed();
        }
        finally
        {
            if (previousSession == null)
            {
                PortalSessionContext.clear();
            }
            else
            {
                PortalSessionContext.setSession(previousSession);
            }
        }
    }

    private PortalPreAuthorize getPortalPreAuthorize(ProceedingJoinPoint joinPoint) throws NoSuchMethodException
    {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        PortalPreAuthorize annotation = signature.getMethod().getAnnotation(PortalPreAuthorize.class);
        if (annotation != null)
        {
            return annotation;
        }
        return joinPoint.getTarget().getClass()
                .getMethod(signature.getName(), signature.getParameterTypes())
                .getAnnotation(PortalPreAuthorize.class);
    }
}
