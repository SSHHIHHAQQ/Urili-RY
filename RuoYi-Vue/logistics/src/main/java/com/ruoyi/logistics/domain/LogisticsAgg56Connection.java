package com.ruoyi.logistics.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * AGG56 私有接入信息。
 */
public class LogisticsAgg56Connection implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long carrierAccountId;

    @JsonIgnore
    private String connectionCode;

    private String appTokenMask;

    private String appKeyMask;

    @JsonIgnore
    private String appTokenCiphertext;

    @JsonIgnore
    private String appKeyCiphertext;

    private String credentialKeyId;

    private String agg56UserId;

    private String agg56UserAccountMask;

    private String agg56CustomerCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getCarrierAccountId()
    {
        return carrierAccountId;
    }

    public void setCarrierAccountId(Long carrierAccountId)
    {
        this.carrierAccountId = carrierAccountId;
    }

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getAppTokenMask()
    {
        return appTokenMask;
    }

    public void setAppTokenMask(String appTokenMask)
    {
        this.appTokenMask = appTokenMask;
    }

    public String getAppKeyMask()
    {
        return appKeyMask;
    }

    public void setAppKeyMask(String appKeyMask)
    {
        this.appKeyMask = appKeyMask;
    }

    public String getAppTokenCiphertext()
    {
        return appTokenCiphertext;
    }

    public void setAppTokenCiphertext(String appTokenCiphertext)
    {
        this.appTokenCiphertext = appTokenCiphertext;
    }

    public String getAppKeyCiphertext()
    {
        return appKeyCiphertext;
    }

    public void setAppKeyCiphertext(String appKeyCiphertext)
    {
        this.appKeyCiphertext = appKeyCiphertext;
    }

    public String getCredentialKeyId()
    {
        return credentialKeyId;
    }

    public void setCredentialKeyId(String credentialKeyId)
    {
        this.credentialKeyId = credentialKeyId;
    }

    public String getAgg56UserId()
    {
        return agg56UserId;
    }

    public void setAgg56UserId(String agg56UserId)
    {
        this.agg56UserId = agg56UserId;
    }

    public String getAgg56UserAccountMask()
    {
        return agg56UserAccountMask;
    }

    public void setAgg56UserAccountMask(String agg56UserAccountMask)
    {
        this.agg56UserAccountMask = agg56UserAccountMask;
    }

    public String getAgg56CustomerCode()
    {
        return agg56CustomerCode;
    }

    public void setAgg56CustomerCode(String agg56CustomerCode)
    {
        this.agg56CustomerCode = agg56CustomerCode;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
