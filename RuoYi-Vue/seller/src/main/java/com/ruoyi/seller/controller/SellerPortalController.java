package com.ruoyi.seller.controller;

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
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.sql.SqlUtil;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.service.ISellerPortalDeptService;
import com.ruoyi.seller.service.ISellerService;
import com.ruoyi.seller.service.ISellerPortalPermissionService;
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
 * Seller terminal session endpoints.
 */
@RestController
@RequestMapping("/seller")
public class SellerPortalController extends BaseController
{
    private static final int PORTAL_LIST_MAX_PAGE_SIZE = 100;

    private static final Set<String> PORTAL_SELF_MANAGEMENT_PERMS = new HashSet<>(Arrays.asList(
            "seller:portal:home",
            "seller:account:list",
            "seller:account:add",
            "seller:account:edit",
            "seller:account:role:query",
            "seller:account:role:edit",
            "seller:account:loginLog:list",
            "seller:account:operLog:list",
            "seller:account:session:list",
            "seller:dept:list",
            "seller:dept:query",
            "seller:dept:add",
            "seller:dept:edit",
            "seller:dept:remove",
            "seller:role:list",
            "seller:role:query",
            "seller:role:add",
            "seller:role:edit",
            "seller:role:remove"
    ));

    @Autowired
    private ISellerPortalPermissionService permissionService;

    @Autowired
    private ISellerService sellerService;

    @Autowired
    private ISellerPortalDeptService deptService;

