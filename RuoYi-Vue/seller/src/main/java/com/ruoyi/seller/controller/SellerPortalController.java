package com.ruoyi.seller.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalTokenSupport;

/**
 * Seller terminal session endpoints.
 */
@Anonymous
@RestController
@RequestMapping("/seller")
public class SellerPortalController extends BaseController
{
    @Autowired
    private PortalTokenSupport portalTokenSupport;

    @Autowired
    private ISellerPortalPermissionService permissionService;

    @GetMapping("/getInfo")
    public AjaxResult getInfo()
    {
        PortalLoginSession session = portalTokenSupport.getSession("seller");
        if (session == null)
        {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }
        return success(permissionService.selectPortalPermissionInfo(session));
    }

    @GetMapping("/getRouters")
    public AjaxResult getRouters()
    {
        PortalLoginSession session = portalTokenSupport.getSession("seller");
        if (session == null)
        {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }
        return success(permissionService.selectPortalMenuTree(session));
    }
}
