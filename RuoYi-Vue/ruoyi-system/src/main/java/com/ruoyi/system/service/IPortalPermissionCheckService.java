package com.ruoyi.system.service;

import java.util.Set;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * Terminal-specific permission provider for seller/buyer portals.
 */
public interface IPortalPermissionCheckService
{
    public String terminal();

    public Set<String> selectPermissions(PortalLoginSession session);
}
