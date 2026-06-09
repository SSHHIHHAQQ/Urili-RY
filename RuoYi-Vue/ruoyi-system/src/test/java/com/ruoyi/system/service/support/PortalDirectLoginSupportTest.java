package com.ruoyi.system.service.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalAccount;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalDirectLoginTicket;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.mapper.PortalDirectLoginTicketMapper;
import com.ruoyi.system.service.ISysConfigService;
import jakarta.servlet.http.HttpServletRequest;

public class PortalDirectLoginSupportTest
{
    private static final String SELLER_WEB_URL = "https://seller.example/direct-login";

    private RecordingRedisCache redisCache;

    private RecordingTicketMapper ticketMapper;

    private PortalDirectLoginSupport support;

    @Before
    public void setUp()
    {
        redisCache = new RecordingRedisCache();
        ticketMapper = new RecordingTicketMapper();
        support = new PortalDirectLoginSupport();
        inject(support, "redisCache", redisCache);
        inject(support, "configService", configService(SELLER_WEB_URL));
        inject(support, "ticketMapper", ticketMapper);
        setAdminSecurityContext();
        setRequestContext();
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void createTokenShouldPersistHashedTicketAndHashKeyedRedisPayload()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Need to inspect seller workspace",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");

        PortalDirectLoginTicket ticket = ticketMapper.insertedTicket;
        assertNotNull(ticket);
        assertEquals("seller", ticket.getTerminal());
        assertEquals(Long.valueOf(7L), ticket.getTargetSubjectId());
        assertEquals("SAAA010001", ticket.getTargetSubjectNo());
        assertEquals(Long.valueOf(44L), ticket.getTargetAccountId());
        assertEquals("seller-owner", ticket.getTargetUserName());
        assertEquals(Long.valueOf(9L), ticket.getActingAdminId());
        assertEquals("admin", ticket.getActingAdminName());
        assertEquals("Need to inspect seller workspace", ticket.getReason());
        assertEquals("ISSUED", ticket.getStatus());
        assertEquals("admin", ticket.getCreateBy());
        assertEquals(hash(result.getToken()), ticket.getTokenHash());
        assertNotEquals(result.getToken(), ticket.getTokenHash());
        assertEquals(64, ticket.getTokenHash().length());

        assertEquals(Long.valueOf(100L), result.getTicketId());
        assertEquals(Integer.valueOf(PortalDirectLoginSupport.EXPIRE_MINUTES), result.getExpireMinutes());
        assertEquals(Long.valueOf(44L), result.getAccountId());
        assertEquals("seller-owner", result.getUsername());
        assertTrue(result.getToken().startsWith("seller_"));
        assertEquals(SELLER_WEB_URL, result.getLoginUrl());
        assertFalse(result.getLoginUrl().contains(result.getToken()));

        String cacheKey = cacheKey(result.getToken());
        assertEquals(cacheKey, redisCache.lastSetKey);
        assertTrue(redisCache.lastSetKey.startsWith("portal_direct_login:seller:"));
        assertFalse(redisCache.values.containsKey(legacyCacheKey(result.getToken())));
        assertFalse(redisCache.lastSetKey.contains(result.getToken()));
        assertEquals(Integer.valueOf(PortalDirectLoginSupport.EXPIRE_MINUTES), redisCache.lastTimeout);
        assertEquals(TimeUnit.MINUTES, redisCache.lastTimeUnit);

