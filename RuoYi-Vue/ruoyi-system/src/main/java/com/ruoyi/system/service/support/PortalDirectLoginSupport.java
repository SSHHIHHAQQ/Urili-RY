package com.ruoyi.system.service.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    private static final String LOGIN_BLACK_IP_CONFIG_KEY = "sys.login.blackIPList";

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
        Long actingAdminId = resolveActingAdminId();
        String actingAdminName = resolveActingAdminName();
        assertActingAdmin(actingAdminId, actingAdminName);

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
        payload.setTicketId(ticket.getTicketId());
        payload.setPortalType(portalType);
        payload.setPartnerId(partnerId);
        payload.setPartnerNo(partnerNo);
        payload.setAccountId(account.getAccountId());
        payload.setUsername(account.getUserName());
        payload.setActingAdminId(actingAdminId);
        payload.setActingAdminName(actingAdminName);
        payload.setDirectLoginReason(directLoginReason);
        payload.setCreateBy(actingAdminName);
        payload.setCreateTime(now);
        payload.setExpireTime(expireTime);

        redisCache.setCacheObject(cacheKey(portalType, tokenHash), payload, EXPIRE_MINUTES, TimeUnit.MINUTES);

        PortalDirectLoginResult result = new PortalDirectLoginResult();
        result.setToken(token);
        result.setTicketId(ticket.getTicketId());
        result.setLoginUrl(buildLoginUrl(webUrlConfigKey, fallbackWebUrl));
        result.setExpireMinutes(EXPIRE_MINUTES);
        result.setExpireTime(expireTime);
        result.setAccountId(account.getAccountId());
        result.setUsername(account.getUserName());
        return result;
    }

    public PortalDirectLoginToken consumeToken(String portalType, String token, Consumer<PortalDirectLoginToken> validator)
    {
        return consumeToken(portalType, token, validator, null);
    }

    public PortalDirectLoginToken consumeToken(String portalType, String token, Consumer<PortalDirectLoginToken> validator,
            BiConsumer<PortalDirectLoginToken, ServiceException> failureAuditor)
    {
        if (StringUtils.isBlank(token))
        {
            throw new ServiceException("免密登录 token 不能为空");
        }
        Date now = new Date();
        if (validator == null)
        {
            throw new ServiceException("免密登录 token 校验器不能为空");
        }
        String tokenHash = hashToken(token);
        PortalDirectLoginTicket ticket = loadUsableTicket(portalType, tokenHash, now, failureAuditor);
        PortalDirectLoginToken payload = null;
        try
        {
            payload = loadUsablePayload(portalType, tokenHash, ticket, now, failureAuditor);
            assertClientIpNotBlocked();
            validator.accept(payload);
            markTicketUsedOrThrow(ticket, now, portalType, tokenHash);
            return payload;
        }
        catch (ServiceException e)
        {
            markTicketUsedAfterFailedAttempt(ticket, now, portalType, tokenHash);
            auditFailedDirectLogin(payload, e, failureAuditor);
            throw e;
        }
    }

    private void assertActingAdmin(Long actingAdminId, String actingAdminName)
    {
        if (actingAdminId == null || actingAdminId <= 0 || StringUtils.isBlank(actingAdminName))
        {
            throw new ServiceException("免密登录后台操作人不能为空");
        }
    }

    private Long resolveActingAdminId()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (ServiceException e)
        {
            throw new ServiceException("免密登录后台操作人不能为空");
        }
    }

    private String resolveActingAdminName()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (ServiceException e)
        {
            throw new ServiceException("免密登录后台操作人不能为空");
        }
    }

    private PortalDirectLoginTicket loadUsableTicket(String portalType, String tokenHash, Date now,
            BiConsumer<PortalDirectLoginToken, ServiceException> failureAuditor)
    {
        PortalDirectLoginTicket ticket = ticketMapper.selectPortalDirectLoginTicketByTokenHash(tokenHash);
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
            throw directLoginFailure(ticket, "免密登录票据已使用", failureAuditor);
        }
        if (ticket.getExpireTime() == null || ticket.getExpireTime().before(now))
        {
            ticketMapper.markPortalDirectLoginTicketExpired(ticket.getTicketId(), SYSTEM_OPERATOR);
            deletePayloadCacheKeys(portalType, tokenHash);
            throw directLoginFailure(ticket, "免密登录票据已过期", failureAuditor);
        }
        return ticket;
    }

    private PortalDirectLoginToken loadUsablePayload(String portalType, String tokenHash,
            PortalDirectLoginTicket ticket, Date now,
            BiConsumer<PortalDirectLoginToken, ServiceException> failureAuditor)
    {
        PortalDirectLoginToken payload = getPayload(portalType, tokenHash);
        if (payload == null)
        {
            ticketMapper.markPortalDirectLoginTicketExpired(ticket.getTicketId(), SYSTEM_OPERATOR);
            deletePayloadCacheKeys(portalType, tokenHash);
            throw directLoginFailure(ticket, "免密登录 token 不存在或已过期", failureAuditor);
        }
        if (!StringUtils.equals(portalType, payload.getPortalType()))
        {
            deletePayloadCacheKeys(portalType, tokenHash);
            throw directLoginFailure(ticket, "免密登录 token 端类型不匹配", failureAuditor);
        }
        if (payload.getExpireTime() == null || payload.getExpireTime().before(now))
        {
            ticketMapper.markPortalDirectLoginTicketExpired(ticket.getTicketId(), SYSTEM_OPERATOR);
            deletePayloadCacheKeys(portalType, tokenHash);
            throw directLoginFailure(ticket, "免密登录 token 已过期", failureAuditor);
        }
        if (!ticket.getTicketId().equals(payload.getTicketId()))
        {
            deletePayloadCacheKeys(portalType, tokenHash);
            throw directLoginFailure(ticket, "免密登录票据不匹配", failureAuditor);
        }
        if (!Objects.equals(ticket.getTargetSubjectId(), payload.getPartnerId())
                || !StringUtils.equals(ticket.getTargetSubjectNo(), payload.getPartnerNo())
                || !Objects.equals(ticket.getTargetAccountId(), payload.getAccountId())
                || !StringUtils.equals(ticket.getTargetUserName(), payload.getUsername()))
        {
            deletePayloadCacheKeys(portalType, tokenHash);
            throw directLoginFailure(ticket, "免密登录目标不匹配", failureAuditor);
        }
        return payload;
    }

    private PortalDirectLoginToken getPayload(String portalType, String tokenHash)
    {
        return redisCache.getCacheObject(cacheKey(portalType, tokenHash));
    }

    private ServiceException directLoginFailure(PortalDirectLoginTicket ticket, String message,
            BiConsumer<PortalDirectLoginToken, ServiceException> failureAuditor)
    {
        ServiceException exception = new ServiceException(message);
        auditFailedDirectLogin(buildTokenContext(ticket), exception, failureAuditor);
        return exception;
    }

    private PortalDirectLoginToken buildTokenContext(PortalDirectLoginTicket ticket)
    {
        PortalDirectLoginToken token = new PortalDirectLoginToken();
        token.setTicketId(ticket.getTicketId());
        token.setPortalType(ticket.getTerminal());
        token.setPartnerId(ticket.getTargetSubjectId());
        token.setPartnerNo(ticket.getTargetSubjectNo());
        token.setAccountId(ticket.getTargetAccountId());
        token.setUsername(ticket.getTargetUserName());
        token.setActingAdminId(ticket.getActingAdminId());
        token.setActingAdminName(ticket.getActingAdminName());
        token.setDirectLoginReason(ticket.getReason());
        token.setCreateBy(ticket.getCreateBy());
        token.setCreateTime(ticket.getCreateTime());
        token.setExpireTime(ticket.getExpireTime());
        return token;
    }

    private void assertClientIpNotBlocked()
    {
        String blackList = configService.selectConfigByKey(LOGIN_BLACK_IP_CONFIG_KEY);
        if (IpUtils.isMatchedIp(blackList, IpUtils.getIpAddr()))
        {
            throw new ServiceException("登录IP已被列入黑名单");
        }
    }

    private void markTicketUsedOrThrow(PortalDirectLoginTicket ticket, Date now, String portalType, String tokenHash)
    {
        if (ticketMapper.markPortalDirectLoginTicketUsed(ticket.getTicketId(), now, IpUtils.getIpAddr(), SYSTEM_OPERATOR) <= 0)
        {
            deletePayloadCacheKeys(portalType, tokenHash);
            throw new ServiceException("免密登录票据已使用");
        }
        deletePayloadCacheKeys(portalType, tokenHash);
    }

    private void markTicketUsedAfterFailedAttempt(PortalDirectLoginTicket ticket, Date now, String portalType,
            String tokenHash)
    {
        ticketMapper.markPortalDirectLoginTicketUsed(ticket.getTicketId(), now, IpUtils.getIpAddr(), SYSTEM_OPERATOR);
        deletePayloadCacheKeys(portalType, tokenHash);
    }

    private void auditFailedDirectLogin(PortalDirectLoginToken payload, ServiceException e,
            BiConsumer<PortalDirectLoginToken, ServiceException> failureAuditor)
    {
        if (payload != null && failureAuditor != null)
        {
            failureAuditor.accept(payload, e);
        }
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

    private String buildLoginUrl(String webUrlConfigKey, String fallbackWebUrl)
    {
        String baseUrl = StringUtils.defaultIfBlank(configService.selectConfigByKey(webUrlConfigKey), fallbackWebUrl);
        if (StringUtils.isBlank(baseUrl))
        {
            throw new ServiceException("端前端地址未配置：" + webUrlConfigKey);
        }

        return baseUrl;
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

    private void deletePayloadCacheKeys(String portalType, String tokenHash)
    {
        redisCache.deleteObject(cacheKey(portalType, tokenHash));
        redisCache.deleteObject(legacyCacheKey(tokenHash));
    }

    private String cacheKey(String portalType, String tokenHash)
    {
        return CACHE_PREFIX + portalType + ":" + tokenHash;
    }

    private String legacyCacheKey(String tokenHash)
    {
        return CACHE_PREFIX + tokenHash;
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
