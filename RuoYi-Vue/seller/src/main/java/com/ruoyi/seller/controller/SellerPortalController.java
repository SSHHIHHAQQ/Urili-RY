package com.ruoyi.seller.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.service.ISellerPortalDeptService;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
import com.ruoyi.system.domain.PortalAccountProfile;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalDeptProfile;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalRoleProfile;
import com.ruoyi.system.domain.PortalSubjectProfile;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Seller terminal session endpoints.
 */
@Anonymous
@RestController
@RequestMapping("/seller")
public class SellerPortalController extends BaseController
{
    @Autowired
    private ISellerPortalPermissionService permissionService;

    @Autowired
    private ISellerService sellerService;

    @Autowired
    private ISellerPortalDeptService deptService;

    @GetMapping("/getInfo")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端用户信息", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getInfo()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(permissionService.selectPortalPermissionInfo(session));
    }

    @GetMapping("/getRouters")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端菜单", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getRouters()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(permissionService.selectPortalMenuTree(session));
    }

    @PostMapping("/logout")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端退出登录", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult logout()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(sellerService.logoutSeller(session));
    }

    @GetMapping("/profile")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端主体资料", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult profile()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(buildProfile(sellerService.selectSellerById(session.getSubjectId())));
    }

    @GetMapping("/account/profile")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端账号资料", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult accountProfile()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(buildAccountProfile(sellerService.selectSellerAccountById(session.getSubjectId(), session.getAccountId())));
    }

    @PutMapping("/account/password")
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端修改密码", businessType = BusinessType.UPDATE, isSaveResponseData = false)
    public AjaxResult updatePassword(@RequestBody PortalPasswordChangeRequest request)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return toAjax(sellerService.updateSellerOwnPassword(session, request));
    }

    @GetMapping("/accounts")
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:list")
    @PortalLog(terminal = "seller", title = "卖家端账号列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult accounts()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        List<PortalAccountProfile> profiles = new ArrayList<>();
        for (SellerAccount account : sellerService.selectSellerAccountList(session.getSubjectId()))
        {
            profiles.add(buildAccountProfile(account));
        }
        return success(profiles);
    }

    @GetMapping("/depts")
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:dept:list")
    @PortalLog(terminal = "seller", title = "卖家端部门列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult depts()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        List<PortalDeptProfile> profiles = new ArrayList<>();
        for (PortalDept dept : deptService.selectDeptList(session.getSubjectId(), new PortalDept()))
        {
            profiles.add(buildDeptProfile(dept));
        }
        return success(profiles);
    }

    @GetMapping("/roles")
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:role:list")
    @PortalLog(terminal = "seller", title = "卖家端角色列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult roles()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        List<PortalRoleProfile> profiles = new ArrayList<>();
        for (PortalRole role : permissionService.selectRoleList(session.getSubjectId(), new PortalRole()))
        {
            profiles.add(buildRoleProfile(role));
        }
        return success(profiles);
    }

    private PortalSubjectProfile buildProfile(Seller seller)
    {
        PortalSubjectProfile profile = new PortalSubjectProfile();
        profile.setTerminal("seller");
        profile.setSubjectId(seller.getSellerId());
        profile.setSubjectNo(seller.getSellerNo());
        profile.setSubjectCode(seller.getSellerCode());
        profile.setSubjectName(seller.getSellerName());
        profile.setSubjectShortName(seller.getSellerShortName());
        profile.setSubjectType(seller.getSellerType());
        profile.setSubjectLevel(seller.getSellerLevel());
        profile.setStatus(seller.getStatus());
        profile.setCountryCode(seller.getCountryCode());
        profile.setStateProvince(seller.getStateProvince());
        profile.setCity(seller.getCity());
        profile.setPostalCode(seller.getPostalCode());
        profile.setAddressLine1(seller.getAddressLine1());
        profile.setAddressLine2(seller.getAddressLine2());
        profile.setContactName(seller.getContactName());
        profile.setContactPhone(seller.getContactPhone());
        profile.setContactEmail(seller.getContactEmail());
        profile.setAttachment(seller.getAttachment());
        profile.setAccountBalance(seller.getAccountBalance());
        profile.setBalanceCurrency(seller.getBalanceCurrency());
        return profile;
    }

    private PortalAccountProfile buildAccountProfile(SellerAccount account)
    {
        PortalAccountProfile profile = new PortalAccountProfile();
        profile.setTerminal("seller");
        profile.setSubjectId(account.getSellerId());
        profile.setAccountId(account.getSellerAccountId());
        profile.setDeptId(account.getDeptId());
        profile.setDeptName(account.getDeptName());
        profile.setAccountRole(account.getAccountRole());
        profile.setStatus(account.getStatus());
        profile.setUserName(account.getUserName());
        profile.setNickName(account.getNickName());
        profile.setEmail(account.getEmail());
        profile.setPhonenumber(account.getPhonenumber());
        profile.setLastLoginTime(account.getLastLoginTime());
        profile.setPwdUpdateTime(account.getPwdUpdateTime());
        return profile;
    }

    private PortalDeptProfile buildDeptProfile(PortalDept dept)
    {
        PortalDeptProfile profile = new PortalDeptProfile();
        profile.setTerminal("seller");
        profile.setSubjectId(dept.getSubjectId());
        profile.setDeptId(dept.getDeptId());
        profile.setParentId(dept.getParentId());
        profile.setParentName(dept.getParentName());
        profile.setDeptName(dept.getDeptName());
        profile.setOrderNum(dept.getOrderNum());
        profile.setLeader(dept.getLeader());
        profile.setPhone(dept.getPhone());
        profile.setEmail(dept.getEmail());
        profile.setStatus(dept.getStatus());
        return profile;
    }

    private PortalRoleProfile buildRoleProfile(PortalRole role)
    {
        PortalRoleProfile profile = new PortalRoleProfile();
        profile.setTerminal("seller");
        profile.setSubjectId(role.getSubjectId());
        profile.setRoleId(role.getRoleId());
        profile.setRoleName(role.getRoleName());
        profile.setRoleKey(role.getRoleKey());
        profile.setRoleSort(role.getRoleSort());
        profile.setStatus(role.getStatus());
        return profile;
    }
}
