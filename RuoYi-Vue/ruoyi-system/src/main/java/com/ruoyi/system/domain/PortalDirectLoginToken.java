package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Redis payload for one-time portal direct-login.
 */
public class PortalDirectLoginToken implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long ticketId;

    private String portalType;

    private Long partnerId;

    private String partnerNo;

    private Long accountId;

    private String username;

    private Long actingAdminId;

    private String actingAdminName;

    private String directLoginReason;

    private String createBy;

    private Date createTime;

    private Date expireTime;

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

    public Long getActingAdminId()
    {
        return actingAdminId;
    }

    public void setActingAdminId(Long actingAdminId)
    {
        this.actingAdminId = actingAdminId;
    }

    public String getActingAdminName()
    {
        return actingAdminName;
    }

    public void setActingAdminName(String actingAdminName)
    {
        this.actingAdminName = actingAdminName;
    }

    public String getDirectLoginReason()
    {
        return directLoginReason;
    }

    public void setDirectLoginReason(String directLoginReason)
    {
        this.directLoginReason = directLoginReason;
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
