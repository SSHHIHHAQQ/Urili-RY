package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * Admin request to create an auditable seller/buyer direct-login ticket.
 */
public class PortalDirectLoginRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String reason;

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }
}
