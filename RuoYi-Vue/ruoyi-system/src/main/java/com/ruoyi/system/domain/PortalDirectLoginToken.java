package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Redis payload for one-time portal direct-login.
 */
public class PortalDirectLoginToken implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String token;

    private Long ticketId;

    private String portalType;

    private Long partnerId;

    private String partnerNo;

    private Long accountId;

    private String username;

    private String createBy;

    private Date createTime;

    private Date expireTime;

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    public String getPortalType()
    {
        return portalType;
    }

    public void setPortalType(String portalType)
    {
        this.portalType = portalType;
    }

    public Long getPartnerId()
    {
        return partnerId;
    }

    public void setPartnerId(Long partnerId)
    {
        this.partnerId = partnerId;
    }

    public String getPartnerNo()
    {
        return partnerNo;
    }

    public void setPartnerNo(String partnerNo)
    {
        this.partnerNo = partnerNo;
    }

    public Long getAccountId()
    {
        return accountId;
    }

    public void setAccountId(Long accountId)
    {
        this.accountId = accountId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }
}
