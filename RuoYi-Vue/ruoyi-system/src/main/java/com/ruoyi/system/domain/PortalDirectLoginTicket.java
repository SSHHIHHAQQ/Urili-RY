package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Auditable one-time direct-login ticket for seller/buyer portals.
 */
public class PortalDirectLoginTicket implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long ticketId;

    private String terminal;

    private Long targetSubjectId;

    private String targetSubjectNo;

    private Long targetAccountId;

    private String targetUserName;

    private Long actingAdminId;

    private String actingAdminName;

    private String reason;

    private String tokenHash;

    private Date expireTime;

    private Date usedTime;

    private String usedIp;

    private String status;

    private String createBy;

    private Date createTime;

    private String updateBy;

    private Date updateTime;

    private String remark;

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    public String getTerminal()
    {
        return terminal;
    }

    public void setTerminal(String terminal)
    {
        this.terminal = terminal;
    }

    public Long getTargetSubjectId()
    {
        return targetSubjectId;
    }

    public void setTargetSubjectId(Long targetSubjectId)
    {
        this.targetSubjectId = targetSubjectId;
    }

    public String getTargetSubjectNo()
    {
        return targetSubjectNo;
    }

    public void setTargetSubjectNo(String targetSubjectNo)
    {
        this.targetSubjectNo = targetSubjectNo;
    }

    public Long getTargetAccountId()
    {
        return targetAccountId;
    }

    public void setTargetAccountId(Long targetAccountId)
    {
        this.targetAccountId = targetAccountId;
    }

    public String getTargetUserName()
    {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName)
    {
        this.targetUserName = targetUserName;
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

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getTokenHash()
    {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash)
    {
        this.tokenHash = tokenHash;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Date getUsedTime()
    {
        return usedTime;
    }

    public void setUsedTime(Date usedTime)
    {
        this.usedTime = usedTime;
    }

    public String getUsedIp()
    {
        return usedIp;
    }

    public void setUsedIp(String usedIp)
    {
        this.usedIp = usedIp;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
