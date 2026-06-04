package com.ruoyi.buyer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalTokenSupport;

/**
 * Buyer terminal session endpoints.
 */
@Anonymous
@RestController
@RequestMapping("/buyer")
public class BuyerPortalController extends BaseController
{
    @Autowired
    private PortalTokenSupport portalTokenSupport;

    @Autowired
    private IBuyerPortalPermissionService permissionService;

    @GetMapping("/getInfo")
    public AjaxResult getInfo()
    {
        PortalLoginSession session = portalTokenSupport.getSession("buyer");
        if (session == null)
        {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }
        return success(permissionService.selectPortalPermissionInfo(session));
    }

    @GetMapping("/getRouters")
    public AjaxResult getRouters()
    {
        PortalLoginSession session = portalTokenSupport.getSession("buyer");
        if (session == null)
        {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }
        return success(permissionService.selectPortalMenuTree(session));
    }
}
