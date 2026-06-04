package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Seller/buyer portal login result.
 */
public class PortalLoginResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String token;

    private String terminal;

    private Long subjectId;

    private String subjectNo;

    private Long accountId;

    private String username;

    private String nickName;

    private Integer expireMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
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

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public Integer getExpireMinutes()
    {
        return expireMinutes;
    }

    public void setExpireMinutes(Integer expireMinutes)
    {
        this.expireMinutes = expireMinutes;
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
