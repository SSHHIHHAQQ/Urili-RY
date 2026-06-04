package com.ruoyi.buyer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Buyer terminal session endpoints.
 */
@Anonymous
@RestController
@RequestMapping("/buyer")
public class BuyerPortalController extends BaseController
{
    @Autowired
    private IBuyerPortalPermissionService permissionService;

    @GetMapping("/getInfo")
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端用户信息", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getInfo()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(permissionService.selectPortalPermissionInfo(session));
    }

    @GetMapping("/getRouters")
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端菜单", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getRouters()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(permissionService.selectPortalMenuTree(session));
    }
}
