package com.ruoyi.system.service.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalAccount;
import com.ruoyi.system.domain.PortalDirectLoginToken;
import com.ruoyi.system.domain.PortalLoginIssue;
import com.ruoyi.system.domain.PortalLoginSession;

public class PortalTokenSupportTest
{
    private static final String HEADER = "Authorization";

    private static final String SECRET = "portal-token-support-test-secret";

    @After
    public void tearDown()
    {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void createLoginStoresSessionUnderTerminalScopedRedisKey()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        PortalTokenSupport support = support(redisCache);
        bindRequest(Map.of("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0"), "127.0.0.1");

        PortalLoginIssue issue = support.createLogin("seller", 11L, "SAA010001", account(22L, "seller-owner"));

        assertNotNull(issue.getResult().getToken());
        assertEquals("seller", issue.getResult().getTerminal());
        assertEquals(Long.valueOf(11L), issue.getResult().getSubjectId());
        assertEquals(Long.valueOf(22L), issue.getResult().getAccountId());
        assertEquals("seller-owner", issue.getResult().getUsername());
        assertEquals(Integer.valueOf(30), issue.getResult().getExpireMinutes());

        String key = redisCache.onlyStoredKey();
        assertTrue(key.startsWith(PortalTokenSupport.CACHE_PREFIX + "seller:seller_"));
        assertFalse(key.startsWith("login_tokens:"));
        assertEquals(Integer.valueOf(30), redisCache.timeouts.get(key));
        assertEquals(TimeUnit.MINUTES, redisCache.units.get(key));
        assertSame(issue.getSession(), redisCache.store.get(key));
    }

    @Test
    public void getSessionRequiresJwtTerminalAndStoredSessionTerminalToMatch()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        PortalTokenSupport support = support(redisCache);
        bindRequest(Map.of("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0"), "127.0.0.1");
        PortalLoginIssue issue = support.createLogin("seller", 11L, "SAA010001", account(22L, "seller-owner"));

        bindRequest(Map.of(HEADER, Constants.TOKEN_PREFIX + issue.getResult().getToken()), "127.0.0.1");

        assertSame(issue.getSession(), support.getSession("seller"));
        assertNull(support.getSession("buyer"));

        issue.getSession().setTerminal("buyer");

