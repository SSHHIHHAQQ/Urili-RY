package com.ruoyi.seller.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.seller.service.ISellerService;

/**
 * Seller portal authentication endpoints.
 */
@RestController
@RequestMapping("/seller")
public class SellerPortalAuthController extends BaseController
{
    @Autowired
    private ISellerService sellerService;

    @PortalLog(terminal = "seller", title = "卖家端登录", isSaveResponseData = false, allowAnonymous = true,
            excludeParamNames = { "password", "code", "uuid" })
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        return success(sellerService.loginSeller(loginBody));
    }

    @PortalLog(terminal = "seller", title = "卖家端免密登录", isSaveResponseData = false, allowAnonymous = true,
            excludeParamNames = { "directLoginToken" })
    @PostMapping("/direct-login")
    public AjaxResult directLogin(@RequestBody(required = false) Map<String, String> body)
    {
        return success(sellerService.directLoginSeller(resolveDirectLoginToken(body)));
    }

    private String resolveDirectLoginToken(Map<String, String> body)
    {
        return body == null ? null : body.get("directLoginToken");
    }
}
