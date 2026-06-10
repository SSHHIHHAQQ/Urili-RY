package com.ruoyi.framework.aspectj;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.BusinessStatus;
import com.ruoyi.common.enums.HttpMethod;
import com.ruoyi.common.filter.PropertyPreExcludeFilter;
import com.ruoyi.common.utils.ExceptionUtil;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.ImageResourceUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.PortalLoginResult;
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
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword",
            "token", "jwt", "directLoginToken", "loginUrl", "accessToken", "refreshToken", "authorization",
            "appKey", "appSecret", "credential", "credentialCiphertext", "appKeyCiphertext",
            "appSecretCiphertext",
            "subjectId", "accountId", "sellerId", "buyerId", "sellerAccountId", "buyerAccountId",
            "directLoginTicketId", "actingAdminId", "actingAdminName", "directLoginReason", "terminal",
            "tokenId", "operParam", "jsonResult" };

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
            PortalLoginSession session = null;
            if (controllerLog.allowAnonymous())
            {
                session = resolveSessionFromLoginResult(controllerLog.terminal(), jsonResult);
            }
            else
            {
                session = PortalSessionContext.getSession(controllerLog.terminal());
                if (session == null)
                {
                    session = portalTokenSupport.getSession(controllerLog.terminal());
                }
            }

            PortalOperLog operLog = new PortalOperLog();
            if (session != null)
            {
                operLog.setSubjectId(session.getSubjectId());
                operLog.setAccountId(session.getAccountId());
                operLog.setOperName(session.getUserName());
            }
            else
            {
                operLog.setOperName("anonymous");
            }
            boolean anonymousAllowed = controllerLog.allowAnonymous();
            operLog.setStatus(e == null && (session != null || anonymousAllowed) ? BusinessStatus.SUCCESS.ordinal() : BusinessStatus.FAIL.ordinal());
            operLog.setOperIp(IpUtils.getIpAddr());
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(), 0, 255));

            if (e != null)
            {
                operLog.setErrorMsg(StringUtils.substring(Convert.toStr(e.getMessage(), ExceptionUtil.getExceptionMessage(e)), 0, 2000));
            }
            else if (session == null && !anonymousAllowed)
            {
                operLog.setErrorMsg("端内会话不存在");
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);

            Long startTime = TIME_THREADLOCAL.get();
            operLog.setCostTime(startTime == null ? 0L : System.currentTimeMillis() - startTime);
            applyDirectLoginAudit(operLog, session);
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
            operLog.setJsonResult(sanitizeLogText(
                    JSON.toJSONString(jsonResult, excludePropertyPreFilter(portalLog.excludeParamNames()))));
        }
    }

    private void setRequestValue(JoinPoint joinPoint, PortalOperLog operLog, String[] excludeParamNames) throws Exception
    {
        String requestMethod = operLog.getRequestMethod();
        Map<?, ?> paramsMap = ServletUtils.getParamMap(ServletUtils.getRequest());
        if (StringUtils.isEmpty(paramsMap) && StringUtils.equalsAny(requestMethod, HttpMethod.PUT.name(), HttpMethod.POST.name(), HttpMethod.DELETE.name()))
        {
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(sanitizeLogText(params));
        }
        else
        {
            Map<?, ?> filteredParamsMap = filterRequestParamMap(paramsMap, excludeParamNames);
            operLog.setOperParam(sanitizeLogText(JSON.toJSONString(filteredParamsMap,
                    excludePropertyPreFilter(excludeParamNames))));
        }
    }

    private void applyDirectLoginAudit(PortalOperLog operLog, PortalLoginSession session)
    {
        operLog.setDirectLogin(Boolean.FALSE);
        if (session == null || !Boolean.TRUE.equals(session.getDirectLogin()))
        {
            return;
        }
        operLog.setDirectLogin(Boolean.TRUE);
        operLog.setDirectLoginTicketId(session.getDirectLoginTicketId());
        operLog.setActingAdminId(session.getActingAdminId());
        operLog.setActingAdminName(session.getActingAdminName());
        operLog.setDirectLoginReason(session.getDirectLoginReason());
        appendDirectLoginAuditParam(operLog, session);
    }

    private void appendDirectLoginAuditParam(PortalOperLog operLog, PortalLoginSession session)
    {
        if (session == null || !Boolean.TRUE.equals(session.getDirectLogin()))
        {
            return;
        }
        String auditPrefix = "directLoginAudit{ticketId=" + session.getDirectLoginTicketId()
                + ", actingAdminId=" + session.getActingAdminId()
                + ", actingAdminName=" + safeAuditValue(session.getActingAdminName())
                + ", reason=" + safeAuditValue(session.getDirectLoginReason()) + "} ";
        operLog.setOperParam(StringUtils.substring(auditPrefix + safeAuditValue(operLog.getOperParam()), 0,
                PARAM_MAX_LENGTH));
    }

    private PortalLoginSession resolveSessionFromLoginResult(String expectedTerminal, Object jsonResult)
    {
        if (!(jsonResult instanceof AjaxResult))
        {
            return null;
        }
        Object data = ((AjaxResult) jsonResult).get(AjaxResult.DATA_TAG);
        if (!(data instanceof PortalLoginResult))
        {
            return null;
        }
        PortalLoginResult loginResult = (PortalLoginResult) data;
        if (!StringUtils.equals(expectedTerminal, loginResult.getTerminal()))
        {
            return null;
        }
        return portalTokenSupport.getSession(expectedTerminal, loginResult.getToken());
    }

    private String safeAuditValue(String value)
    {
        return StringUtils.isBlank(value) ? "" : value;
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
                        String jsonObj = sanitizeLogText(JSON.toJSONString(o, excludePropertyPreFilter(excludeParamNames)));
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

    private String sanitizeLogText(String value)
    {
        return StringUtils.substring(ImageResourceUtils.redactInlineImagePayloads(value), 0, PARAM_MAX_LENGTH);
    }

    public PropertyPreExcludeFilter excludePropertyPreFilter(String[] excludeParamNames)
    {
        return new PropertyPreExcludeFilter().addExcludes(ArrayUtils.addAll(EXCLUDE_PROPERTIES, excludeParamNames));
    }

    private Map<?, ?> filterRequestParamMap(Map<?, ?> paramsMap, String[] excludeParamNames)
    {
        if (StringUtils.isEmpty(paramsMap))
        {
            return paramsMap;
        }
        Set<String> excludedNames = new HashSet<>();
        for (String name : ArrayUtils.addAll(EXCLUDE_PROPERTIES, excludeParamNames))
        {
            if (StringUtils.isNotEmpty(name))
            {
                excludedNames.add(name.toLowerCase(Locale.ROOT));
            }
        }
        Map<Object, Object> filteredParamsMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : paramsMap.entrySet())
        {
            Object key = entry.getKey();
            if (key != null && excludedNames.contains(String.valueOf(key).toLowerCase(Locale.ROOT)))
            {
                continue;
            }
            filteredParamsMap.put(key, entry.getValue());
        }
        return filteredParamsMap;
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