        assertNull(support.getSession("seller"));
    }

    @Test
    public void getSessionCanResolveExplicitLoginResultTokenForAnonymousLoginAudit()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        PortalTokenSupport support = support(redisCache);
        bindRequest(Map.of("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0"), "127.0.0.1");
        PortalLoginIssue issue = support.createLogin("seller", 11L, "SAA010001", account(22L, "seller-owner"));

        assertSame(issue.getSession(), support.getSession("seller", issue.getResult().getToken()));
        assertSame(issue.getSession(), support.getSession("seller",
                Constants.TOKEN_PREFIX + issue.getResult().getToken()));
        assertNull(support.getSession("buyer", issue.getResult().getToken()));
    }

    @Test
    public void createLoginCopiesDirectLoginAuditIntoStoredSession()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        PortalTokenSupport support = support(redisCache);
        bindRequest(Map.of("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0"), "127.0.0.1");

        PortalDirectLoginToken directLoginToken = new PortalDirectLoginToken();
        directLoginToken.setTicketId(100L);
        directLoginToken.setActingAdminId(1L);
        directLoginToken.setActingAdminName("admin");
        directLoginToken.setDirectLoginReason("support check");

        PortalLoginIssue issue = support.createLogin("seller", 11L, "SAA010001",
                account(22L, "seller-owner"), directLoginToken);

        assertEquals(Boolean.TRUE, issue.getSession().getDirectLogin());
        assertEquals(Long.valueOf(100L), issue.getSession().getDirectLoginTicketId());
        assertEquals(Long.valueOf(1L), issue.getSession().getActingAdminId());
        assertEquals("admin", issue.getSession().getActingAdminName());
        assertEquals("support check", issue.getSession().getDirectLoginReason());
        assertSame(issue.getSession(), redisCache.store.get(redisCache.onlyStoredKey()));
    }

    @Test
    public void requireSessionRejectsCrossTerminalToken()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        PortalTokenSupport support = support(redisCache);
        bindRequest(Map.of("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0"), "127.0.0.1");
        PortalLoginIssue issue = support.createLogin("seller", 11L, "SAA010001", account(22L, "seller-owner"));
        bindRequest(Map.of(HEADER, Constants.TOKEN_PREFIX + issue.getResult().getToken()), "127.0.0.1");

        try
        {
            support.requireSession("buyer");
        }
        catch (ServiceException e)
        {
            assertEquals(Integer.valueOf(HttpStatus.UNAUTHORIZED), e.getCode());
            assertEquals("登录状态已失效", e.getMessage());
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void deleteLoginTokensUsesExplicitTerminalScope()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        PortalTokenSupport support = support(redisCache);

        support.deleteLoginTokens("buyer", List.of("buyer_token_a", "seller_token_b", ""));

        assertEquals(List.of("portal_login_tokens:buyer:buyer_token_a",
                "portal_login_tokens:buyer:seller_token_b"), redisCache.deletedKeys);
    }

    @Test
    public void deleteLoginTokenUsesSessionTerminalScope()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        PortalTokenSupport support = support(redisCache);
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("seller");
        session.setTokenId("seller_token_a");

        support.deleteLoginToken(session);

        assertEquals(List.of("portal_login_tokens:seller:seller_token_a"), redisCache.deletedKeys);
    }

    private PortalTokenSupport support(RecordingRedisCache redisCache)
    {
        PortalTokenSupport support = new PortalTokenSupport();
        setField(support, "secret", SECRET);
        setField(support, "header", HEADER);
        setField(support, "expireTime", 30);
        setField(support, "redisCache", redisCache);
        return support;
    }

    private PortalAccount account(Long accountId, String userName)
    {
        TestPortalAccount account = new TestPortalAccount();
        account.setAccountId(accountId);
        account.setUserName(userName);
        account.setNickName(userName);
        return account;
    }

    private void bindRequest(Map<String, String> headers, String remoteAddr)
    {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("getHeader".equals(methodName))
            {
                return headers.get((String) args[0]);
            }
            if ("getRemoteAddr".equals(methodName))
            {
                return remoteAddr;
            }
            if ("toString".equals(methodName))
            {
                return "PortalTokenSupportTestRequest";
            }
            if ("hashCode".equals(methodName))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName))
            {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        };
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[] { HttpServletRequest.class }, handler);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private Object defaultValue(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (returnType == boolean.class)
        {
            return false;
        }
        if (returnType == byte.class)
        {
            return (byte) 0;
        }
        if (returnType == short.class)
        {
            return (short) 0;
        }
        if (returnType == int.class)
        {
            return 0;
        }
        if (returnType == long.class)
        {
            return 0L;
        }
        if (returnType == float.class)
        {
            return 0F;
        }
        if (returnType == double.class)
        {
            return 0D;
        }
        if (returnType == char.class)
        {
            return '\0';
        }
        return null;
    }

    private void setField(Object target, String fieldName, Object value)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Unable to set field " + fieldName, e);
        }
    }

    private static class RecordingRedisCache extends RedisCache
    {
        private final Map<String, Object> store = new LinkedHashMap<>();

        private final Map<String, Integer> timeouts = new LinkedHashMap<>();

        private final Map<String, TimeUnit> units = new LinkedHashMap<>();

        private final List<String> deletedKeys = new ArrayList<>();

        @Override
        public <T> void setCacheObject(final String key, final T value, final Integer timeout,
                final TimeUnit timeUnit)
        {
            store.put(key, value);
            timeouts.put(key, timeout);
            units.put(key, timeUnit);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCacheObject(final String key)
        {
            return (T) store.get(key);
        }

        @Override
        public boolean deleteObject(final Collection collection)
        {
            deletedKeys.clear();
            for (Object key : collection)
            {
                deletedKeys.add(String.valueOf(key));
                store.remove(String.valueOf(key));
            }
            return !collection.isEmpty();
        }

        private String onlyStoredKey()
        {
            assertEquals(1, store.size());
            return store.keySet().iterator().next();
        }
    }

    private static class TestPortalAccount extends PortalAccount
    {
        private static final long serialVersionUID = 1L;
    }
}
