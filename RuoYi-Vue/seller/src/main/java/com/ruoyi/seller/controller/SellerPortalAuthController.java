package com.ruoyi.seller.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.utils.StringUtils;
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

    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        return success(sellerService.loginSeller(loginBody));
    }

    @PostMapping("/direct-login")
    public AjaxResult directLogin(@RequestParam(value = "directLoginToken", required = false) String directLoginToken,
            @RequestBody(required = false) Map<String, String> body)
    {
        return success(sellerService.directLoginSeller(resolveDirectLoginToken(directLoginToken, body)));
    }

    @GetMapping("/direct-login")
    public AjaxResult directLogin(@RequestParam("directLoginToken") String directLoginToken)
    {
        return success(sellerService.directLoginSeller(directLoginToken));
    }

    private String resolveDirectLoginToken(String directLoginToken, Map<String, String> body)
    {
        if (StringUtils.isNotBlank(directLoginToken))
        {
            return directLoginToken;
        }
        return body == null ? null : body.get("directLoginToken");
    }
}
