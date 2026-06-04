package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Set;

/**
 * Seller/buyer terminal permission snapshot for the current account.
 */
public class PortalPermissionInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String terminal;

    private Long subjectId;

    private String subjectNo;

    private Long accountId;

    private String userName;

    private String nickName;

    private Set<String> roles;

    private Set<String> permissions;

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

    public Set<String> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<String> roles)
    {
        this.roles = roles;
    }

    public Set<String> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(Set<String> permissions)
    {
        this.permissions = permissions;
    }
}
