package com.ruoyi.framework.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
public class PortalPreAuthorizeAspect
{
    private final PortalPermissionChecker portalPermissionChecker;

    public PortalPreAuthorizeAspect(PortalPermissionChecker portalPermissionChecker)
    {
        this.portalPermissionChecker = portalPermissionChecker;
    }

    @Around("@annotation(portalPreAuthorize)")
    public Object around(ProceedingJoinPoint joinPoint, PortalPreAuthorize portalPreAuthorize) throws Throwable
    {
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
}
