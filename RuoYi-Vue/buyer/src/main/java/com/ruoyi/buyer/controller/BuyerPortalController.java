package com.ruoyi.buyer.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.sql.SqlUtil;
import com.ruoyi.system.domain.PortalAccountProfile;
import com.ruoyi.system.domain.PortalAccountRoleAssign;
import com.ruoyi.system.domain.PortalDept;
import com.ruoyi.system.domain.PortalDeptProfile;
import com.ruoyi.system.domain.PortalLoginLog;
import com.ruoyi.system.domain.PortalLoginSession;
import com.ruoyi.system.domain.PortalMenu;
import com.ruoyi.system.domain.PortalOperLog;
import com.ruoyi.system.domain.PortalPasswordChangeRequest;
import com.ruoyi.system.domain.PortalRole;
import com.ruoyi.system.domain.PortalRoleProfile;
import com.ruoyi.system.domain.PortalSubjectProfile;
import com.ruoyi.system.service.support.PartnerSupport;
import com.ruoyi.system.service.support.PortalPermissionSupport;
import com.ruoyi.system.service.support.PortalSessionContext;

/**
 * Buyer terminal session endpoints.
 */
@RestController
@RequestMapping("/buyer")
public class BuyerPortalController extends BaseController
{
    private static final int PORTAL_LIST_MAX_PAGE_SIZE = 100;

    private static final Set<String> PORTAL_SELF_MANAGEMENT_PERMS = new HashSet<>(Arrays.asList(
            "buyer:portal:home",
            "buyer:account:list",
            "buyer:account:add",
            "buyer:account:edit",
            "buyer:account:role:query",
            "buyer:account:role:edit",
            "buyer:account:loginLog:list",
            "buyer:account:operLog:list",
            "buyer:account:session:list",
            "buyer:dept:list",
            "buyer:dept:query",
            "buyer:dept:add",
            "buyer:dept:edit",
            "buyer:dept:remove",
            "buyer:role:list",
            "buyer:role:query",
            "buyer:role:add",
            "buyer:role:edit",
            "buyer:role:remove"
    ));

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
        return success(PortalPermissionSupport.buildRouters(permissionService.selectPortalMenuTree(session)));
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

