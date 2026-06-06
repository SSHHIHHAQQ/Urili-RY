package com.ruoyi.system.domain;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Seller/buyer terminal visible role profile.
 */
public class PortalRoleProfile implements Serializable
{
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private String terminal;

    @JsonIgnore
    private Long subjectId;

    private Long roleId;

    private String roleName;

    private String roleKey;

    private Integer roleSort;

    private String status;

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

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }

    public String getRoleKey()
    {
        return roleKey;
    }

    public void setRoleKey(String roleKey)
    {
        this.roleKey = roleKey;
    }

    public Integer getRoleSort()
    {
        return roleSort;
    }

    public void setRoleSort(Integer roleSort)
    {
        this.roleSort = roleSort;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
