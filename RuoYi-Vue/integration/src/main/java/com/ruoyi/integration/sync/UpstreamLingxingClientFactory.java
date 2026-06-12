package com.ruoyi.integration.sync;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.lingxing.LingxingClientException;
import com.ruoyi.integration.lingxing.LingxingCredentials;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.lingxing.LingxingRequestLogEntry;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.system.service.support.SecretCipherSupport;

/**
 * 领星客户端创建与请求日志落库。
 */
@Component
public class UpstreamLingxingClientFactory
{
    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Autowired
    private SecretCipherSupport secretCipherSupport;

    @Autowired
    private UpstreamClockHealthGuard clockHealthGuard;

    public LingxingOpenApiClient createClient(UpstreamSystemConnection connection)
    {
        String appKey = secretCipherSupport.decrypt(connection.getAppKeyCiphertext());
        String appSecret = secretCipherSupport.decrypt(connection.getAppSecretCiphertext());
        return createClient(connection.getConnectionCode(), appKey, appSecret);
    }

    public LingxingOpenApiClient createClient(String connectionCode, String appKey, String appSecret)
    {
        clockHealthGuard.assertSystemClockHealthy();
        return new LingxingOpenApiClient(new LingxingCredentials(appKey, appSecret),
            entry -> insertRequestLog(connectionCode, entry));
    }

    public void checkWarehouseAccess(String connectionCode, String appKey, String appSecret)
    {
        LingxingOpenApiClient client = createClient(connectionCode, appKey, appSecret);
        try
        {
            client.checkWarehouseAccess(UUID.randomUUID().toString());
        }
        catch (RuntimeException ex)
        {
            throw toServiceException(ex);
        }
    }

    public ServiceException toServiceException(RuntimeException ex)
    {
        if (ex instanceof ServiceException serviceException)
        {
            return serviceException;
        }
        if (ex instanceof LingxingClientException clientException)
        {
            return new ServiceException("领星接口调用失败：" + clientException.getMessage());
        }
        return new ServiceException(StringUtils.defaultIfBlank(ex.getMessage(), "上游系统处理失败"));
    }

    private void insertRequestLog(String connectionCode, LingxingRequestLogEntry entry)
    {
        UpstreamRequestLog log = new UpstreamRequestLog();
        log.setRequestLogId(entry.getRequestLogId());
        log.setConnectionCode(connectionCode);
        log.setTraceId(entry.getTraceId());
        log.setOperation(entry.getOperation());
        log.setEndpoint(entry.getEndpoint());
        log.setRequestTime(entry.getRequestTime());
        log.setResponseTime(entry.getResponseTime());
        log.setDurationMs(entry.getDurationMs());
        log.setRequestPayloadRedacted(entry.getRequestPayloadRedacted());
        log.setResponsePayloadRedacted(entry.getResponsePayloadRedacted());
        log.setExternalErrorCode(entry.getExternalErrorCode());
        log.setExternalErrorMessage(entry.getExternalErrorMessage());
        log.setStatus(entry.getStatus());
        if (entry.getRequestLogId() == null)
        {
            upstreamSystemMapper.insertRequestLog(log);
            entry.setRequestLogId(log.getRequestLogId());
            return;
        }
        upstreamSystemMapper.updateRequestLog(log);
    }
}