        PortalDirectLoginToken payload = redisCache.getCacheObject(cacheKey);
        assertNotNull(payload);
        assertPayloadDoesNotExposeDirectLoginToken();
        assertEquals(result.getTicketId(), payload.getTicketId());
        assertEquals("seller", payload.getPortalType());
        assertEquals(Long.valueOf(7L), payload.getPartnerId());
        assertEquals("SAAA010001", payload.getPartnerNo());
        assertEquals(Long.valueOf(44L), payload.getAccountId());
        assertEquals("seller-owner", payload.getUsername());
        assertEquals("admin", payload.getCreateBy());
        assertEquals(result.getExpireTime(), payload.getExpireTime());
        assertEquals(PortalDirectLoginSupport.EXPIRE_MINUTES * 60L * 1000L,
                payload.getExpireTime().getTime() - payload.getCreateTime().getTime());
    }

    @Test
    public void createTokenShouldKeepConfiguredHashRouteUrlClean()
    {
        inject(support, "configService", configService("https://portal.example/#/seller/direct-login"));

        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Need to inspect seller workspace",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, null);

        assertEquals("https://portal.example/#/seller/direct-login", result.getLoginUrl());
        assertFalse(result.getLoginUrl().contains(result.getToken()));
    }

    @Test
    public void createTokenShouldKeepConfiguredHashRouteQueryUrlClean()
    {
        inject(support, "configService", configService("https://portal.example/#/seller/direct-login?source=admin"));

        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Need to inspect seller workspace",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, null);

        assertEquals("https://portal.example/#/seller/direct-login?source=admin", result.getLoginUrl());
        assertFalse(result.getLoginUrl().contains(result.getToken()));
    }

    @Test
    public void consumeTokenShouldMarkTicketUsedOnceAndDeleteRedisPayload()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");

        PortalDirectLoginToken payload = support.consumeToken("seller", result.getToken(), token -> {
        });

        assertEquals(result.getTicketId(), payload.getTicketId());
        assertEquals(1, ticketMapper.usedCalls);
        assertEquals(result.getTicketId(), ticketMapper.usedTicketId);
        assertNotNull(ticketMapper.usedTime);
        assertEquals("203.0.113.10", ticketMapper.usedIp);
        assertEquals("system", ticketMapper.usedUpdateBy);
        assertTrue(redisCache.deletedKeys.contains(cacheKey(result.getToken())));
        assertNull(redisCache.getCacheObject(cacheKey(result.getToken())));

        assertThrows(ServiceException.class, () -> support.consumeToken("seller", result.getToken(), token -> {
        }));
        assertEquals(1, ticketMapper.usedCalls);
    }

    @Test
    public void consumeTokenShouldRejectLegacyRedisPayloadAndDeleteBothKeyShapes()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");
        String cacheKey = cacheKey(result.getToken());
        String legacyCacheKey = legacyCacheKey(result.getToken());
        PortalDirectLoginToken legacyPayload = redisCache.getCacheObject(cacheKey);
        redisCache.values.remove(cacheKey);
        redisCache.values.put(legacyCacheKey, legacyPayload);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.consumeToken("seller", result.getToken(), token -> {
                }));

        assertEquals("免密登录 token 不存在或已过期", exception.getMessage());
        assertEquals(0, ticketMapper.usedCalls);
        assertEquals(1, ticketMapper.expiredCalls);
        assertEquals(result.getTicketId(), ticketMapper.expiredTicketId);
        assertTrue(redisCache.deletedKeys.contains(cacheKey));
        assertTrue(redisCache.deletedKeys.contains(legacyCacheKey));
        assertNull(redisCache.getCacheObject(cacheKey));
        assertNull(redisCache.getCacheObject(legacyCacheKey));
    }

    @Test
    public void consumeTokenShouldExpireTicketWhenRedisPayloadIsMissing()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");
        String cacheKey = cacheKey(result.getToken());
        redisCache.values.remove(cacheKey);
        PortalDirectLoginToken[] auditedPayload = new PortalDirectLoginToken[1];
        ServiceException[] auditedException = new ServiceException[1];

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.consumeToken("seller", result.getToken(), token -> {
                }, (payload, failure) -> {
                    auditedPayload[0] = payload;
                    auditedException[0] = failure;
                }));

        assertEquals("免密登录 token 不存在或已过期", exception.getMessage());
        assertEquals(exception, auditedException[0]);
        assertTicketContext(result.getTicketId(), auditedPayload[0]);
        assertEquals(0, ticketMapper.usedCalls);
        assertEquals(1, ticketMapper.expiredCalls);
        assertEquals(result.getTicketId(), ticketMapper.expiredTicketId);
        assertEquals("system", ticketMapper.expiredUpdateBy);
        assertTrue(redisCache.deletedKeys.contains(cacheKey));
    }

    @Test
    public void consumeTokenShouldConsumeTicketWhenBlacklistedIpAttemptsLogin()
    {
        inject(support, "configService", configService(SELLER_WEB_URL, "203.0.113.10"));
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.consumeToken("seller", result.getToken(), token -> {
                }));

        assertEquals("登录IP已被列入黑名单", exception.getMessage());
        assertEquals(1, ticketMapper.usedCalls);
        assertEquals(result.getTicketId(), ticketMapper.usedTicketId);
        assertEquals(0, ticketMapper.expiredCalls);
        assertTrue(redisCache.deletedKeys.contains(cacheKey(result.getToken())));
        assertNull(redisCache.getCacheObject(cacheKey(result.getToken())));
    }

    @Test
    public void consumeTokenShouldConsumeTicketWhenValidatorRejectsLogin()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");

        assertThrows(ServiceException.class, () -> support.consumeToken("seller", result.getToken(), payload -> {
            throw new ServiceException("seller disabled");
        }));

        assertEquals(1, ticketMapper.usedCalls);
        assertEquals(result.getTicketId(), ticketMapper.usedTicketId);
        assertEquals(0, ticketMapper.expiredCalls);
        assertTrue(redisCache.deletedKeys.contains(cacheKey(result.getToken())));
        assertNull(redisCache.getCacheObject(cacheKey(result.getToken())));

        assertThrows(ServiceException.class, () -> support.consumeToken("seller", result.getToken(), payload -> {
        }));
        assertEquals(1, ticketMapper.usedCalls);
    }

    @Test
    public void consumeTokenShouldAuditFailedPayloadWhenValidatorRejectsLogin()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");
        PortalDirectLoginToken[] auditedPayload = new PortalDirectLoginToken[1];
        ServiceException[] auditedException = new ServiceException[1];

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.consumeToken("seller", result.getToken(), payload -> {
                    throw new ServiceException("seller disabled");
                }, (payload, failure) -> {
                    auditedPayload[0] = payload;
                    auditedException[0] = failure;
                }));

        assertEquals("seller disabled", exception.getMessage());
        assertEquals(exception, auditedException[0]);
        assertEquals(result.getTicketId(), auditedPayload[0].getTicketId());
        assertEquals("seller", auditedPayload[0].getPortalType());
        assertEquals(Long.valueOf(7L), auditedPayload[0].getPartnerId());
        assertEquals(Long.valueOf(44L), auditedPayload[0].getAccountId());
        assertEquals(1, ticketMapper.usedCalls);
        assertEquals(result.getTicketId(), ticketMapper.usedTicketId);
        assertTrue(redisCache.deletedKeys.contains(cacheKey(result.getToken())));
        assertNull(redisCache.getCacheObject(cacheKey(result.getToken())));
    }

    @Test
    public void consumeTokenShouldRejectPayloadTargetMismatchBeforeMarkingTicketUsed()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");
        String cacheKey = cacheKey(result.getToken());
        PortalDirectLoginToken payload = redisCache.getCacheObject(cacheKey);
        payload.setPartnerId(8L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.consumeToken("seller", result.getToken(), token -> {
                }));

        assertEquals("免密登录目标不匹配", exception.getMessage());
        assertEquals(1, ticketMapper.usedCalls);
        assertEquals(result.getTicketId(), ticketMapper.usedTicketId);
        assertEquals(0, ticketMapper.expiredCalls);
        assertTrue(redisCache.deletedKeys.contains(cacheKey));
        assertNull(redisCache.getCacheObject(cacheKey));
    }

    @Test
    public void consumeTokenShouldRejectWrongTerminalWithoutConsumingRealScopedPayload()
    {
        PortalDirectLoginResult result = support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "Support inspection",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login");
        String sellerCacheKey = cacheKey(result.getToken());
        String buyerCacheKey = cacheKey("buyer", result.getToken());
        PortalDirectLoginToken[] auditedPayload = new PortalDirectLoginToken[1];
        ServiceException[] auditedException = new ServiceException[1];

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.consumeToken("buyer", result.getToken(), token -> {
                }, (payload, failure) -> {
                    auditedPayload[0] = payload;
                    auditedException[0] = failure;
                }));

        assertEquals("免密登录票据端类型不匹配", exception.getMessage());
        assertNull(auditedException[0]);
        assertNull(auditedPayload[0]);
        assertEquals(0, ticketMapper.usedCalls);
        assertEquals(0, ticketMapper.expiredCalls);
        assertFalse(redisCache.deletedKeys.contains(sellerCacheKey));
        assertTrue(redisCache.deletedKeys.contains(buyerCacheKey));
        assertNotNull(redisCache.getCacheObject(sellerCacheKey));

        PortalDirectLoginToken payload = support.consumeToken("seller", result.getToken(), token -> {
        });

        assertEquals(result.getTicketId(), payload.getTicketId());
        assertEquals(1, ticketMapper.usedCalls);
        assertEquals(result.getTicketId(), ticketMapper.usedTicketId);
        assertTrue(redisCache.deletedKeys.contains(sellerCacheKey));
        assertNull(redisCache.getCacheObject(sellerCacheKey));
    }

    @Test
    public void consumeTokenShouldExpireTicketAndDeleteRedisPayloadWhenTicketIsExpired()
    {
        String token = "seller_expired_token";
        String cacheKey = cacheKey(token);
        PortalDirectLoginTicket ticket = new PortalDirectLoginTicket();
        ticket.setTicketId(200L);
        ticket.setTerminal("seller");
        ticket.setTokenHash(hash(token));
        ticket.setExpireTime(new Date(System.currentTimeMillis() - 1000L));
        ticket.setStatus("ISSUED");
        ticketMapper.store(ticket);

        PortalDirectLoginToken payload = new PortalDirectLoginToken();
        payload.setTicketId(ticket.getTicketId());
        payload.setPortalType("seller");
        payload.setExpireTime(new Date(System.currentTimeMillis() - 1000L));
        redisCache.values.put(cacheKey, payload);

        assertThrows(ServiceException.class, () -> support.consumeToken("seller", token, ignored -> {
        }));

        assertEquals(0, ticketMapper.usedCalls);
        assertEquals(1, ticketMapper.expiredCalls);
        assertEquals(Long.valueOf(200L), ticketMapper.expiredTicketId);
        assertEquals("system", ticketMapper.expiredUpdateBy);
        assertTrue(redisCache.deletedKeys.contains(cacheKey));
        assertNull(redisCache.getCacheObject(cacheKey));
    }

    @Test
    public void createTokenShouldRequireReasonBeforePersistingTicketOrRedisPayload()
    {
        assertThrows(ServiceException.class, () -> support.createToken("seller", 7L, "SAAA010001",
                activeAccount(44L, "seller-owner"), "   ",
                PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login"));

        assertNull(ticketMapper.insertedTicket);
        assertTrue(redisCache.values.isEmpty());
    }

    @Test
    public void createTokenShouldRequireActingAdminBeforePersistingTicketOrRedisPayload()
    {
        SecurityContextHolder.clearContext();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> support.createToken("seller", 7L, "SAAA010001",
                        activeAccount(44L, "seller-owner"), "Support inspection",
                        PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, "http://fallback/seller/direct-login"));

        assertEquals("免密登录后台操作人不能为空", exception.getMessage());
        assertNull(ticketMapper.insertedTicket);
        assertTrue(redisCache.values.isEmpty());
    }

    private static TestPortalAccount activeAccount(Long accountId, String userName)
    {
        TestPortalAccount account = new TestPortalAccount();
        account.setAccountId(accountId);
        account.setUserName(userName);
        account.setStatus(PartnerSupport.STATUS_NORMAL);
        return account;
    }

    private static void setAdminSecurityContext()
    {
        SysUser user = new SysUser();
        user.setUserId(9L);
        user.setDeptId(3L);
        user.setUserName("admin");
        LoginUser loginUser = new LoginUser(9L, 3L, user, Collections.singleton("*:*:*"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static void setRequestContext()
    {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName()))
                    {
                        return null;
                    }
                    if ("getRemoteAddr".equals(method.getName()))
                    {
                        return "203.0.113.10";
                    }
                    if ("getRequestURI".equals(method.getName()))
                    {
                        return "/seller/direct-login";
                    }
                    if ("getParameter".equals(method.getName()))
                    {
                        return null;
                    }
                    if ("getParameterMap".equals(method.getName()))
                    {
                        return Collections.emptyMap();
                    }
                    if ("getMethod".equals(method.getName()))
                    {
                        return "GET";
                    }
                    return defaultValue(method.getReturnType());
                });
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static ISysConfigService configService(String portalWebUrl)
    {
        return configService(portalWebUrl, "");
    }

    private static ISysConfigService configService(String portalWebUrl, String blackIpList)
    {
        return (ISysConfigService) Proxy.newProxyInstance(ISysConfigService.class.getClassLoader(),
                new Class<?>[] { ISysConfigService.class }, (proxy, method, args) -> {
                    if ("selectConfigByKey".equals(method.getName()))
                    {
                        if ("sys.login.blackIPList".equals(args[0]))
                        {
                            return blackIpList;
                        }
                        return portalWebUrl;
                    }
                    if (boolean.class.equals(method.getReturnType()))
                    {
                        return false;
                    }
                    if (int.class.equals(method.getReturnType()))
                    {
                        return 0;
                    }
                    return null;
                });
    }

    private static void inject(Object target, String fieldName, Object value)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError("Unable to inject " + fieldName, e);
        }
    }

    private static Object defaultValue(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (boolean.class.equals(returnType))
        {
            return false;
        }
        if (byte.class.equals(returnType))
        {
            return (byte) 0;
        }
        if (short.class.equals(returnType))
        {
            return (short) 0;
        }
        if (int.class.equals(returnType))
        {
            return 0;
        }
        if (long.class.equals(returnType))
        {
            return 0L;
        }
        if (float.class.equals(returnType))
        {
            return 0F;
        }
        if (double.class.equals(returnType))
        {
            return 0D;
        }
        if (char.class.equals(returnType))
        {
            return '\0';
        }
        return null;
    }

    private static String hash(String token)
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
        catch (Exception e)
        {
            throw new AssertionError("Unable to hash test token", e);
        }
    }

    private static String cacheKey(String token)
    {
        return cacheKey("seller", token);
    }

    private static String cacheKey(String portalType, String token)
    {
        return "portal_direct_login:" + portalType + ":" + hash(token);
    }

    private static String legacyCacheKey(String token)
    {
        return "portal_direct_login:" + hash(token);
    }

    private static void assertPayloadDoesNotExposeDirectLoginToken()
    {
        for (Field field : PortalDirectLoginToken.class.getDeclaredFields())
        {
            assertNotEquals("token", field.getName());
        }
    }

    private static void assertTicketContext(Long ticketId, PortalDirectLoginToken token)
    {
        assertNotNull(token);
        assertEquals(ticketId, token.getTicketId());
        assertEquals("seller", token.getPortalType());
        assertEquals(Long.valueOf(7L), token.getPartnerId());
        assertEquals("SAAA010001", token.getPartnerNo());
        assertEquals(Long.valueOf(44L), token.getAccountId());
        assertEquals("seller-owner", token.getUsername());
        assertEquals(Long.valueOf(9L), token.getActingAdminId());
        assertEquals("admin", token.getActingAdminName());
        assertEquals("Support inspection", token.getDirectLoginReason());
    }

    private static class TestPortalAccount extends PortalAccount
    {
        private static final long serialVersionUID = 1L;
    }

    private static class RecordingRedisCache extends RedisCache
    {
        private final Map<String, Object> values = new HashMap<>();

        private final java.util.Set<String> deletedKeys = new java.util.HashSet<>();

        private String lastSetKey;

        private Integer lastTimeout;

        private TimeUnit lastTimeUnit;

        @Override
        public <T> void setCacheObject(String key, T value, Integer timeout, TimeUnit timeUnit)
        {
            values.put(key, value);
            lastSetKey = key;
            lastTimeout = timeout;
            lastTimeUnit = timeUnit;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCacheObject(String key)
        {
            return (T) values.get(key);
        }

        @Override
        public boolean deleteObject(String key)
        {
            deletedKeys.add(key);
            values.remove(key);
            return true;
        }
    }

    private static class RecordingTicketMapper implements PortalDirectLoginTicketMapper
    {
        private final Map<String, PortalDirectLoginTicket> ticketsByHash = new HashMap<>();

        private PortalDirectLoginTicket insertedTicket;

        private long nextTicketId = 100L;

        private int usedCalls;

        private Long usedTicketId;

        private Date usedTime;

        private String usedIp;

        private String usedUpdateBy;

        private int expiredCalls;

        private Long expiredTicketId;

        private String expiredUpdateBy;

        @Override
        public int insertPortalDirectLoginTicket(PortalDirectLoginTicket ticket)
        {
            ticket.setTicketId(nextTicketId++);
            insertedTicket = ticket;
            store(ticket);
            return 1;
        }

        @Override
        public List<PortalDirectLoginTicket> selectPortalDirectLoginTicketList(PortalDirectLoginTicket ticket)
        {
            return Collections.emptyList();
        }

        @Override
        public PortalDirectLoginTicket selectPortalDirectLoginTicketByTokenHash(String tokenHash)
        {
            return ticketsByHash.get(tokenHash);
        }

        @Override
        public int markPortalDirectLoginTicketUsed(Long ticketId, Date usedTime, String usedIp, String updateBy)
        {
            PortalDirectLoginTicket ticket = findByTicketId(ticketId);
            if (ticket == null || !"ISSUED".equals(ticket.getStatus()) || ticket.getUsedTime() != null
                    || ticket.getExpireTime().before(usedTime))
            {
                return 0;
            }
            usedCalls++;
            usedTicketId = ticketId;
            this.usedTime = usedTime;
            this.usedIp = usedIp;
            usedUpdateBy = updateBy;
            ticket.setUsedTime(usedTime);
            ticket.setUsedIp(usedIp);
            ticket.setStatus("USED");
            return 1;
        }

        @Override
        public int markPortalDirectLoginTicketExpired(Long ticketId, String updateBy)
        {
            PortalDirectLoginTicket ticket = findByTicketId(ticketId);
            if (ticket == null || !"ISSUED".equals(ticket.getStatus()))
            {
                return 0;
            }
            expiredCalls++;
            expiredTicketId = ticketId;
            expiredUpdateBy = updateBy;
            ticket.setStatus("EXPIRED");
            return 1;
        }

        private void store(PortalDirectLoginTicket ticket)
        {
            ticketsByHash.put(ticket.getTokenHash(), ticket);
        }

        private PortalDirectLoginTicket findByTicketId(Long ticketId)
        {
            return ticketsByHash.values().stream()
                    .filter(ticket -> ticketId.equals(ticket.getTicketId()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
