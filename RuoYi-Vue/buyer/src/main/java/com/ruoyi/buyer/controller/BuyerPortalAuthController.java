package com.ruoyi.buyer.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginBody;

/**
 * Buyer portal authentication endpoints.
 */
@RestController
@RequestMapping("/buyer")
public class BuyerPortalAuthController extends BaseController
{
    @Autowired
    private IBuyerService buyerService;

    @PortalLog(terminal = "buyer", title = "买家端登录", isSaveResponseData = false, allowAnonymous = true,
            excludeParamNames = { "password", "code", "uuid" })
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        return success(buyerService.loginBuyer(loginBody));
    }

    @PortalLog(terminal = "buyer", title = "买家端免密登录", isSaveResponseData = false, allowAnonymous = true,
            excludeParamNames = { "directLoginToken" })
    @PostMapping("/direct-login")
    public AjaxResult directLogin(@RequestBody(required = false) Map<String, String> body)
    {
        return success(buyerService.directLoginBuyer(resolveDirectLoginToken(body)));
    }

    private String resolveDirectLoginToken(Map<String, String> body)
    {
        return body == null ? null : body.get("directLoginToken");
    }
}
