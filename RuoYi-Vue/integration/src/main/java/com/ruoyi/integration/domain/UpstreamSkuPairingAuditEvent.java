package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * SKU 配对审计事件。
 */
public class UpstreamSkuPairingAuditEvent
{
    private Long auditEventId;
    private String connectionCode;
    private String masterSku;
    private String systemSku;
    private String eventType;
    private String operator;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date eventTime;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String remark;

    public Long getAuditEventId() { return auditEventId; }
    public void setAuditEventId(Long auditEventId) { this.auditEventId = auditEventId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getSystemSku() { return systemSku; }
    public void setSystemSku(String systemSku) { this.systemSku = systemSku; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
    public String getBeforeSnapshot() { return beforeSnapshot; }
    public void setBeforeSnapshot(String beforeSnapshot) { this.beforeSnapshot = beforeSnapshot; }
    public String getAfterSnapshot() { return afterSnapshot; }
    public void setAfterSnapshot(String afterSnapshot) { this.afterSnapshot = afterSnapshot; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
