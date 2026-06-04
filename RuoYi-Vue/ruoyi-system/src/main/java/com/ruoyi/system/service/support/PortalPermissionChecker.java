package com.ruoyi.system.service.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.IPortalPermissionCheckService;

/**
 * Shared seller/buyer portal permission checker.
 */
@Component
public class PortalPermissionChecker
{
    private final PortalTokenSupport portalTokenSupport;

    private final Map<String, IPortalPermissionCheckService> checkServices = new HashMap<>();

    @Autowired
    public PortalPermissionChecker(PortalTokenSupport portalTokenSupport, List<IPortalPermissionCheckService> services)
    {
        this.portalTokenSupport = portalTokenSupport;
        if (services != null)
        {
            for (IPortalPermissionCheckService service : services)
            {
                if (StringUtils.isNotEmpty(service.terminal()))
                {
                    checkServices.put(service.terminal(), service);
                }
            }
        }
    }

    public PortalLoginSession requireAuthorized(String terminal, String[] requiredPermissions, String[] anyPermissions)
    {
        PortalLoginSession session = portalTokenSupport.requireSession(terminal);
        IPortalPermissionCheckService service = checkServices.get(terminal);
        if (service == null)
        {
            throw new ServiceException("端内权限服务未配置");
        }

        Set<String> permissions = service.selectPermissions(session);
        if ((requiredPermissions == null || requiredPermissions.length == 0)
                && (anyPermissions == null || anyPermissions.length == 0))
        {
            return session;
        }

        if (!PortalPermissionSupport.hasAllPermissions(permissions, requiredPermissions))
        {
            throw new ServiceException("没有操作权限", HttpStatus.FORBIDDEN);
        }
        if (!PortalPermissionSupport.hasAnyPermission(permissions, anyPermissions))
        {
            throw new ServiceException("没有操作权限", HttpStatus.FORBIDDEN);
        }
        return session;
    }
}
