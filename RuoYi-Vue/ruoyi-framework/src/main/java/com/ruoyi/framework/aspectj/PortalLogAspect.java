package com.ruoyi.framework.aspectj;

import java.util.Collection;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.BusinessStatus;
import com.ruoyi.common.enums.HttpMethod;
import com.ruoyi.common.filter.PropertyPreExcludeFilter;
import com.ruoyi.common.utils.ExceptionUtil;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.service.support.PortalSessionContext;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.service.support.PortalTokenSupport;

/**
 * Seller/buyer portal operation log aspect.
 */
@Aspect
@Component
public class PortalLogAspect
{
    private static final Logger log = LoggerFactory.getLogger(PortalLogAspect.class);

    /** Excluded sensitive properties. */
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };

    /** Cost time holder. */
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<Long>("Portal Cost Time");

    /** Max request/response field length. */
    private static final int PARAM_MAX_LENGTH = 2000;

    private final PortalTokenSupport portalTokenSupport;

    public PortalLogAspect(PortalTokenSupport portalTokenSupport)
    {
        this.portalTokenSupport = portalTokenSupport;
    }

    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, PortalLog controllerLog)
    {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, PortalLog controllerLog, Object jsonResult)
    {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, PortalLog controllerLog, Exception e)
    {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, PortalLog controllerLog, final Exception e, Object jsonResult)
    {
        try
        {
            PortalLoginSession session = PortalSessionContext.getSession(controllerLog.terminal());
            if (session == null)
            {
                session = portalTokenSupport.getSession(controllerLog.terminal());
            }
            if (session == null)
            {
                return;
            }

            PortalOperLog operLog = new PortalOperLog();
            operLog.setSubjectId(session.getSubjectId());
            operLog.setAccountId(session.getAccountId());
            operLog.setOperName(session.getUserName());
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            operLog.setOperIp(IpUtils.getIpAddr());
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(), 0, 255));

            if (e != null)
            {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(Convert.toStr(e.getMessage(), ExceptionUtil.getExceptionMessage(e)), 0, 2000));
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);

            Long startTime = TIME_THREADLOCAL.get();
            operLog.setCostTime(startTime == null ? 0L : System.currentTimeMillis() - startTime);
            AsyncManager.me().execute(AsyncFactory.recordPortalOper(controllerLog.terminal(), operLog));
        }
        catch (Exception exp)
        {
            log.error("端内操作日志记录异常:{}", exp.getMessage());
            exp.printStackTrace();
        }
        finally
        {
            TIME_THREADLOCAL.remove();
        }
    }

    public void getControllerMethodDescription(JoinPoint joinPoint, PortalLog portalLog, PortalOperLog operLog, Object jsonResult) throws Exception
    {
        operLog.setBusinessType(portalLog.businessType().ordinal());
        operLog.setTitle(portalLog.title());
        if (portalLog.isSaveRequestData())
        {
            setRequestValue(joinPoint, operLog, portalLog.excludeParamNames());
        }
        if (portalLog.isSaveResponseData() && StringUtils.isNotNull(jsonResult))
        {
            operLog.setJsonResult(StringUtils.substring(JSON.toJSONString(jsonResult), 0, 2000));
        }
    }

    private void setRequestValue(JoinPoint joinPoint, PortalOperLog operLog, String[] excludeParamNames) throws Exception
    {
        String requestMethod = operLog.getRequestMethod();
        Map<?, ?> paramsMap = ServletUtils.getParamMap(ServletUtils.getRequest());
        if (StringUtils.isEmpty(paramsMap) && StringUtils.equalsAny(requestMethod, HttpMethod.PUT.name(), HttpMethod.POST.name(), HttpMethod.DELETE.name()))
        {
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(params);
        }
        else
        {
            operLog.setOperParam(StringUtils.substring(JSON.toJSONString(paramsMap, excludePropertyPreFilter(excludeParamNames)), 0, PARAM_MAX_LENGTH));
        }
    }

    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames)
    {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0)
        {
            for (Object o : paramsArray)
            {
                if (StringUtils.isNotNull(o) && !isFilterObject(o))
                {
                    try
                    {
                        String jsonObj = JSON.toJSONString(o, excludePropertyPreFilter(excludeParamNames));
                        params.append(jsonObj).append(" ");
                        if (params.length() >= PARAM_MAX_LENGTH)
                        {
                            return StringUtils.substring(params.toString(), 0, PARAM_MAX_LENGTH);
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("端内请求参数拼装异常 msg:{}, 参数:{}", e.getMessage(), paramsArray, e);
                    }
                }
            }
        }
        return params.toString();
    }

    public PropertyPreExcludeFilter excludePropertyPreFilter(String[] excludeParamNames)
    {
        return new PropertyPreExcludeFilter().addExcludes(ArrayUtils.addAll(EXCLUDE_PROPERTIES, excludeParamNames));
    }

    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o)
    {
        Class<?> clazz = o.getClass();
        if (clazz.isArray())
        {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        }
        else if (Collection.class.isAssignableFrom(clazz))
        {
            Collection collection = (Collection) o;
            for (Object value : collection)
            {
                return value instanceof MultipartFile;
            }
        }
        else if (Map.class.isAssignableFrom(clazz))
        {
            Map map = (Map) o;
            for (Object value : map.entrySet())
            {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }
}
