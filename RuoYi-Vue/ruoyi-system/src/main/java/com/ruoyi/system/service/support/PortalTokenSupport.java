package com.ruoyi.system.service.support;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.UserAgentUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.PortalAccount;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.domain.PortalLoginIssue;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginResult;
import com.ruoyi.system.domain.PortalLoginSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Seller/buyer portal token support isolated from the admin token cache.
 */
@Component
public class PortalTokenSupport
{
    public static final String CLAIM_LOGIN_KEY = "portal_login_key";

    public static final String CLAIM_TERMINAL = "portal_terminal";

    public static final String CACHE_PREFIX = "portal_login_tokens:";

    @Value("${token.secret}")
    private String secret;

    @Value("${token.header}")
    private String header;

    @Value("${token.expireTime}")
    private int expireTime;

    @Autowired
    private RedisCache redisCache;

    public PortalLoginIssue createLogin(String terminal, Long subjectId, String subjectNo, PortalAccount account)
    {
        return createLogin(terminal, subjectId, subjectNo, account, null);
    }

    public PortalLoginIssue createLogin(String terminal, Long subjectId, String subjectNo, PortalAccount account,
            PortalDirectLoginToken directLoginToken)
    {
        Date loginTime = new Date();
        Date expireAt = new Date(loginTime.getTime() + expireTime * 60L * 1000L);
        String tokenId = terminal + "_" + IdUtils.fastSimpleUUID();

        PortalLoginSession session = buildSession(terminal, subjectId, subjectNo, account, tokenId, loginTime, expireAt);
        applyDirectLoginAudit(session, directLoginToken);
        redisCache.setCacheObject(getTokenKey(terminal, tokenId), session, expireTime, TimeUnit.MINUTES);

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_LOGIN_KEY, tokenId);
        claims.put(CLAIM_TERMINAL, terminal);
        claims.put(Constants.JWT_USERNAME, account.getUserName());

        PortalLoginResult result = new PortalLoginResult();
        result.setToken(createJwtToken(claims));
        result.setTerminal(terminal);
        result.setSubjectId(subjectId);
        result.setSubjectNo(subjectNo);
        result.setAccountId(account.getAccountId());
        result.setUsername(account.getUserName());
        result.setNickName(account.getNickName());
        result.setExpireMinutes(expireTime);
        result.setExpireTime(expireAt);

