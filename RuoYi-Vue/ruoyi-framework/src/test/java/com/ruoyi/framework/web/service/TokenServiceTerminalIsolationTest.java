package com.ruoyi.framework.web.service;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.support.PortalTokenSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;

public class TokenServiceTerminalIsolationTest
{
    private static final String HEADER = "Authorization";

    private static final String SECRET = "token-service-terminal-isolation-secret";

    @Test
    public void adminTokenServiceRejectsPortalTokenWithoutAdminLoginClaim()
    {
        RecordingRedisCache redisCache = new RecordingRedisCache();
        TokenService tokenService = tokenService(redisCache);
        HttpServletRequest request = request(Constants.TOKEN_PREFIX + portalJwt());

        assertNull(tokenService.getLoginUser(request));
        assertTrue(redisCache.requestedKeys.isEmpty());
    }

    private TokenService tokenService(RecordingRedisCache redisCache)
    {
        TokenService tokenService = new TokenService();
        setField(tokenService, "header", HEADER);
        setField(tokenService, "secret", SECRET);
        setField(tokenService, "expireTime", 30);
        setField(tokenService, "redisCache", redisCache);
        return tokenService;
    }

    private String portalJwt()
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put(PortalTokenSupport.CLAIM_LOGIN_KEY, "seller_test_token");
        claims.put(PortalTokenSupport.CLAIM_TERMINAL, "seller");
        claims.put(Constants.JWT_USERNAME, "seller-owner");
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    private HttpServletRequest request(String authorization)
    {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if ("getHeader".equals(method.getName()) && args != null && args.length == 1)
            {
                return HEADER.equals(args[0]) ? authorization : null;
            }
            if ("toString".equals(method.getName()))
            {
                return "TokenServiceTerminalIsolationTestRequest";
            }
            if ("hashCode".equals(method.getName()))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName()))
            {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        };
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[] { HttpServletRequest.class }, handler);
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
        private final List<String> requestedKeys = new ArrayList<>();

        @Override
        public <T> T getCacheObject(final String key)
        {
            requestedKeys.add(key);
            return null;
        }
    }
}