    @GetMapping("/getInfo")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端用户信息", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getInfo()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(permissionService.selectPortalPermissionInfo(session));
    }

    @GetMapping("/getRouters")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端菜单", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult getRouters()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(PortalPermissionSupport.buildRouters(permissionService.selectPortalMenuTree(session)));
    }

    @PostMapping("/logout")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端退出登录", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult logout()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(sellerService.logoutSeller(session));
    }

    @GetMapping("/profile")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端主体资料", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult profile()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(buildProfile(sellerService.selectSellerById(session.getSubjectId())));
    }

    @GetMapping("/account/profile")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端账号资料", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public AjaxResult accountProfile()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(buildAccountProfile(sellerService.selectSellerAccountById(session.getSubjectId(), session.getAccountId())));
    }

    @PutMapping("/account/password")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller")
    @PortalLog(terminal = "seller", title = "卖家端修改密码", businessType = BusinessType.UPDATE, isSaveResponseData = false)
    public AjaxResult updatePassword(@RequestBody PortalPasswordChangeRequest request)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return toAjax(sellerService.updateSellerOwnPassword(session, request));
    }

    @GetMapping("/accounts")
    @Anonymous
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

    @PostMapping("/accounts")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:add")
    @PortalLog(terminal = "seller", title = "卖家端账号新增", businessType = BusinessType.INSERT,
            excludeParamNames = { "password" })
    public AjaxResult addAccount(@RequestBody SellerAccount account)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        normalizeNewSubAccount(account);
        return toAjax(sellerService.insertSellerAccount(session.getSubjectId(), account));
    }

    @PutMapping("/accounts/{targetAccountId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:edit")
    @PortalLog(terminal = "seller", title = "卖家端账号修改", businessType = BusinessType.UPDATE,
            excludeParamNames = { "password" })
    public AjaxResult editAccount(@PathVariable("targetAccountId") Long targetAccountId, @RequestBody SellerAccount account)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        if (account == null)
        {
            throw new ServiceException("卖家端账号不能为空");
        }
        account.setSellerAccountId(targetAccountId);
        account.setAccountId(targetAccountId);
        account.setSellerId(null);
        return toAjax(sellerService.updateSellerAccount(session.getSubjectId(), account));
    }

    @GetMapping("/accounts/{targetAccountId}/roles")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = {
            "seller:account:role:query", "seller:role:list" })
    @PortalLog(terminal = "seller", title = "卖家端账号角色", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult accountRoles(@PathVariable("targetAccountId") Long targetAccountId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        AjaxResult ajax = AjaxResult.success();
        ajax.put("roles", permissionService.selectRoleAll(session.getSubjectId()));
        ajax.put("checkedKeys", permissionService.selectAccountRoleIds(session.getSubjectId(), targetAccountId));
        return ajax;
    }

    @PutMapping("/accounts/{targetAccountId}/roles")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = {
            "seller:account:role:edit", "seller:account:role:query", "seller:role:list" })
    @PortalLog(terminal = "seller", title = "卖家端账号角色分配", businessType = BusinessType.GRANT)
    public AjaxResult assignAccountRoles(@PathVariable("targetAccountId") Long targetAccountId,
            @RequestBody PortalAccountRoleAssign assign)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return toAjax(permissionService.assignAccountRoles(session.getSubjectId(), targetAccountId,
                assign == null ? null : assign.getRoleIds()));
    }

    @GetMapping("/depts")
    @Anonymous
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

    @GetMapping("/depts/{deptId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:dept:query")
    @PortalLog(terminal = "seller", title = "卖家端部门详情", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult dept(@PathVariable("deptId") Long deptId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(deptService.selectDeptById(session.getSubjectId(), deptId));
    }

    @GetMapping("/depts/treeselect")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:dept:query")
    @PortalLog(terminal = "seller", title = "卖家端部门树", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult deptTree(PortalDept dept)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(deptService.buildDeptTreeSelect(session.getSubjectId(), dept));
    }

    @PostMapping("/depts")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:dept:add")
    @PortalLog(terminal = "seller", title = "卖家端部门新增", businessType = BusinessType.INSERT)
    public AjaxResult addDept(@Validated @RequestBody PortalDept dept)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return toAjax(deptService.insertDept(session.getSubjectId(), dept));
    }

    @PutMapping("/depts/{deptId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:dept:edit")
    @PortalLog(terminal = "seller", title = "卖家端部门修改", businessType = BusinessType.UPDATE)
    public AjaxResult editDept(@PathVariable("deptId") Long deptId, @Validated @RequestBody PortalDept dept)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        dept.setDeptId(deptId);
        return toAjax(deptService.updateDept(session.getSubjectId(), dept));
    }

    @DeleteMapping("/depts/{deptId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:dept:remove")
    @PortalLog(terminal = "seller", title = "卖家端部门删除", businessType = BusinessType.DELETE)
    public AjaxResult removeDept(@PathVariable("deptId") Long deptId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return toAjax(deptService.deleteDeptById(session.getSubjectId(), deptId));
    }

    @GetMapping("/roles")
    @Anonymous
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

    @GetMapping("/roles/{roleId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:role:query")
    @PortalLog(terminal = "seller", title = "卖家端角色详情", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult role(@PathVariable("roleId") Long roleId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return success(permissionService.selectRoleById(session.getSubjectId(), roleId));
    }

    @GetMapping("/roles/menus")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:role:query")
    @PortalLog(terminal = "seller", title = "卖家端角色菜单模板", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult roleMenus()
    {
        PortalSessionContext.requireSession("seller");
        AjaxResult ajax = AjaxResult.success();
        ajax.put("menus", PortalPermissionSupport.buildMenuTreeSelect(selectPortalSelfManagementMenus()));
        ajax.put("checkedKeys", new Long[0]);
        return ajax;
    }

    @GetMapping("/roles/{roleId}/menus")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:role:query")
    @PortalLog(terminal = "seller", title = "卖家端角色菜单", businessType = BusinessType.OTHER,
            isSaveResponseData = false)
    public AjaxResult roleMenus(@PathVariable("roleId") Long roleId)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        List<PortalMenu> selfManagementMenus = selectPortalSelfManagementMenus();
        AjaxResult ajax = AjaxResult.success();
        ajax.put("menus", PortalPermissionSupport.buildMenuTreeSelect(selfManagementMenus));
        ajax.put("checkedKeys", selectPortalSelfManagementMenuIds(session.getSubjectId(), roleId, selfManagementMenus));
        return ajax;
    }

    @PostMapping("/roles")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:role:add")
    @PortalLog(terminal = "seller", title = "卖家端角色新增", businessType = BusinessType.INSERT)
    public AjaxResult addRole(@Validated @RequestBody PortalRole role)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        assertPortalAssignableRoleMenus(role);
        return toAjax(permissionService.insertRole(session.getSubjectId(), role));
    }

    @PutMapping("/roles/{roleId}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:role:edit")
    @PortalLog(terminal = "seller", title = "卖家端角色修改", businessType = BusinessType.UPDATE)
    public AjaxResult editRole(@PathVariable("roleId") Long roleId, @Validated @RequestBody PortalRole role)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        role.setRoleId(roleId);
        assertPortalAssignableRoleMenus(role);
        return toAjax(permissionService.updateRole(session.getSubjectId(), role));
    }

    @DeleteMapping("/roles/{roleIds}")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:role:remove")
    @PortalLog(terminal = "seller", title = "卖家端角色删除", businessType = BusinessType.DELETE)
    public AjaxResult removeRoles(@PathVariable Long[] roleIds)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        return toAjax(permissionService.deleteRoleByIds(session.getSubjectId(), roleIds));
    }

    @GetMapping("/account/login-logs")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:loginLog:list")
    @PortalLog(terminal = "seller", title = "卖家端登录日志", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public TableDataInfo accountLoginLogs(PortalLoginLog log)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        startPortalListPage();
        return getDataTable(sellerService.selectSellerOwnLoginLogList(session, log));
    }

    @GetMapping("/account/oper-logs")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:operLog:list")
    @PortalLog(terminal = "seller", title = "卖家端操作日志", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public TableDataInfo accountOperLogs(PortalOperLog log)
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        startPortalListPage();
        return getDataTable(sellerService.selectSellerOwnOperLogList(session, log));
    }

    @GetMapping("/account/sessions")
    @Anonymous
    @PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:session:list")
    @PortalLog(terminal = "seller", title = "卖家端会话列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public TableDataInfo accountSessions()
    {
        PortalLoginSession session = PortalSessionContext.requireSession("seller");
        startPortalListPage();
        return getDataTable(sellerService.selectSellerOwnSessionList(session));
    }

    private void normalizeNewSubAccount(SellerAccount account)
    {
        if (account == null)
        {
            throw new ServiceException("卖家端账号不能为空");
        }
        String accountRole = PartnerSupport.normalizeAccountRole(account.getAccountRole());
        if (PartnerSupport.ACCOUNT_ROLE_OWNER.equals(accountRole))
        {
            throw new ServiceException("卖家端自助入口不能创建 OWNER 主账号");
        }
        account.setAccountRole(accountRole);
        account.setSellerAccountId(null);
        account.setAccountId(null);
        account.setSellerId(null);
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
                throw new ServiceException("卖家端自助角色菜单必须使用有效菜单ID");
            }
            if (!seenMenuIds.add(menuId))
            {
                throw new ServiceException("卖家端自助角色菜单不能重复");
            }
            PortalMenu menu = permissionService.selectMenuById(menuId);
            PortalPermissionSupport.assertReadableTerminalMenu(menu, "seller");
            if (!PORTAL_SELF_MANAGEMENT_PERMS.contains(StringUtils.trimToEmpty(menu.getPerms())))
            {
                throw new ServiceException("卖家端自助角色只能分配本轮最小权限模板");
            }
        }
    }

    private List<Long> selectPortalSelfManagementMenuIds(Long sellerId, Long roleId, List<PortalMenu> selfManagementMenus)
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
        for (Long menuId : permissionService.selectMenuIdsByRoleId(sellerId, roleId))
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
            String perms = menu == null ? "" : StringUtils.trimToEmpty(menu.getPerms());
            if (PORTAL_SELF_MANAGEMENT_PERMS.contains(perms))
            {
                PortalPermissionSupport.assertReadableTerminalMenu(menu, "seller");
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
