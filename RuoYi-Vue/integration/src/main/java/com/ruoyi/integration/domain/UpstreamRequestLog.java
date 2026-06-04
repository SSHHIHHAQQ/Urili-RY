package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 上游系统请求日志。
 */
public class UpstreamRequestLog
{
    private Long requestLogId;
    private String connectionCode;
    private String traceId;
    private String operation;
    private String endpoint;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date requestTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date responseTime;
    private Long durationMs;
    private String requestPayloadRedacted;
    private String responsePayloadRedacted;
    private String externalErrorCode;
    private String externalErrorMessage;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getRequestLogId() { return requestLogId; }
    public void setRequestLogId(Long requestLogId) { this.requestLogId = requestLogId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public Date getRequestTime() { return requestTime; }
    public void setRequestTime(Date requestTime) { this.requestTime = requestTime; }
    public Date getResponseTime() { return responseTime; }
    public void setResponseTime(Date responseTime) { this.responseTime = responseTime; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getRequestPayloadRedacted() { return requestPayloadRedacted; }
    public void setRequestPayloadRedacted(String requestPayloadRedacted) { this.requestPayloadRedacted = requestPayloadRedacted; }
    public String getResponsePayloadRedacted() { return responsePayloadRedacted; }
    public void setResponsePayloadRedacted(String responsePayloadRedacted) { this.responsePayloadRedacted = responsePayloadRedacted; }
    public String getExternalErrorCode() { return externalErrorCode; }
    public void setExternalErrorCode(String externalErrorCode) { this.externalErrorCode = externalErrorCode; }
    public String getExternalErrorMessage() { return externalErrorMessage; }
    public void setExternalErrorMessage(String externalErrorMessage) { this.externalErrorMessage = externalErrorMessage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
