package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * Seller/buyer portal request to change the current account password.
 */
public class PortalPasswordChangeRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String oldPassword;

    private String newPassword;

    private String confirmPassword;

    public String getOldPassword()
    {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword)
    {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword(String newPassword)
    {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
    }
}
