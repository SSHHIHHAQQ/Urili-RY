package com.ruoyi.seller.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Seller terminal session endpoints.
 */
@Anonymous
@RestController
@RequestMapping("/seller")
public class SellerPortalController extends BaseController
{
    @Autowired
    private ISellerPortalPermissionService permissionService;

    @GetMapping("/getInfo")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端用户信息", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getInfo()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(permissionService.selectPortalPermissionInfo(session));
    }

    @GetMapping("/getRouters")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端菜单", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getRouters()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(permissionService.selectPortalMenuTree(session));
    }
}
