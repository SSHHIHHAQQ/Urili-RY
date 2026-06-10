package com.ruoyi.logistics.agg56;

import java.util.Date;

/**
 * AGG56 请求日志条目。
 */
public class Agg56RequestLogEntry
{
    private String traceId;

    private String operation;

    private String endpoint;

    private String httpMethod;

    private String businessOrderNo;

    private String providerOrderNo;

    private Date requestTime;

    private Date responseTime;

    private Long durationMs;

    private Integer httpStatus;

    private String providerCode;

    private String providerMessage;

    private String status;

    private String requestPayloadRedacted;

    private String responsePayloadRedacted;

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getHttpMethod()
    {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    public String getBusinessOrderNo()
    {
        return businessOrderNo;
    }

    public void setBusinessOrderNo(String businessOrderNo)
    {
        this.businessOrderNo = businessOrderNo;
    }

    public String getProviderOrderNo()
    {
        return providerOrderNo;
    }

    public void setProviderOrderNo(String providerOrderNo)
    {
        this.providerOrderNo = providerOrderNo;
    }

    public Date getRequestTime()
    {
        return requestTime;
    }

    public void setRequestTime(Date requestTime)
    {
        this.requestTime = requestTime;
    }

    public Date getResponseTime()
    {
        return responseTime;
    }

    public void setResponseTime(Date responseTime)
    {
        this.responseTime = responseTime;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public Integer getHttpStatus()
    {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus)
    {
        this.httpStatus = httpStatus;
    }

    public String getProviderCode()
    {
        return providerCode;
    }

    public void setProviderCode(String providerCode)
    {
        this.providerCode = providerCode;
    }

    public String getProviderMessage()
    {
        return providerMessage;
    }

    public void setProviderMessage(String providerMessage)
    {
        this.providerMessage = providerMessage;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getRequestPayloadRedacted()
    {
        return requestPayloadRedacted;
    }

    public void setRequestPayloadRedacted(String requestPayloadRedacted)
    {
        this.requestPayloadRedacted = requestPayloadRedacted;
    }

    public String getResponsePayloadRedacted()
    {
        return responsePayloadRedacted;
    }

    public void setResponsePayloadRedacted(String responsePayloadRedacted)
    {
        this.responsePayloadRedacted = responsePayloadRedacted;
    }
}
