package com.ruoyi.seller.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.system.domain.PortalAccount;
import com.ruoyi.system.domain.PortalDirectLoginResult;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalDirectLoginSupport;

public class SellerServiceImplTest
{
    @Test
    public void createSellerAccountDirectLoginUsesSelectedSellerAccount()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        SellerServiceImpl service = service(mapper(seller, account), directLoginSupport);

        PortalDirectLoginResult result = service.createSellerAccountDirectLogin(11L, 22L, "support check");

        assertEquals(Long.valueOf(22L), result.getAccountId());
        assertEquals("seller-staff", result.getUsername());
        assertEquals(1, directLoginSupport.callCount);
        assertEquals("seller", directLoginSupport.portalType);
        assertEquals(Long.valueOf(11L), directLoginSupport.partnerId);
        assertEquals("SAA010001", directLoginSupport.partnerNo);
        assertSame(account, directLoginSupport.account);
        assertEquals("support check", directLoginSupport.reason);
        assertEquals(PortalDirectLoginSupport.SELLER_WEB_URL_CONFIG_KEY, directLoginSupport.webUrlConfigKey);
        assertEquals("http://127.0.0.1:8001/seller/direct-login", directLoginSupport.fallbackWebUrl);
    }

    @Test
    public void createSellerAccountDirectLoginRejectsAccountFromAnotherSeller()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 99L, "other-seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        SellerServiceImpl service = service(mapper(seller, account), directLoginSupport);

        try
        {
            service.createSellerAccountDirectLogin(11L, 22L, "support check");
        }
        catch (ServiceException e)
        {
            assertEquals("卖家账号不存在", e.getMessage());
            assertEquals(0, directLoginSupport.callCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void createSellerAccountDirectLoginRejectsDisabledSeller()
    {
        Seller seller = seller(11L, "SAA010001", "1");
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingDirectLoginSupport directLoginSupport = new RecordingDirectLoginSupport();
        SellerServiceImpl service = service(mapper(seller, account), directLoginSupport);

        try
        {
            service.createSellerAccountDirectLogin(11L, 22L, "support check");
        }
        catch (ServiceException e)
        {
            assertEquals("卖家已停用，不能免密登录", e.getMessage());
            assertEquals(0, directLoginSupport.callCount);
            return;
        }
        throw new AssertionError("Expected ServiceException");
    }

    @Test
    public void selectSellerOwnLoginLogListUsesSessionScopeAndIgnoresClientScope()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginSession session = session(11L, 22L);
        PortalLoginLog request = new PortalLoginLog();
        request.setSubjectId(99L);
        request.setAccountId(88L);
        request.setUserName("staff");
        request.setIpaddr("127.0.0.1");
        request.setStatus(Constants.SUCCESS);
        request.getParams().put("beginTime", "2026-06-01");
        request.getParams().put("unexpected", "ignored");

        List<PortalLoginLog> result = service.selectSellerOwnLoginLogList(session, request);

        assertSame(mapper.loginLogResult, result);
        assertEquals(Long.valueOf(11L), mapper.loginLogQuery.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.loginLogQuery.getAccountId());
        assertEquals("staff", mapper.loginLogQuery.getUserName());
        assertEquals("127.0.0.1", mapper.loginLogQuery.getIpaddr());
        assertEquals(Constants.SUCCESS, mapper.loginLogQuery.getStatus());
        assertEquals("2026-06-01", mapper.loginLogQuery.getParams().get("beginTime"));
        assertEquals(null, mapper.loginLogQuery.getParams().get("unexpected"));
        assertEquals(Long.valueOf(99L), request.getSubjectId());
        assertEquals(Long.valueOf(88L), request.getAccountId());
    }

    @Test
    public void selectSellerOwnOperLogListUsesSessionScopeAndIgnoresClientScope()
    {
        Seller seller = seller(11L, "SAA010001", PartnerSupport.STATUS_NORMAL);
        SellerAccount account = account(22L, 11L, "seller-staff", PartnerSupport.STATUS_NORMAL);
        RecordingSellerMapper mapper = recordingMapper(seller, account);
        SellerServiceImpl service = service(mapper.proxy(), new RecordingDirectLoginSupport());
        PortalLoginSession session = session(11L, 22L);
        PortalOperLog request = new PortalOperLog();
        request.setSubjectId(99L);
        request.setAccountId(88L);
        request.setTitle("订单");
        request.setOperName("staff");
        request.setStatus(0);
        request.getParams().put("endTime", "2026-06-05");
        request.getParams().put("unexpected", "ignored");

        List<PortalOperLog> result = service.selectSellerOwnOperLogList(session, request);

        assertSame(mapper.operLogResult, result);
        assertEquals(Long.valueOf(11L), mapper.operLogQuery.getSubjectId());
        assertEquals(Long.valueOf(22L), mapper.operLogQuery.getAccountId());
        assertEquals("订单", mapper.operLogQuery.getTitle());
        assertEquals("staff", mapper.operLogQuery.getOperName());
        assertEquals(Integer.valueOf(0), mapper.operLogQuery.getStatus());
        assertEquals("2026-06-05", mapper.operLogQuery.getParams().get("endTime"));
        assertEquals(null, mapper.operLogQuery.getParams().get("unexpected"));
        assertEquals(Long.valueOf(99L), request.getSubjectId());
        assertEquals(Long.valueOf(88L), request.getAccountId());
    }

    private SellerServiceImpl service(SellerMapper mapper, PortalDirectLoginSupport directLoginSupport)
    {
        SellerServiceImpl service = new SellerServiceImpl();
        setField(service, "sellerMapper", mapper);
        setField(service, "directLoginSupport", directLoginSupport);
        return service;
    }

    private Seller seller(Long sellerId, String sellerNo, String status)
    {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setSellerNo(sellerNo);
        seller.setStatus(status);
        return seller;
    }

    private SellerAccount account(Long accountId, Long sellerId, String userName, String status)
    {
        SellerAccount account = new SellerAccount();
        account.setSellerAccountId(accountId);
        account.setAccountId(accountId);
        account.setSellerId(sellerId);
        account.setUserName(userName);
        account.setStatus(status);
        return account;
    }

    private PortalLoginSession session(Long sellerId, Long accountId)
    {
        PortalLoginSession session = new PortalLoginSession();
        session.setTerminal("seller");
        session.setSubjectId(sellerId);
        session.setAccountId(accountId);
        session.setTokenId("seller_test_token");
        return session;
    }

    private SellerMapper mapper(Seller seller, SellerAccount... accounts)
    {
        return recordingMapper(seller, accounts).proxy();
    }

    private RecordingSellerMapper recordingMapper(Seller seller, SellerAccount... accounts)
    {
        return new RecordingSellerMapper(seller, accounts);
    }

    private class RecordingSellerMapper implements InvocationHandler
    {
        private final Seller seller;

        private final Map<Long, SellerAccount> accountById = new HashMap<>();

        private final List<PortalLoginLog> loginLogResult = new ArrayList<>();

        private final List<PortalOperLog> operLogResult = new ArrayList<>();

        private PortalLoginLog loginLogQuery;

        private PortalOperLog operLogQuery;

        private RecordingSellerMapper(Seller seller, SellerAccount... accounts)
        {
            this.seller = seller;
            for (SellerAccount account : accounts)
            {
                accountById.put(account.getSellerAccountId(), account);
            }
        }

        private SellerMapper proxy()
        {
            return (SellerMapper) Proxy.newProxyInstance(
                SellerMapper.class.getClassLoader(), new Class<?>[] { SellerMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            String methodName = method.getName();
            if ("selectSellerById".equals(methodName))
            {
                Long sellerId = (Long) args[0];
                return seller != null && sellerId.equals(seller.getSellerId()) ? seller : null;
            }
            if ("selectSellerAccountById".equals(methodName))
            {
                return accountById.get((Long) args[0]);
            }
            if ("selectSellerLoginLogList".equals(methodName))
            {
                loginLogQuery = (PortalLoginLog) args[0];
                return loginLogResult;
            }
            if ("selectSellerOperLogList".equals(methodName))
            {
                operLogQuery = (PortalOperLog) args[0];
                return operLogResult;
            }
            if ("toString".equals(methodName))
            {
                return "SellerServiceImplTestMapper";
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
        }
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

    private static class RecordingDirectLoginSupport extends PortalDirectLoginSupport
    {
        private int callCount;

        private String portalType;

        private Long partnerId;

        private String partnerNo;

        private PortalAccount account;

        private String reason;

        private String webUrlConfigKey;

        private String fallbackWebUrl;

        @Override
        public PortalDirectLoginResult createToken(String portalType, Long partnerId, String partnerNo,
                PortalAccount account, String reason, String webUrlConfigKey, String fallbackWebUrl)
        {
            this.callCount++;
            this.portalType = portalType;
            this.partnerId = partnerId;
            this.partnerNo = partnerNo;
            this.account = account;
            this.reason = reason;
            this.webUrlConfigKey = webUrlConfigKey;
            this.fallbackWebUrl = fallbackWebUrl;

            PortalDirectLoginResult result = new PortalDirectLoginResult();
            result.setToken(portalType + "_test_token");
            result.setTicketId(1L);
            result.setLoginUrl(fallbackWebUrl + "?directLoginToken=" + result.getToken());
            result.setExpireMinutes(30);
            result.setExpireTime(new Date());
            result.setAccountId(account.getAccountId());
            result.setUsername(account.getUserName());
            return result;
        }
    }
}
