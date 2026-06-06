package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Seller/buyer portal session payload.
 */
public class PortalLoginSession implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String tokenId;

    private String terminal;

    private Long subjectId;

    private String subjectNo;

    private Long accountId;

    private String userName;

    private String nickName;

    private String loginIp;

    private String loginLocation;

    private String browser;

    private String os;

    private Date loginTime;

    private Date expireTime;

    private String status;

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

    public String getSubjectNo()
    {
        return subjectNo;
    }

    public void setSubjectNo(String subjectNo)
    {
        this.subjectNo = subjectNo;
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

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getLoginIp()
    {
        return loginIp;
    }

    public void setLoginIp(String loginIp)
    {
        this.loginIp = loginIp;
    }

    public String getLoginLocation()
    {
        return loginLocation;
    }

    public void setLoginLocation(String loginLocation)
    {
        this.loginLocation = loginLocation;
    }

    public String getBrowser()
    {
        return browser;
    }

    public void setBrowser(String browser)
    {
        this.browser = browser;
    }

    public String getOs()
    {
        return os;
    }

    public void setOs(String os)
    {
        this.os = os;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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
