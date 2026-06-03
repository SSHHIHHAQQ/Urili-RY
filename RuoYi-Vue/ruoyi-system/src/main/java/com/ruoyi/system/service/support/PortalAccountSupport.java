package com.ruoyi.system.service.support;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.PortalAccount;
import com.ruoyi.system.mapper.PortalAccountMapper;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

/**
 * Shared account creation and password operations for buyer/seller modules.
 */
@Component
public class PortalAccountSupport
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private PortalAccountMapper portalAccountMapper;

    public SysUser createPortalUser(PortalAccount account, String roleKey, String remark)
    {
        SysUser user = new SysUser();
        user.setUserName(PartnerSupport.trimRequired(account.getUserName(), "登录用户名不能为空"));
        user.setNickName(PartnerSupport.trimRequired(account.getNickName(), "姓名不能为空"));
        user.setEmail(StringUtils.trimToEmpty(account.getEmail()));
        user.setPhonenumber(StringUtils.trimToEmpty(account.getPhonenumber()));
        user.setStatus(StringUtils.defaultIfBlank(account.getStatus(), PartnerSupport.STATUS_NORMAL));
        user.setPassword(SecurityUtils.encryptPassword(account.getPassword()));
        user.setRoleIds(resolvePortalRoleIds(roleKey));
        user.setCreateBy(SecurityUtils.getUsername());
        user.setRemark(remark);

        if (!userService.checkUserNameUnique(user))
        {
            throw new ServiceException("登录账号已存在");
        }
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            throw new ServiceException("手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            throw new ServiceException("邮箱账号已存在");
        }

        userService.insertUser(user);
        return user;
    }

    public int resetPassword(Long userId, String password)
    {
        return userService.resetUserPwd(userId, SecurityUtils.encryptPassword(password));
    }

    public void syncUserProfile(Long userId, String nickName, String status, String remark)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setDeptId(0L);
        user.setNickName(PartnerSupport.limitLength(nickName, 30));
        user.setEmail("");
        user.setPhonenumber("");
        user.setStatus(status);
        user.setUpdateBy(SecurityUtils.getUsername());
        user.setRemark(remark);
        userService.updateUserProfile(user);
    }

    public void updateUserStatus(Long userId, String status)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        userService.updateUserStatus(user);
    }

    public void assertUserNotBoundToAnyPortal(Long userId)
    {
        int sellerAccountCount = portalAccountMapper.countSellerAccountByUserId(userId);
        int buyerAccountCount = portalAccountMapper.countBuyerAccountByUserId(userId);
        if (sellerAccountCount > 0 || buyerAccountCount > 0)
        {
            throw new ServiceException("该用户已绑定端账号");
        }
    }

    private Long[] resolvePortalRoleIds(String roleKey)
    {
        List<SysRole> roles = roleService.selectRoleAll();
        for (SysRole role : roles)
        {
            if (roleKey.equals(role.getRoleKey()))
            {
                return new Long[] { role.getRoleId() };
            }
        }
        throw new ServiceException("端账号角色未初始化：" + roleKey);
    }
}
