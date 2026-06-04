package com.ruoyi.system.service.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.PortalAccount;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.service.ISysConfigService;

/**
 * Shared one-time direct-login token creation for buyer/seller portals.
 */
@Component
public class PortalDirectLoginSupport
{
    public static final int EXPIRE_MINUTES = 30;

    public static final String SELLER_WEB_URL_CONFIG_KEY = "portal.seller.web.url";

    public static final String BUYER_WEB_URL_CONFIG_KEY = "portal.buyer.web.url";

    private static final String CACHE_PREFIX = "portal_direct_login:";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    public PortalDirectLoginResult createToken(String portalType, Long partnerId, String partnerNo,
            PortalAccount account, String webUrlConfigKey, String fallbackWebUrl)
    {
        assertAccountCanLogin(account);

        Date now = new Date();
        Date expireTime = new Date(now.getTime() + EXPIRE_MINUTES * 60L * 1000L);
        String token = portalType + "_" + IdUtils.fastSimpleUUID();

        PortalDirectLoginToken payload = new PortalDirectLoginToken();
        payload.setToken(token);
        payload.setPortalType(portalType);
        payload.setPartnerId(partnerId);
        payload.setPartnerNo(partnerNo);
        payload.setUserId(account.getUserId());
        payload.setUsername(account.getUserName());
        payload.setCreateBy(SecurityUtils.getUsername());
        payload.setCreateTime(now);
        payload.setExpireTime(expireTime);

        redisCache.setCacheObject(CACHE_PREFIX + token, payload, EXPIRE_MINUTES, TimeUnit.MINUTES);

        PortalDirectLoginResult result = new PortalDirectLoginResult();
        result.setToken(token);
        result.setLoginUrl(buildLoginUrl(webUrlConfigKey, fallbackWebUrl, token));
        result.setExpireMinutes(EXPIRE_MINUTES);
        result.setExpireTime(expireTime);
        result.setUserId(account.getUserId());
        result.setUsername(account.getUserName());
        return result;
    }

    private void assertAccountCanLogin(PortalAccount account)
    {
        if (account == null || account.getUserId() == null)
        {
            throw new ServiceException("主账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("端账号已停用，不能免密登录");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getUserStatus()))
        {
            throw new ServiceException("用户已停用，不能免密登录");
        }
    }

    private String buildLoginUrl(String webUrlConfigKey, String fallbackWebUrl, String token)
    {
        String baseUrl = StringUtils.defaultIfBlank(configService.selectConfigByKey(webUrlConfigKey), fallbackWebUrl);
        if (StringUtils.isBlank(baseUrl))
        {
            throw new ServiceException("端前端地址未配置：" + webUrlConfigKey);
        }

        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "directLoginToken=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
