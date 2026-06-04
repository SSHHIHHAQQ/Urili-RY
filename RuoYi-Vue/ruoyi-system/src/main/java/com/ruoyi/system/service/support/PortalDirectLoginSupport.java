package com.ruoyi.system.service.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.PortalAccount;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.mapper.PortalDirectLoginTicketMapper;
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

    private static final String STATUS_ISSUED = "ISSUED";

    private static final String SYSTEM_OPERATOR = "system";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private PortalDirectLoginTicketMapper ticketMapper;

    public PortalDirectLoginResult createToken(String portalType, Long partnerId, String partnerNo,
            PortalAccount account, String reason, String webUrlConfigKey, String fallbackWebUrl)
    {
        assertAccountCanLogin(account);
        String directLoginReason = normalizeReason(reason);

        Date now = new Date();
        Date expireTime = new Date(now.getTime() + EXPIRE_MINUTES * 60L * 1000L);
        String token = portalType + "_" + IdUtils.fastSimpleUUID();
        String tokenHash = hashToken(token);
        Long actingAdminId = SecurityUtils.getUserId();
        String actingAdminName = SecurityUtils.getUsername();

        PortalDirectLoginTicket ticket = new PortalDirectLoginTicket();
        ticket.setTerminal(portalType);
        ticket.setTargetSubjectId(partnerId);
        ticket.setTargetSubjectNo(partnerNo);
        ticket.setTargetAccountId(account.getAccountId());
        ticket.setTargetUserName(account.getUserName());
        ticket.setActingAdminId(actingAdminId);
        ticket.setActingAdminName(actingAdminName);
        ticket.setReason(directLoginReason);
        ticket.setTokenHash(tokenHash);
        ticket.setExpireTime(expireTime);
        ticket.setStatus(STATUS_ISSUED);
        ticket.setCreateBy(actingAdminName);
        ticketMapper.insertPortalDirectLoginTicket(ticket);

        PortalDirectLoginToken payload = new PortalDirectLoginToken();
        payload.setToken(token);
        payload.setTicketId(ticket.getTicketId());
        payload.setPortalType(portalType);
        payload.setPartnerId(partnerId);
        payload.setPartnerNo(partnerNo);
        payload.setAccountId(account.getAccountId());
        payload.setUsername(account.getUserName());
        payload.setCreateBy(actingAdminName);
        payload.setCreateTime(now);
        payload.setExpireTime(expireTime);

        redisCache.setCacheObject(CACHE_PREFIX + token, payload, EXPIRE_MINUTES, TimeUnit.MINUTES);

        PortalDirectLoginResult result = new PortalDirectLoginResult();
        result.setToken(token);
        result.setTicketId(ticket.getTicketId());
        result.setLoginUrl(buildLoginUrl(webUrlConfigKey, fallbackWebUrl, token));
        result.setExpireMinutes(EXPIRE_MINUTES);
        result.setExpireTime(expireTime);
        result.setAccountId(account.getAccountId());
        result.setUsername(account.getUserName());
        return result;
    }

    public PortalDirectLoginToken consumeToken(String portalType, String token)
    {
        if (StringUtils.isBlank(token))
        {
            throw new ServiceException("免密登录 token 不能为空");
        }

        Date now = new Date();
        String cacheKey = CACHE_PREFIX + token;
        PortalDirectLoginTicket ticket = loadUsableTicket(portalType, token, now, cacheKey);
        PortalDirectLoginToken payload = loadUsablePayload(portalType, cacheKey, ticket, now);

        if (ticketMapper.markPortalDirectLoginTicketUsed(ticket.getTicketId(), now, IpUtils.getIpAddr(), SYSTEM_OPERATOR) <= 0)
        {
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("免密登录票据已使用");
        }

        redisCache.deleteObject(cacheKey);
        return payload;
    }

    private PortalDirectLoginTicket loadUsableTicket(String portalType, String token, Date now, String cacheKey)
    {
        PortalDirectLoginTicket ticket = ticketMapper.selectPortalDirectLoginTicketByTokenHash(hashToken(token));
        if (ticket == null)
        {
            throw new ServiceException("免密登录票据不存在");
        }
        if (!StringUtils.equals(portalType, ticket.getTerminal()))
        {
            throw new ServiceException("免密登录票据端类型不匹配");
        }
        if (!STATUS_ISSUED.equals(ticket.getStatus()) || ticket.getUsedTime() != null)
        {
            throw new ServiceException("免密登录票据已使用");
        }
        if (ticket.getExpireTime() == null || ticket.getExpireTime().before(now))
        {
            ticketMapper.markPortalDirectLoginTicketExpired(ticket.getTicketId(), SYSTEM_OPERATOR);
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("免密登录票据已过期");
        }
        return ticket;
    }

    private PortalDirectLoginToken loadUsablePayload(String portalType, String cacheKey,
            PortalDirectLoginTicket ticket, Date now)
    {
        PortalDirectLoginToken payload = redisCache.getCacheObject(cacheKey);
        if (payload == null)
        {
            throw new ServiceException("免密登录 token 不存在或已过期");
        }
        if (!StringUtils.equals(portalType, payload.getPortalType()))
        {
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("免密登录 token 端类型不匹配");
        }
        if (payload.getExpireTime() == null || payload.getExpireTime().before(now))
        {
            ticketMapper.markPortalDirectLoginTicketExpired(ticket.getTicketId(), SYSTEM_OPERATOR);
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("免密登录 token 已过期");
        }
        if (!ticket.getTicketId().equals(payload.getTicketId()))
        {
            redisCache.deleteObject(cacheKey);
            throw new ServiceException("免密登录票据不匹配");
        }
        return payload;
    }

    private void assertAccountCanLogin(PortalAccount account)
    {
        if (account == null || account.getAccountId() == null)
        {
            throw new ServiceException("主账号不存在");
        }
        if (!PartnerSupport.STATUS_NORMAL.equals(account.getStatus()))
        {
            throw new ServiceException("端账号已停用，不能免密登录");
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

    private String hashToken(String token)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes)
            {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new ServiceException("免密登录 token 哈希失败");
        }
    }

    private String normalizeReason(String reason)
    {
        String value = PartnerSupport.trimRequired(reason, "免密登录原因不能为空");
        if (value.length() > 255)
        {
            throw new ServiceException("免密登录原因不能超过255个字符");
        }
        return value;
    }
}
