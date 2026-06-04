package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * Seller/buyer terminal account role assignment payload.
 */
public class PortalAccountRoleAssign implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long[] roleIds;

    public Long[] getRoleIds()
    {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds)
    {
        this.roleIds = roleIds;
    }
}
