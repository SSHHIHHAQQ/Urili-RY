package com.ruoyi.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Shared portal account binding fields.
 */
public abstract class PortalAccount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long accountId;

    private Long deptId;

    private String deptName;

    private String accountRole;

    private String status;

    private String userName;

    private String nickName;

    private String password;

    private String email;

    private String phonenumber;

    private String userStatus;

    private String lastLoginIp;

    private java.util.Date lastLoginTime;

    private java.util.Date pwdUpdateTime;

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

    @NotBlank(message = "账号角色不能为空")
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

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 0, max = 30, message = "登录账号长度不能超过30个字符")
    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getUserStatus()
    {
        return userStatus;
    }

    public void setUserStatus(String userStatus)
    {
        this.userStatus = userStatus;
    }

    public String getLastLoginIp()
    {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp)
    {
        this.lastLoginIp = lastLoginIp;
    }

    public java.util.Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(java.util.Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public java.util.Date getPwdUpdateTime()
    {
        return pwdUpdateTime;
    }

    public void setPwdUpdateTime(java.util.Date pwdUpdateTime)
    {
        this.pwdUpdateTime = pwdUpdateTime;
    }
}
