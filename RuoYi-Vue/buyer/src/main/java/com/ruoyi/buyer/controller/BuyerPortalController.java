package com.ruoyi.buyer.controller;

import java.util.ArrayList;
import java.util.List;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.service.IBuyerPortalDeptService;
import com.ruoyi.buyer.service.IBuyerPortalPermissionService;
import com.ruoyi.buyer.service.IBuyerService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.PortalLog;
import com.ruoyi.common.annotation.PortalPreAuthorize;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.sql.SqlUtil;
import com.ruoyi.system.domain.PortalAccountProfile;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalDeptProfile;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalRoleProfile;
import com.ruoyi.system.domain.PortalSubjectProfile;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Buyer terminal session endpoints.
 */
@RestController
@RequestMapping("/buyer")
public class BuyerPortalController extends BaseController
{
    private static final int PORTAL_LIST_MAX_PAGE_SIZE = 100;

    @Autowired
    private IBuyerPortalPermissionService permissionService;

    @Autowired
    private IBuyerService buyerService;

    @Autowired
    private IBuyerPortalDeptService deptService;

    @GetMapping("/getInfo")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端用户信息", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getInfo()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(permissionService.selectPortalPermissionInfo(session));
    }

    @GetMapping("/getRouters")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端菜单", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getRouters()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(permissionService.selectPortalMenuTree(session));
    }

    @PostMapping("/logout")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端退出登录", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult logout()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(buyerService.logoutBuyer(session));
    }

    @GetMapping("/profile")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端主体资料", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult profile()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(buildProfile(buyerService.selectBuyerById(session.getSubjectId())));
    }

    @GetMapping("/account/profile")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端账号资料", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult accountProfile()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(buildAccountProfile(buyerService.selectBuyerAccountById(session.getSubjectId(), session.getAccountId())));
    }

    @PutMapping("/account/password")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer")
    @PortalLog(terminal = "buyer", title = "买家端修改密码", businessType = BusinessType.UPDATE, isSaveResponseData = false)
    public AjaxResult updatePassword(@RequestBody PortalPasswordChangeRequest request)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return toAjax(buyerService.updateBuyerOwnPassword(session, request));
    }

    @GetMapping("/accounts")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:list")
    @PortalLog(terminal = "buyer", title = "买家端账号列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult accounts()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        List<PortalAccountProfile> profiles = new ArrayList<>();
        for (BuyerAccount account : buyerService.selectBuyerAccountList(session.getSubjectId()))
        {
            profiles.add(buildAccountProfile(account));
        }
        return success(profiles);
    }

    @GetMapping("/depts")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:dept:list")
    @PortalLog(terminal = "buyer", title = "买家端部门列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult depts()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        List<PortalDeptProfile> profiles = new ArrayList<>();
        for (PortalDept dept : deptService.selectDeptList(session.getSubjectId(), new PortalDept()))
        {
            profiles.add(buildDeptProfile(dept));
        }
        return success(profiles);
    }

    @GetMapping("/roles")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:role:list")
    @PortalLog(terminal = "buyer", title = "买家端角色列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult roles()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        List<PortalRoleProfile> profiles = new ArrayList<>();
        for (PortalRole role : permissionService.selectRoleList(session.getSubjectId(), new PortalRole()))
        {
            profiles.add(buildRoleProfile(role));
        }
        return success(profiles);
    }

    @GetMapping("/account/login-logs")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:loginLog:list")
    @PortalLog(terminal = "buyer", title = "买家端登录日志", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public TableDataInfo accountLoginLogs(PortalLoginLog log)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        startPortalListPage();
        return getDataTable(buyerService.selectBuyerOwnLoginLogList(session, log));
    }

    @GetMapping("/account/oper-logs")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:operLog:list")
    @PortalLog(terminal = "buyer", title = "买家端操作日志", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public TableDataInfo accountOperLogs(PortalOperLog log)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        startPortalListPage();
        return getDataTable(buyerService.selectBuyerOwnOperLogList(session, log));
    }

    @GetMapping("/account/sessions")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:session:list")
    @PortalLog(terminal = "buyer", title = "买家端会话列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public TableDataInfo accountSessions()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        startPortalListPage();
        return getDataTable(buyerService.selectBuyerOwnSessionList(session));
    }

    private void startPortalListPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        if (pageNum == null || pageNum < 1)
        {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1)
        {
            pageSize = 10;
        }
        if (pageSize > PORTAL_LIST_MAX_PAGE_SIZE)
        {
            pageSize = PORTAL_LIST_MAX_PAGE_SIZE;
        }
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(pageDomain.getReasonable());
    }

    private PortalSubjectProfile buildProfile(Buyer buyer)
    {
        PortalSubjectProfile profile = new PortalSubjectProfile();
        profile.setTerminal("buyer");
        profile.setSubjectId(buyer.getBuyerId());
        profile.setSubjectNo(buyer.getBuyerNo());
        profile.setSubjectCode(buyer.getBuyerCode());
        profile.setSubjectName(buyer.getBuyerName());
        profile.setSubjectShortName(buyer.getBuyerShortName());
        profile.setSubjectType(buyer.getBuyerType());
        profile.setSubjectLevel(buyer.getBuyerLevel());
        profile.setStatus(buyer.getStatus());
        profile.setCountryCode(buyer.getCountryCode());
        profile.setStateProvince(buyer.getStateProvince());
        profile.setCity(buyer.getCity());
        profile.setPostalCode(buyer.getPostalCode());
        profile.setAddressLine1(buyer.getAddressLine1());
        profile.setAddressLine2(buyer.getAddressLine2());
        profile.setContactName(buyer.getContactName());
        profile.setContactPhone(buyer.getContactPhone());
        profile.setContactEmail(buyer.getContactEmail());
        profile.setAttachment(buyer.getAttachment());
        return profile;
    }

    private PortalAccountProfile buildAccountProfile(BuyerAccount account)
    {
        PortalAccountProfile profile = new PortalAccountProfile();
        profile.setTerminal("buyer");
        profile.setSubjectId(account.getBuyerId());
        profile.setAccountId(account.getBuyerAccountId());
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
        profile.setTerminal("buyer");
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
        profile.setTerminal("buyer");
        profile.setSubjectId(role.getSubjectId());
        profile.setRoleId(role.getRoleId());
        profile.setRoleName(role.getRoleName());
        profile.setRoleKey(role.getRoleKey());
        profile.setRoleSort(role.getRoleSort());
        profile.setStatus(role.getStatus());
        return profile;
    }

}
