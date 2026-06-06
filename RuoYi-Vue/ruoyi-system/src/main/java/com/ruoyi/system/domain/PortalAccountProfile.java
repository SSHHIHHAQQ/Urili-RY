package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Seller/buyer terminal visible current account profile.
 */
public class PortalAccountProfile implements Serializable
{
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private String terminal;

    @JsonIgnore
    private Long subjectId;

    @JsonIgnore
    private Long accountId;

    private Long deptId;

    private String deptName;

    private String accountRole;

    private String status;

    private String userName;

    private String nickName;

    private String email;

    private String phonenumber;

    private Date lastLoginTime;

    private Date pwdUpdateTime;

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

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getAccountRole()
    {
        return accountRole;
    }

    public void setAccountRole(String accountRole)
    {
        this.accountRole = accountRole;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public Date getPwdUpdateTime()
    {
        return pwdUpdateTime;
    }

    public void setPwdUpdateTime(Date pwdUpdateTime)
    {
        this.pwdUpdateTime = pwdUpdateTime;
    }
}
