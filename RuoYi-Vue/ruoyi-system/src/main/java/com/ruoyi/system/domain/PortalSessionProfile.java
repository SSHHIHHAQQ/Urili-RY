package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Seller/buyer terminal visible session profile.
 */
public class PortalSessionProfile implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String tokenId;

    private String terminal;

    private Long subjectId;

    private Long accountId;

    private String userName;

    private String loginIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date logoutTime;

    private String status;

    private Boolean current;

    private Boolean directLogin;

    private Long directLoginTicketId;

    private Long actingAdminId;

    private String actingAdminName;

    private String directLoginReason;

    public String getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }

    public String getTerminal()
    {
        return terminal;
    }

    public void setTerminal(String terminal)
    {
        this.terminal = terminal;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public Long getAccountId()
    {
        return accountId;
    }

    public void setAccountId(Long accountId)
    {
        this.accountId = accountId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getLoginIp()
    {
        return loginIp;
    }

    public void setLoginIp(String loginIp)
    {
        this.loginIp = loginIp;
    }

    public Date getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(Date loginTime)
    {
        this.loginTime = loginTime;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Date getLogoutTime()
    {
        return logoutTime;
    }

    public void setLogoutTime(Date logoutTime)
    {
        this.logoutTime = logoutTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Boolean getCurrent()
    {
        return current;
    }

    public void setCurrent(Boolean current)
    {
        this.current = current;
    }

    public Boolean getDirectLogin()
    {
        return directLogin;
    }

    public void setDirectLogin(Boolean directLogin)
    {
        this.directLogin = directLogin;
    }

    public Long getDirectLoginTicketId()
    {
        return directLoginTicketId;
    }

    public void setDirectLoginTicketId(Long directLoginTicketId)
    {
        this.directLoginTicketId = directLoginTicketId;
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
}