    @PostMapping("/accounts")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:add")
    @PortalLog(terminal = "buyer", title = "买家端账号新增", businessType = BusinessType.INSERT,
            excludeParamNames = { "password" })
    public AjaxResult addAccount(@RequestBody BuyerAccount account)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        normalizeNewSubAccount(account);
        return toAjax(buyerService.insertBuyerAccount(session.getSubjectId(), account));
    }

    @PutMapping("/accounts/{targetAccountId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:edit")
    @PortalLog(terminal = "buyer", title = "买家端账号修改", businessType = BusinessType.UPDATE,
            excludeParamNames = { "password" })
    public AjaxResult editAccount(@PathVariable("targetAccountId") Long targetAccountId, @RequestBody BuyerAccount account)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        if (account == null)
        {
            throw new ServiceException("买家端账号不能为空");
        }
        account.setBuyerAccountId(targetAccountId);
        account.setAccountId(targetAccountId);
        return toAjax(buyerService.updateBuyerAccount(session.getSubjectId(), account));
    }

    @GetMapping("/accounts/{targetAccountId}/roles")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:role:query")
    @PortalLog(terminal = "buyer", title = "买家端账号角色", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult accountRoles(@PathVariable("targetAccountId") Long targetAccountId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        AjaxResult ajax = AjaxResult.success();
        ajax.put("roles", permissionService.selectRoleAll(session.getSubjectId()));
        ajax.put("checkedKeys", permissionService.selectAccountRoleIds(session.getSubjectId(), targetAccountId));
        return ajax;
    }

    @PutMapping("/accounts/{targetAccountId}/roles")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:role:edit")
    @PortalLog(terminal = "buyer", title = "买家端账号角色分配", businessType = BusinessType.GRANT)
    public AjaxResult assignAccountRoles(@PathVariable("targetAccountId") Long targetAccountId,
            @RequestBody PortalAccountRoleAssign assign)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return toAjax(permissionService.assignAccountRoles(session.getSubjectId(), targetAccountId,
                assign == null ? null : assign.getRoleIds()));
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

    @GetMapping("/depts/{deptId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:dept:query")
    @PortalLog(terminal = "buyer", title = "买家端部门详情", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult dept(@PathVariable("deptId") Long deptId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(deptService.selectDeptById(session.getSubjectId(), deptId));
    }

    @GetMapping("/depts/treeselect")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:dept:query")
    @PortalLog(terminal = "buyer", title = "买家端部门树", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult deptTree(PortalDept dept)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(deptService.buildDeptTreeSelect(session.getSubjectId(), dept));
    }

    @PostMapping("/depts")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:dept:add")
    @PortalLog(terminal = "buyer", title = "买家端部门新增", businessType = BusinessType.INSERT)
    public AjaxResult addDept(@Validated @RequestBody PortalDept dept)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return toAjax(deptService.insertDept(session.getSubjectId(), dept));
    }

    @PutMapping("/depts/{deptId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:dept:edit")
    @PortalLog(terminal = "buyer", title = "买家端部门修改", businessType = BusinessType.UPDATE)
    public AjaxResult editDept(@PathVariable("deptId") Long deptId, @Validated @RequestBody PortalDept dept)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        dept.setDeptId(deptId);
        return toAjax(deptService.updateDept(session.getSubjectId(), dept));
    }

    @DeleteMapping("/depts/{deptId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:dept:remove")
    @PortalLog(terminal = "buyer", title = "买家端部门删除", businessType = BusinessType.DELETE)
    public AjaxResult removeDept(@PathVariable("deptId") Long deptId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return toAjax(deptService.deleteDeptById(session.getSubjectId(), deptId));
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

    @GetMapping("/roles/{roleId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:role:query")
    @PortalLog(terminal = "buyer", title = "买家端角色详情", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult role(@PathVariable("roleId") Long roleId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return success(permissionService.selectRoleById(session.getSubjectId(), roleId));
    }

    @GetMapping("/roles/menus")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:role:query")
    @PortalLog(terminal = "buyer", title = "买家端角色菜单模板", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult roleMenus()
    {
        PortalSessionContext.requireSession("buyer");
        AjaxResult ajax = AjaxResult.success();
        ajax.put("menus", PortalPermissionSupport.buildMenuTreeSelect(selectPortalSelfManagementMenus()));
        ajax.put("checkedKeys", new Long[0]);
        return ajax;
    }

    @GetMapping("/roles/{roleId}/menus")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:role:query")
    @PortalLog(terminal = "buyer", title = "买家端角色菜单", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult roleMenus(@PathVariable("roleId") Long roleId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        List<PortalMenu> selfManagementMenus = selectPortalSelfManagementMenus();
        AjaxResult ajax = AjaxResult.success();
        ajax.put("menus", PortalPermissionSupport.buildMenuTreeSelect(selfManagementMenus));
        ajax.put("checkedKeys", selectPortalSelfManagementMenuIds(session.getSubjectId(), roleId, selfManagementMenus));
        return ajax;
    }

    @PostMapping("/roles")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:role:add")
    @PortalLog(terminal = "buyer", title = "买家端角色新增", businessType = BusinessType.INSERT)
    public AjaxResult addRole(@Validated @RequestBody PortalRole role)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        assertPortalAssignableRoleMenus(role);
        return toAjax(permissionService.insertRole(session.getSubjectId(), role));
    }

    @PutMapping("/roles/{roleId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:role:edit")
    @PortalLog(terminal = "buyer", title = "买家端角色修改", businessType = BusinessType.UPDATE)
    public AjaxResult editRole(@PathVariable("roleId") Long roleId, @Validated @RequestBody PortalRole role)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        role.setRoleId(roleId);
        assertPortalAssignableRoleMenus(role);
        return toAjax(permissionService.updateRole(session.getSubjectId(), role));
    }

    @DeleteMapping("/roles/{roleIds}")
    @Anonymous
    @PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:role:remove")
    @PortalLog(terminal = "buyer", title = "买家端角色删除", businessType = BusinessType.DELETE)
    public AjaxResult removeRoles(@PathVariable Long[] roleIds)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("buyer");
        return toAjax(permissionService.deleteRoleByIds(session.getSubjectId(), roleIds));
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

    private void normalizeNewSubAccount(BuyerAccount account)
    {
        if (account == null)
        {
            throw new ServiceException("买家端账号不能为空");
        }
        String accountRole = PartnerSupport.normalizeAccountRole(account.getAccountRole());
        if (PartnerSupport.ACCOUNT_ROLE_OWNER.equals(accountRole))
        {
            throw new ServiceException("买家端自助入口不能创建 OWNER 主账号");
        }
        account.setAccountRole(accountRole);
        account.setBuyerAccountId(null);
        account.setAccountId(null);
        account.setBuyerId(null);
    }

    private void assertPortalAssignableRoleMenus(PortalRole role)
    {
        if (role == null || role.getMenuIds() == null)
        {
            return;
        }
        Set<Long> seenMenuIds = new HashSet<>();
        for (Long menuId : role.getMenuIds())
        {
            if (menuId == null || menuId <= 0)
            {
                throw new ServiceException("买家端自助角色菜单必须使用有效菜单ID");
            }
            if (!seenMenuIds.add(menuId))
            {
                throw new ServiceException("买家端自助角色菜单不能重复");
            }
            PortalMenu menu = permissionService.selectMenuById(menuId);
            if (menu == null || !PORTAL_SELF_MANAGEMENT_PERMS.contains(menu.getPerms()))
            {
                throw new ServiceException("买家端自助角色只能分配本轮最小权限模板");
            }
        }
    }

    private List<Long> selectPortalSelfManagementMenuIds(Long buyerId, Long roleId, List<PortalMenu> selfManagementMenus)
    {
        Set<Long> allowedMenuIds = new HashSet<>();
        for (PortalMenu menu : selfManagementMenus)
        {
            if (menu.getMenuId() != null)
            {
                allowedMenuIds.add(menu.getMenuId());
            }
        }

        List<Long> checkedMenuIds = new ArrayList<>();
        for (Long menuId : permissionService.selectMenuIdsByRoleId(buyerId, roleId))
        {
            if (allowedMenuIds.contains(menuId))
            {
                checkedMenuIds.add(menuId);
            }
        }
        return checkedMenuIds;
    }

    private List<PortalMenu> selectPortalSelfManagementMenus()
    {
        List<PortalMenu> menus = new ArrayList<>();
        for (PortalMenu menu : permissionService.selectMenuList(new PortalMenu()))
        {
            if (menu != null && PORTAL_SELF_MANAGEMENT_PERMS.contains(menu.getPerms()))
            {
                menus.add(menu);
            }
        }
        return menus;
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
