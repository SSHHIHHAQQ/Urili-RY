package com.ruoyi.system.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.mapper.PortalOperLogMapper;
import com.ruoyi.system.service.ISysOperLogService;

public class PortalOperLogServiceImplTest
{
    @Test
    public void sellerTerminalWritesOnlySellerOperLog()
    {
        RecordingPortalOperLogMapper mapper = new RecordingPortalOperLogMapper();
        PortalOperLogServiceImpl service = service(mapper.proxy());
        PortalOperLog log = operLog(11L, 22L);

        service.insertOperLog("seller", log);

        assertSame(log, mapper.sellerOperLog);
        assertNull(mapper.buyerOperLog);
        assertEquals(1, mapper.sellerCalls);
        assertEquals(0, mapper.buyerCalls);
    }

    @Test
    public void buyerTerminalWritesOnlyBuyerOperLog()
    {
        RecordingPortalOperLogMapper mapper = new RecordingPortalOperLogMapper();
        PortalOperLogServiceImpl service = service(mapper.proxy());
        PortalOperLog log = operLog(33L, 44L);

        service.insertOperLog("buyer", log);

        assertSame(log, mapper.buyerOperLog);
        assertNull(mapper.sellerOperLog);
        assertEquals(0, mapper.sellerCalls);
        assertEquals(1, mapper.buyerCalls);
    }

    @Test
    public void unsupportedTerminalFailsWithoutWritingAnyPortalOperLog()
    {
        RecordingPortalOperLogMapper mapper = new RecordingPortalOperLogMapper();
        PortalOperLogServiceImpl service = service(mapper.proxy());

        assertThrows(ServiceException.class, () -> service.insertOperLog("admin", operLog(1L, 2L)));

        assertNull(mapper.sellerOperLog);
        assertNull(mapper.buyerOperLog);
        assertEquals(0, mapper.sellerCalls);
        assertEquals(0, mapper.buyerCalls);
    }

    @Test
    public void serviceDoesNotKeepSysOperLogDependency()
    {
        for (Field field : PortalOperLogServiceImpl.class.getDeclaredFields())
        {
            assertTrue("Portal oper log service must not write through sys_oper_log: " + field.getType().getName(),
                    field.getType() != ISysOperLogService.class && !field.getType().getName().contains("SysOperLog"));
        }
    }

    private PortalOperLogServiceImpl service(PortalOperLogMapper mapper)
    {
        PortalOperLogServiceImpl service = new PortalOperLogServiceImpl();
        setField(service, "portalOperLogMapper", mapper);
        return service;
    }

    private PortalOperLog operLog(Long subjectId, Long accountId)
    {
        PortalOperLog log = new PortalOperLog();
        log.setSubjectId(subjectId);
        log.setAccountId(accountId);
        log.setTitle("Portal action");
        log.setOperName("owner");
        return log;
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
            throw new AssertionError(e);
        }
    }

    private class RecordingPortalOperLogMapper implements InvocationHandler
    {
        private PortalOperLog sellerOperLog;

        private PortalOperLog buyerOperLog;

        private int sellerCalls;

        private int buyerCalls;

        private PortalOperLogMapper proxy()
        {
            return (PortalOperLogMapper) Proxy.newProxyInstance(
                    PortalOperLogMapper.class.getClassLoader(), new Class<?>[] { PortalOperLogMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            if ("insertSellerOperLog".equals(method.getName()))
            {
                sellerCalls++;
                sellerOperLog = (PortalOperLog) args[0];
                return null;
            }
            if ("insertBuyerOperLog".equals(method.getName()))
            {
                buyerCalls++;
                buyerOperLog = (PortalOperLog) args[0];
                return null;
            }
            if ("toString".equals(method.getName()))
            {
                return "RecordingPortalOperLogMapper";
            }
            if ("hashCode".equals(method.getName()))
            {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName()))
            {
                return proxy == args[0];
            }
            throw new AssertionError("Unexpected mapper method: " + method.getName());
        }
    }
}
