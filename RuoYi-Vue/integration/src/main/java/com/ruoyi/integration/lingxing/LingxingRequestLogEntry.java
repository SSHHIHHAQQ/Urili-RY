package com.ruoyi.integration.lingxing;

import java.util.Date;

/**
 * 领星请求日志条目。
 */
public class LingxingRequestLogEntry
{
    private Long requestLogId;
    private String traceId;
    private String operation;
    private String endpoint;
    private Date requestTime;
    private Date responseTime;
    private Long durationMs;
    private String requestPayloadRedacted;
    private String responsePayloadRedacted;
    private String externalErrorCode;
    private String externalErrorMessage;
    private String status;

    public Long getRequestLogId() { return requestLogId; }
    public void setRequestLogId(Long requestLogId) { this.requestLogId = requestLogId; }
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
}