        PortalLoginIssue issue = new PortalLoginIssue();
        issue.setResult(result);
        issue.setSession(session);
        return issue;
    }

    public PortalLoginLog buildLoginLog(Long subjectId, Long accountId, String userName, String status, String msg)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();

        PortalLoginLog log = new PortalLoginLog();
        log.setSubjectId(subjectId);
        log.setAccountId(accountId);
        log.setUserName(userName);
        log.setIpaddr(ip);
        log.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        log.setBrowser(UserAgentUtils.getBrowser(userAgent));
        log.setOs(UserAgentUtils.getOperatingSystem(userAgent));
        log.setStatus(status);
        log.setMsg(msg);
        log.setDirectLogin(Boolean.FALSE);
        log.setLoginTime(new Date());
        return log;
    }

    public PortalLoginLog buildDirectLoginLog(Long subjectId, Long accountId, String userName, String status,
            String msg, PortalDirectLoginToken directLoginToken)
    {
        PortalLoginLog log = buildLoginLog(subjectId, accountId, userName, status, msg);
        applyDirectLoginAudit(log, directLoginToken);
        return log;
    }

    public PortalLoginLog buildDirectLoginLog(Long subjectId, Long accountId, String userName, String status,
            String msg, PortalLoginSession session)
    {
        PortalLoginLog log = buildLoginLog(subjectId, accountId, userName, status, msg);
        applyDirectLoginAudit(log, session);
        return log;
    }

    public PortalLoginSession getSession(String expectedTerminal)
    {
        return getSession(expectedTerminal, getToken());
    }

    public PortalLoginSession getSession(String expectedTerminal, String token)
    {
        token = normalizeToken(token);
        if (StringUtils.isEmpty(token))
        {
            return null;
        }

        try
        {
            Claims claims = parseToken(token);
            String terminal = (String) claims.get(CLAIM_TERMINAL);
            String tokenId = (String) claims.get(CLAIM_LOGIN_KEY);
            if (!StringUtils.equals(expectedTerminal, terminal) || StringUtils.isEmpty(tokenId))
            {
                return null;
            }
            PortalLoginSession session = redisCache.getCacheObject(getTokenKey(expectedTerminal, tokenId));
            if (session == null || !StringUtils.equals(expectedTerminal, session.getTerminal()))
            {
                return null;
            }
            return session;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public PortalLoginSession requireSession(String expectedTerminal)
    {
        PortalLoginSession session = getSession(expectedTerminal);
        if (session == null)
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        return session;
    }

    private void applyDirectLoginAudit(PortalLoginSession session, PortalDirectLoginToken directLoginToken)
    {
        if (directLoginToken == null)
        {
            return;
        }
        session.setDirectLogin(Boolean.TRUE);
        session.setDirectLoginTicketId(directLoginToken.getTicketId());
        session.setActingAdminId(directLoginToken.getActingAdminId());
        session.setActingAdminName(directLoginToken.getActingAdminName());
        session.setDirectLoginReason(directLoginToken.getDirectLoginReason());
    }

    private void applyDirectLoginAudit(PortalLoginLog log, PortalDirectLoginToken directLoginToken)
    {
        if (directLoginToken == null)
        {
            return;
        }
        log.setDirectLogin(Boolean.TRUE);
        log.setDirectLoginTicketId(directLoginToken.getTicketId());
        log.setActingAdminId(directLoginToken.getActingAdminId());
        log.setActingAdminName(directLoginToken.getActingAdminName());
        log.setDirectLoginReason(directLoginToken.getDirectLoginReason());
    }

    private void applyDirectLoginAudit(PortalLoginLog log, PortalLoginSession session)
    {
        if (session == null || !Boolean.TRUE.equals(session.getDirectLogin()))
        {
            return;
        }
        log.setDirectLogin(Boolean.TRUE);
        log.setDirectLoginTicketId(session.getDirectLoginTicketId());
        log.setActingAdminId(session.getActingAdminId());
        log.setActingAdminName(session.getActingAdminName());
        log.setDirectLoginReason(session.getDirectLoginReason());
    }

    public void deleteLoginTokens(String terminal, List<String> tokenIds)
    {
        if (StringUtils.isEmpty(terminal) || tokenIds == null || tokenIds.isEmpty())
        {
            return;
        }
        List<String> keys = tokenIds.stream()
                .filter(StringUtils::isNotEmpty)
                .map(tokenId -> getTokenKey(terminal, tokenId))
                .collect(Collectors.toList());
        if (!keys.isEmpty())
        {
            redisCache.deleteObject(keys);
        }
    }

    public void deleteLoginToken(PortalLoginSession session)
    {
        if (session == null)
        {
            return;
        }
        deleteLoginTokens(session.getTerminal(), List.of(session.getTokenId()));
    }

    private PortalLoginSession buildSession(String terminal, Long subjectId, String subjectNo, PortalAccount account,
            String tokenId, Date loginTime, Date expireAt)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();

        PortalLoginSession session = new PortalLoginSession();
        session.setTokenId(tokenId);
        session.setTerminal(terminal);
        session.setSubjectId(subjectId);
        session.setSubjectNo(subjectNo);
        session.setAccountId(account.getAccountId());
        session.setUserName(account.getUserName());
        session.setNickName(account.getNickName());
        session.setLoginIp(ip);
        session.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        session.setBrowser(UserAgentUtils.getBrowser(userAgent));
        session.setOs(UserAgentUtils.getOperatingSystem(userAgent));
        session.setLoginTime(loginTime);
        session.setExpireTime(expireAt);
        session.setStatus(PartnerSupport.STATUS_NORMAL);
        session.setDirectLogin(Boolean.FALSE);
        return session;
    }

    private String getTokenKey(String terminal, String tokenId)
    {
        return CACHE_PREFIX + terminal + ":" + tokenId;
    }

    private String createJwtToken(Map<String, Object> claims)
    {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    private String getToken()
    {
        return normalizeToken(ServletUtils.getRequest().getHeader(header));
    }

    private String normalizeToken(String token)
    {
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }
}
