package com.ruoyi.system.service.support;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * Resolves the current operator for admin or seller/buyer portal writes.
 */
public final class PortalActorSupport
{
    private PortalActorSupport()
    {
    }

    public static String currentActorName()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (ServiceException e)
        {
            PortalLoginSession session = PortalSessionContext.getSession();
            if (session != null && StringUtils.isNotBlank(session.getUserName()))
            {
                return session.getUserName();
            }
            throw e;
        }
    }
}
