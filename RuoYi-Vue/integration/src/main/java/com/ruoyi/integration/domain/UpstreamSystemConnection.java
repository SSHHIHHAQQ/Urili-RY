package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 上游系统主仓接入。
 */
public class UpstreamSystemConnection extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String connectionCode;

    private String systemKind;

    private String masterWarehouseName;

    private String settlementType;

    private String appKeyMask;

    private String appSecretMask;

    @JsonIgnore
    private String appKeyCiphertext;

    @JsonIgnore
    private String appSecretCiphertext;

    private String credentialKeyId;

    private String status;

    private String credentialStatus;

    private String enabledCapabilities;

    private Integer displayOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastAuthorizedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncTime;

    private Integer requestLogCount;

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getSystemKind()
    {
        return systemKind;
    }

    public void setSystemKind(String systemKind)
    {
        this.systemKind = systemKind;
    }

    public String getMasterWarehouseName()
    {
        return masterWarehouseName;
    }

    public void setMasterWarehouseName(String masterWarehouseName)
    {
        this.masterWarehouseName = masterWarehouseName;
    }

    public String getSettlementType()
    {
        return settlementType;
    }

    public void setSettlementType(String settlementType)
    {
        this.settlementType = settlementType;
    }

    public String getAppKeyMask()
    {
        return appKeyMask;
    }

    public void setAppKeyMask(String appKeyMask)
    {
        this.appKeyMask = appKeyMask;
    }

    public String getAppSecretMask()
    {
        return appSecretMask;
    }

    public void setAppSecretMask(String appSecretMask)
    {
        this.appSecretMask = appSecretMask;
    }

    public String getAppKeyCiphertext()
    {
        return appKeyCiphertext;
    }

    public void setAppKeyCiphertext(String appKeyCiphertext)
    {
        this.appKeyCiphertext = appKeyCiphertext;
    }

    public String getAppSecretCiphertext()
    {
        return appSecretCiphertext;
    }

    public void setAppSecretCiphertext(String appSecretCiphertext)
    {
        this.appSecretCiphertext = appSecretCiphertext;
    }

    public String getCredentialKeyId()
    {
        return credentialKeyId;
    }

    public void setCredentialKeyId(String credentialKeyId)
    {
        this.credentialKeyId = credentialKeyId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getCredentialStatus()
    {
        return credentialStatus;
    }

    public void setCredentialStatus(String credentialStatus)
    {
        this.credentialStatus = credentialStatus;
    }

    public String getEnabledCapabilities()
    {
        return enabledCapabilities;
    }

    public void setEnabledCapabilities(String enabledCapabilities)
    {
        this.enabledCapabilities = enabledCapabilities;
    }

    public Integer getDisplayOrder()
    {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder)
    {
        this.displayOrder = displayOrder;
    }

    public Date getLastAuthorizedTime()
    {
        return lastAuthorizedTime;
    }

    public void setLastAuthorizedTime(Date lastAuthorizedTime)
    {
        this.lastAuthorizedTime = lastAuthorizedTime;
    }

    public Date getLastSyncTime()
    {
        return lastSyncTime;
    }

    public void setLastSyncTime(Date lastSyncTime)
    {
        this.lastSyncTime = lastSyncTime;
    }

    public Integer getRequestLogCount()
    {
        return requestLogCount;
    }

    public void setRequestLogCount(Integer requestLogCount)
    {
        this.requestLogCount = requestLogCount;
    }
}
