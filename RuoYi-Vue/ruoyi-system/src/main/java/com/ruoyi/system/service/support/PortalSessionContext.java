package com.ruoyi.system.service.support;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * Request-local seller/buyer portal session context.
 */
public class PortalSessionContext
{
    private static final ThreadLocal<PortalLoginSession> SESSION_HOLDER = new ThreadLocal<>();

    private PortalSessionContext()
    {
    }

    public static void setSession(PortalLoginSession session)
    {
        SESSION_HOLDER.set(session);
    }

    public static PortalLoginSession getSession()
    {
        return SESSION_HOLDER.get();
    }

    public static PortalLoginSession getSession(String expectedTerminal)
    {
        PortalLoginSession session = getSession();
        if (session == null || !StringUtils.equals(expectedTerminal, session.getTerminal()))
        {
            return null;
        }
        return session;
    }

    public static PortalLoginSession requireSession(String expectedTerminal)
    {
        PortalLoginSession session = getSession(expectedTerminal);
        if (session == null)
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        return session;
    }

    public static void clear()
    {
        SESSION_HOLDER.remove();
    }
}
