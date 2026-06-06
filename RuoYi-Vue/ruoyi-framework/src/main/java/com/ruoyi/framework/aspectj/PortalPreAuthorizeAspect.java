package com.ruoyi.framework.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.enums.BusinessStatus;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.service.support.PortalPermissionChecker;
import com.ruoyi.system.service.support.PortalSessionContext;
import com.ruoyi.system.service.support.PortalTokenSupport;

/**
 * Seller/buyer portal permission guard aspect.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortalPreAuthorizeAspect
{
    private static final int PARAM_MAX_LENGTH = 2000;

    private final PortalPermissionChecker portalPermissionChecker;

    private final PortalTokenSupport portalTokenSupport;

    public PortalPreAuthorizeAspect(PortalPermissionChecker portalPermissionChecker, PortalTokenSupport portalTokenSupport)
    {
        this.portalPermissionChecker = portalPermissionChecker;
        this.portalTokenSupport = portalTokenSupport;
    }

    @Around("@annotation(com.ruoyi.common.annotation.PortalPreAuthorize)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        PortalPreAuthorize portalPreAuthorize = getPortalPreAuthorize(joinPoint);
        PortalLoginSession previousSession = PortalSessionContext.getSession();
        PortalLoginSession session;
        try
        {
            session = portalPermissionChecker.requireAuthorized(
                    portalPreAuthorize.terminal(),
                    portalPreAuthorize.hasPermi(),
                    portalPreAuthorize.hasAnyPermi());
        }
        catch (ServiceException e)
        {
            recordAuthorizationFailure(joinPoint, portalPreAuthorize, e);
            throw e;
        }
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

    private void recordAuthorizationFailure(ProceedingJoinPoint joinPoint, PortalPreAuthorize portalPreAuthorize,
            ServiceException exception)
    {
        try
        {
            PortalLoginSession session = portalTokenSupport.getSession(portalPreAuthorize.terminal());
            PortalOperLog operLog = new PortalOperLog();
            if (session != null)
            {
                operLog.setSubjectId(session.getSubjectId());
                operLog.setAccountId(session.getAccountId());
                operLog.setOperName(session.getUserName());
                appendDirectLoginAudit(operLog, session);
            }
            else
            {
                operLog.setOperName("anonymous");
            }

            operLog.setTitle("Portal access denied");
            operLog.setBusinessType(BusinessType.OTHER.ordinal());
            operLog.setStatus(BusinessStatus.FAIL.ordinal());
            operLog.setOperIp(IpUtils.getIpAddr());
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(), 0, 255));
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            operLog.setMethod(joinPoint.getTarget().getClass().getName() + "."
                    + joinPoint.getSignature().getName() + "()");
            operLog.setErrorMsg(StringUtils.substring(resolveFailureMessage(exception), 0, 2000));
            operLog.setCostTime(0L);
            AsyncManager.me().execute(AsyncFactory.recordPortalOper(portalPreAuthorize.terminal(), operLog));
        }
        catch (Exception ignored)
        {
            // Authorization failures must never be hidden by audit logging.
        }
    }

    private String resolveFailureMessage(ServiceException exception)
    {
        if (exception == null || StringUtils.isEmpty(exception.getMessage()))
        {
            return "Portal authorization failed";
        }
        Integer code = exception.getCode();
        if (code == null)
        {
            return exception.getMessage();
        }
        return code + ": " + exception.getMessage();
    }

    private void appendDirectLoginAudit(PortalOperLog operLog, PortalLoginSession session)
    {
        if (session == null || !Boolean.TRUE.equals(session.getDirectLogin()))
        {
            return;
        }
        String auditPrefix = "directLoginAudit{ticketId=" + session.getDirectLoginTicketId()
                + ", actingAdminId=" + session.getActingAdminId()
                + ", actingAdminName=" + safeAuditValue(session.getActingAdminName())
                + ", reason=" + safeAuditValue(session.getDirectLoginReason()) + "} ";
        operLog.setOperParam(StringUtils.substring(auditPrefix + safeAuditValue(operLog.getOperParam()), 0,
                PARAM_MAX_LENGTH));
    }

    private String safeAuditValue(String value)
    {
        return StringUtils.isBlank(value) ? "" : value;
    }
}
