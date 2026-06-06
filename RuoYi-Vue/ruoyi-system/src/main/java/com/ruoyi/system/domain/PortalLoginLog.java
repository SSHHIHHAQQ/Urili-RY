package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Seller/buyer portal login log fields.
 */
public class PortalLoginLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long infoId;

    private Long subjectId;

    private Long accountId;

    private String userName;

    private String ipaddr;

    private String loginLocation;

    private String browser;

    private String os;

    private String status;

    private String msg;

    private Boolean directLogin;

    private Long directLoginTicketId;

    private Long actingAdminId;

    private String actingAdminName;

    private String directLoginReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;

    public Long getInfoId()
    {
        return infoId;
    }

    public void setInfoId(Long infoId)
    {
        this.infoId = infoId;
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

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
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

    public Date getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(Date loginTime)
    {
        this.loginTime = loginTime;
    }
}
